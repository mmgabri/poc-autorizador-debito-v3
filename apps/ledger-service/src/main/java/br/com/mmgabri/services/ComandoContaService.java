package br.com.mmgabri.services;

import br.com.mmgabri.adapters.dynamodb.entity.ComandoContaEntity;
import br.com.mmgabri.adapters.dynamodb.mapper.ComandoContaMapper;
import br.com.mmgabri.adapters.dynamodb.repository.ComandoContaRepository;
import br.com.mmgabri.adapters.redis.LedgerCompletionRedisPublisher;
import br.com.mmgabri.domain.ComandoContaRequest;
import br.com.mmgabri.domain.enuns.ComandoContaStatusEnum;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ComandoContaService {

    private static final Logger logger = LoggerFactory.getLogger(ComandoContaService.class);

    private final ComandoContaRepository comandoContaRepository;
    private final LedgerCompletionRedisPublisher completionPublisher;
    private final ComandoContaMapper comandoContaMapper;

    public void execute(ComandoContaRequest payload) {
        sleep(Duration.ofMillis(payload.sleepLedgerEfetivacao()));

        boolean approved = "000".equals(payload.customReturnLedger());
        String errorDescription = getMessage(payload.customReturnLedger());
        ComandoContaEntity fields = comandoContaMapper.toResultFields(payload, approved, errorDescription);

        // Caso normal: registro ainda PENDING -> COMPLETED, e sinaliza via Redis
        // (alguém pode estar esperando).
        if (comandoContaRepository.tryTransition(fields, ComandoContaStatusEnum.PENDING, ComandoContaStatusEnum.COMPLETED)) {
            completionPublisher.signal(payload.correlationId());
            logger.debug("Efetivação concluída via SQS. correlationId={} approved={}", payload.correlationId(), approved);
            return;
        }

        // Caso de corrida: o autorizador já desistiu (TIMEOUT) - ninguém está mais
        // ouvindo o Redis. Grava o resultado real mesmo assim; não sinaliza.
        if (comandoContaRepository.tryTransition(fields, ComandoContaStatusEnum.TIMEOUT, ComandoContaStatusEnum.COMPLETED_LATE)) {
            logger.warn("Efetivação concluída após o autorizador desistir por timeout. correlationId={} approved={}", payload.correlationId(), approved);
            return;
        }

        logger.warn("Nenhuma transição aplicada - comando já resolvido (reentrega?). correlationId={}", payload.correlationId());
    }

    // Simula processamento
    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String getMessage(String errorCode) {
        switch (errorCode) {
            case "SDO":
                return "Saldo Insuficiente";
            case "LIM":
                return "Limite Insuficiente";
            case "SEN":
                return "Senha invalida";
            case "CHP":
                return "Erro na autenticação do chip";
            case "CVV":
                return "CVV Invalido";
            case "CNE":
                return "Cartão invalido";
            case "IND":
                return "Sistema indisponivel";
            case "TIM":
                return "Timeout  na aplicação";
            case "EIN":
                return "Error de sistema";
            case "ERR":
                return "Erro de sistema";
            default:
                return "Processamento efetuado";
        }
    }
}
