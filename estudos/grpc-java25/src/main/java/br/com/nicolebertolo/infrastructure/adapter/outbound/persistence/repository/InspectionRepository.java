package br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.repository;

import br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, String> {

}
