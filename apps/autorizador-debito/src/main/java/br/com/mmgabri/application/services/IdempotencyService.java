package br.com.mmgabri.application.services;

import br.com.mmgabri.adapters.dynamodb.entity.IdempotencyEntity;
import br.com.mmgabri.adapters.dynamodb.mapper.DatabaseMapper;
import br.com.mmgabri.adapters.dynamodb.repository.IdempotencyRepository;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.TechnicalException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final IdempotencyRepository repo;
    private final DatabaseMapper map;

    public void execute(Payload payload) {
        Optional<IdempotencyEntity> idempotency = repo.getByCorrelationId(payload.getHeaderMessage().getCorrelationId());

        if (idempotency.isPresent()) {
            throw new TechnicalException("idempotency", "999", "Transação já processada");
        }

        repo.save(map.mapToIdempotency(payload));
    }
}

