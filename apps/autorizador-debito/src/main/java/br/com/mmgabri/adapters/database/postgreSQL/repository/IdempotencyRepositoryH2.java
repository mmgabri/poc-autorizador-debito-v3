package br.com.mmgabri.adapters.database.postgreSQL.repository;

import br.com.mmgabri.adapters.database.postgreSQL.entity.IdempotencyEntityH2;
import br.com.mmgabri.adapters.database.postgreSQL.entity.TransactionContextEntityH2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRepositoryH2 extends JpaRepository<IdempotencyEntityH2, String> {

}
