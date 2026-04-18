package br.com.nicolebertolo.application.port.inbound;


import br.com.nicolebertolo.domain.model.UploadStatus;
import br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.Inspection;
import br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.PropertyListing;

import java.util.List;
import java.util.Optional;

/**
 * Caso de uso para listar múltiplos anúncios de forma contínua (streaming).
 * Ideal para consultas com grande volume de dados.
 */
public interface ListingUseCase {

    Optional<PropertyListing> getListing(String id);

    List<PropertyListing> getAllListings(ListingFilter filter);

    UploadStatus uploadInspection(br.com.nicolebertolo.domain.model.Inspection inspection);

    UploadStatus liveInspection(Inspection inspection);

    /**
     * Record com critérios de filtro.
     */
    record ListingFilter(String city, List<String> tags) {
    }
}