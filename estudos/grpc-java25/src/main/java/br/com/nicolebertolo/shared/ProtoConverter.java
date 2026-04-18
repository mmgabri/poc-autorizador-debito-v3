package br.com.nicolebertolo.shared;

import br.com.nicolebertolo.domain.model.Inspection;
import br.com.nicolebertolo.infrastructure.adapter.inbound.soap.schema.InspectionSoap;
import br.com.nicolebertolo.proto.listing.Image;
import br.com.nicolebertolo.proto.listing.PropertyListing;

public class ProtoConverter {

    public static PropertyListing convertFromDomain(br.com.nicolebertolo.domain.model.PropertyListing listing) {
        return PropertyListing.newBuilder()
                .setId(listing.id())
                .setTitle(listing.title())
                .setDescription(listing.description())
                .addAllFeatures(listing.features())
                .addAllImages(listing.images().stream().map(ProtoConverter::convertFromDomain).toList())
                .build();
    }

    public static Image convertFromDomain(br.com.nicolebertolo.domain.model.Image image) {
        return Image.newBuilder()
                .setId(image.id())
                .setData(com.google.protobuf.ByteString.copyFrom(image.data()))
                .setCaption(image.caption())
                .setMime(image.mime())
                .build();
    }

    public static br.com.nicolebertolo.domain.model.Image convertToDomain(Image protoImage) {
        return new br.com.nicolebertolo.domain.model.Image(
                protoImage.getId(),
                protoImage.getData().toByteArray(),
                protoImage.getMime(),
                protoImage.getCaption()
        );
    }

    public static Inspection convertToDomain(br.com.nicolebertolo.proto.listing.Inspection protoInspection) {
        return new Inspection(
                protoInspection.getId(),
                protoInspection.getPropertyId(),
                protoInspection.getInspector(),
                protoInspection.getNotes(),
                protoInspection.getPhotosList().stream().map(ProtoConverter::convertToDomain).toList()
        );
    }

    public static Inspection convertToDomain(InspectionSoap inspectionSoap) {
        return new Inspection(
                inspectionSoap.getId(),
                inspectionSoap.getPropertyId(),
                inspectionSoap.getInspector(),
                inspectionSoap.getNotes(),
                inspectionSoap.getPhotos().stream().map(soapPhoto ->
                        new br.com.nicolebertolo.domain.model.Image(
                                soapPhoto.getId(),
                                soapPhoto.getData(),
                                soapPhoto.getMime(),
                                soapPhoto.getCaption()
                        )
                ).toList()
        );

    }
}
