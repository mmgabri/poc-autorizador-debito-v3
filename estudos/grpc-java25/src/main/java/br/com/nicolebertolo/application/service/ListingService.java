package br.com.nicolebertolo.application.service;


import br.com.nicolebertolo.application.port.inbound.ListingUseCase;
import br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.Inspection;
import br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.PropertyListing;
import br.com.nicolebertolo.domain.model.UploadStatus;
import br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.repository.InspectionRepository;
import br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.repository.PropertyListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static br.com.nicolebertolo.shared.RecordConverter.convertToEntity;

@Service
public class ListingService implements ListingUseCase {

    private final PropertyListingRepository propertyListingRepository;
    private final InspectionRepository inspectionRepository;

    public ListingService(PropertyListingRepository propertyListingRepository, InspectionRepository inspectionRepository) {
        this.propertyListingRepository = propertyListingRepository;
        this.inspectionRepository = inspectionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PropertyListing> getListing(String id) {
        var listing = propertyListingRepository.findById(id);
        listing.get().getFeatures().size();
        listing.get().getTags().size();
        listing.get().getImages().size();
        return listing;
    }

    @Override
    public List<PropertyListing> getAllListings(ListingFilter filter) {

        List<PropertyListing> result = propertyListingRepository.findAll();

        return result.stream().filter(listing -> {
            boolean matchesCity = filter.city() == null || listing.getCity().equalsIgnoreCase(filter.city());
            boolean matchesTags = filter.tags() == null || filter.tags().isEmpty() ||
                    listing.getTags().stream().anyMatch(tag -> filter.tags().contains(tag));
            return matchesCity && matchesTags;
        }).toList();
    }

    @Override
    public UploadStatus uploadInspection(br.com.nicolebertolo.domain.model.Inspection inspection) {
        var property = propertyListingRepository.findById(inspection.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + inspection.propertyId()));

        var entity = convertToEntity(inspection);
        entity.setPropertyId(property);
        Inspection savedInspection = inspectionRepository.save(entity);

        try {
            return new UploadStatus(savedInspection.getId(), true, "Inspection uploaded successfully.");
        } catch (Exception e) {
            return new UploadStatus(null, false, "Failed to upload inspection: " + e.getMessage());
        }
    }

    @Override
    public UploadStatus liveInspection(Inspection inspection) {
        Inspection savedInspections = inspectionRepository.save(inspection);

        return savedInspections != null ?
                new UploadStatus(savedInspections.getId(), true, "Inspection live uploaded successfully.") :
                new UploadStatus(null, false, "Failed to live upload inspection.");
    }
}
