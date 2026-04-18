package br.com.nicolebertolo.infrastructure.adapter.inbound.soap;

import br.com.nicolebertolo.application.port.inbound.ListingUseCase;
import br.com.nicolebertolo.domain.exception.PropertyListingNotFoundException;
import br.com.nicolebertolo.domain.model.Inspection;
import br.com.nicolebertolo.domain.model.PropertyListing;
import br.com.nicolebertolo.domain.model.UploadStatus;
import br.com.nicolebertolo.infrastructure.adapter.inbound.soap.schema.InspectionSoap;
import br.com.nicolebertolo.infrastructure.adapter.inbound.soap.schema.UploadStatusSoap;
import br.com.nicolebertolo.shared.RecordConverter;
import jakarta.xml.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static br.com.nicolebertolo.shared.ProtoConverter.convertToDomain;
import static br.com.nicolebertolo.shared.RecordConverter.convertFromEntity;

@Endpoint
public class ListingSoapEndpoint {

    private static final String NAMESPACE_URI = "http://nicolebertolo.com.br/listings";
    private static final Logger log = LoggerFactory.getLogger(ListingSoapEndpoint.class);

    private final ListingUseCase listingService;
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public ListingSoapEndpoint(ListingUseCase listingService) {
        this.listingService = listingService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetListingRequest")
    @ResponsePayload
    public GetListingResponse getListing(@RequestPayload GetListingRequest request) {
        log.info("[SOAP] GetListing ID={}", request.getId());

        var listing = listingService.getListing(request.getId())
                .orElseThrow(() -> new PropertyListingNotFoundException("Not found: " + request.getId()));

        var response = new GetListingResponse();
        response.setTitle(listing.getTitle());
        response.setDescription(listing.getDescription());
        response.setFeatures(listing.getFeatures());
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "StreamListingsRequest")
    @ResponsePayload
    public StreamListingsResponse streamListings(@RequestPayload StreamListingsRequest request) {
        log.info("[SOAP] Stream listings for city={}", request.getCity());

        var listings = listingService.getAllListings(
                new ListingUseCase.ListingFilter(request.getCity(), request.getTags())
        );

        var response = new StreamListingsResponse();
        response.setListings(listings.stream().map(RecordConverter::convertFromEntity).toList());
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "UploadInspectionsRequest")
    @ResponsePayload
    public UploadInspectionsResponse uploadInspections(@RequestPayload UploadInspectionsRequest request) {
        log.info("[SOAP] Uploading {} inspections", request.getInspections().size());

        var inspections = request.getInspections();
        var receivedCount = inspections.size();
        var successCount = new AtomicInteger(0);

        inspections.forEach(inspection -> {
            var status = listingService.uploadInspection(convertToDomain(inspection));
            if (status.isOk()) successCount.incrementAndGet();
        });

        var summary = new UploadStatusSoap(
                UUID.randomUUID().toString(),
                true,
                String.format("Processed %d inspections successfully (%d total).",
                        successCount.get(), receivedCount)
        );

        var response = new UploadInspectionsResponse();
        response.setUploadStatus(summary);
        return response;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "GetListingRequest", namespace = "http://nicolebertolo.com.br/listings")
    public static class GetListingRequest {

        @XmlElement(name = "id", namespace = "http://nicolebertolo.com.br/listings", required = true)
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "GetListingResponse", namespace = "http://nicolebertolo.com.br/listings")
    public static class GetListingResponse {
        @XmlElement(name = "title", namespace = "http://nicolebertolo.com.br/listings")
        private String title;

        @XmlElement(name = "description", namespace = "http://nicolebertolo.com.br/listings")
        private String description;

        @XmlElement(name = "features", namespace = "http://nicolebertolo.com.br/listings")
        private List<String> features;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<String> getFeatures() { return features; }
        public void setFeatures(List<String> features) { this.features = features; }
    }

    @XmlRootElement(name = "StreamListingsRequest", namespace = NAMESPACE_URI)
    public static class StreamListingsRequest {
        private String city;
        private List<String> tags;

        @XmlElement
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        @XmlElement
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    @XmlRootElement(name = "StreamListingsResponse", namespace = NAMESPACE_URI)
    public static class StreamListingsResponse {
        private List<PropertyListing> listings;
        @XmlElement
        public List<PropertyListing> getListings() { return listings; }
        public void setListings(List<PropertyListing> listings) { this.listings = listings; }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"inspections"})
    @XmlRootElement(name = "UploadInspectionsRequest", namespace = "http://nicolebertolo.com.br/listings")
    public static class UploadInspectionsRequest {

        @XmlElementWrapper(name = "inspections", namespace = "http://nicolebertolo.com.br/listings")
        @XmlElement(name = "inspection", namespace = "http://nicolebertolo.com.br/listings")
        private List<InspectionSoap> inspections;

        public List<InspectionSoap> getInspections() {
            if (inspections == null) {
                inspections = new ArrayList<>();
            }
            return inspections;
        }

        public void setInspections(List<InspectionSoap> inspections) {
            this.inspections = inspections;
        }
    }

    @XmlRootElement(name = "UploadInspectionsResponse", namespace = ListingSoapEndpoint.NAMESPACE_URI)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UploadInspectionsResponse {

        @XmlElement(name = "uploadStatus", namespace = ListingSoapEndpoint.NAMESPACE_URI)
        private UploadStatusSoap uploadStatus;

        public UploadInspectionsResponse() {
        }

        public UploadStatusSoap getUploadStatus() {
            return uploadStatus;
        }

        public void setUploadStatus(UploadStatusSoap uploadStatus) {
            this.uploadStatus = uploadStatus;
        }
    }
}
