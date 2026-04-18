package br.com.mmgabri.adapters.database.postgreSQL.repository;

import br.com.mmgabri.adapters.database.postgreSQL.entity.ServiceContextEntityH2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceContextRepositoryH2 extends JpaRepository<ServiceContextEntityH2, String> {

}
