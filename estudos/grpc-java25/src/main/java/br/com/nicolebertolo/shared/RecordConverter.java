package br.com.nicolebertolo.shared;

import br.com.nicolebertolo.domain.model.Image;
import br.com.nicolebertolo.domain.model.Inspection;
import br.com.nicolebertolo.domain.model.PropertyListing;

public class RecordConverter {

    public static PropertyListing convertFromEntity(br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.PropertyListing entity) {
        return new PropertyListing(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getFeatures(),
                entity.getImages().stream()
                        .map(imageEntity -> new br.com.nicolebertolo.domain.model.Image(
                                imageEntity.getId(),
                                imageEntity.getData(),
                                imageEntity.getMime(),
                                imageEntity.getCaption()
                        ))
                        .toList()
        );
    }

    public static br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.PropertyListing convertToEntity(PropertyListing listing) {
        var entity = new br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.PropertyListing();
        entity.setId(listing.id());
        entity.setTitle(listing.title());
        entity.setDescription(listing.description());
        entity.setFeatures(listing.features());
        entity.setImages(listing.images().stream()
                .map(image -> {
                    var imageEntity = new br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.Image();
                    imageEntity.setId(image.id());
                    imageEntity.setData(image.data());
                    imageEntity.setMime(image.mime());
                    imageEntity.setCaption(image.caption());
                    return imageEntity;
                })
                .toList());
        return entity;
    }

    public static Inspection convertFromEntity(br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.Inspection entity) {
        return new Inspection(
                entity.getId(),
                entity.getProperty().getId(),
                entity.getInspector(),
                entity.getNotes(),
                entity.getPhotos().stream()
                        .map(photoEntity -> new br.com.nicolebertolo.domain.model.Image(
                                photoEntity.getId(),
                                photoEntity.getData(),
                                photoEntity.getMime(),
                                photoEntity.getCaption()
                        ))
                        .toList()
        );
    }

    public static br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.Inspection convertToEntity(Inspection inspection) {
        var entity = new br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.Inspection();
        entity.setId(inspection.id());
        // Note: PropertyListing entity should be set separately, as we only have propertyId here
        entity.setInspector(inspection.inspector());
        entity.setNotes(inspection.notes());
        entity.setPhotos(inspection.photos().stream()
                .map(photo -> {
                    var photoEntity = new br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.Image();
                    photoEntity.setId(photo.id());
                    photoEntity.setData(photo.data());
                    photoEntity.setMime(photo.mime());
                    photoEntity.setCaption(photo.caption());
                    return photoEntity;
                })
                .toList());
        return entity;
    }

    public static Image convertToDomain(br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity.Image entity) {
        return new Image(
                entity.getId(),
                entity.getData(),
                entity.getMime(),
                entity.getCaption()
        );
    }
}
