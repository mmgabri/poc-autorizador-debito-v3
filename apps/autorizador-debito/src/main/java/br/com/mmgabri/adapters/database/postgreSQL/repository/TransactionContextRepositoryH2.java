package br.com.mmgabri.adapters.database.postgreSQL.repository;

import br.com.mmgabri.adapters.database.postgreSQL.entity.TransactionContextEntityH2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionContextRepositoryH2 extends JpaRepository<TransactionContextEntityH2, String> {

}
