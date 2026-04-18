package br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.repository;

import br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.PropertyListing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyListingRepository extends JpaRepository<PropertyListing, String> {
}
