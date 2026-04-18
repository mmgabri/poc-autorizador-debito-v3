package br.com.mmgabri.application.services;

import br.com.mmgabri.adapters.database.dynamodb.entity.IdempotencyEntity;
import br.com.mmgabri.adapters.database.dynamodb.mapper.DatabaseMapper;
import br.com.mmgabri.adapters.database.dynamodb.repository.IdempotencyRepository;
import br.com.mmgabri.adapters.database.postgreSQL.entity.IdempotencyEntityH2;
import br.com.mmgabri.adapters.database.postgreSQL.mapper.DatabaseH2Mapper;
import br.com.mmgabri.adapters.database.postgreSQL.repository.IdempotencyRepositoryH2;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.TechnicalException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    @Value("${custom.database-memo}")
    private boolean isDatabaseMemo;

    private final IdempotencyRepository repo;
    private final DatabaseMapper map;
    private final IdempotencyRepositoryH2 repoH2;
    private final DatabaseH2Mapper mapH2;

    public void execute(Payload payload) {
        if (isDatabaseMemo) {
            Optional<IdempotencyEntityH2> idempotency = repoH2.findById(payload.getHeaderMessage().getCorrelationId());
            if (idempotency.isPresent()) {
                throw new TechnicalException("idempotency", "999", "Transação já processada");
            }
            repoH2.save(mapH2.mapToIdempotency(payload));
            return;
        }

        Optional<IdempotencyEntity> idempotency = repo.getByCorrelationId(payload.getHeaderMessage().getCorrelationId());

        if (idempotency.isPresent()) {
            throw new TechnicalException("idempotency", "999", "Transação já processada");
        }

        repo.save(map.mapToIdempotency(payload));
    }
}

