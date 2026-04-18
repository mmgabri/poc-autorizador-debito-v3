package br.com.nicolebertolo.infrastructure.adapter.inbound.rest;

import br.com.nicolebertolo.application.port.inbound.ListingUseCase;
import br.com.nicolebertolo.application.service.ListingService;
import br.com.nicolebertolo.domain.exception.PropertyListingNotFoundException;
import br.com.nicolebertolo.domain.model.Inspection;
import br.com.nicolebertolo.domain.model.PropertyListing;
import br.com.nicolebertolo.domain.model.UploadStatus;
import br.com.nicolebertolo.shared.RecordConverter;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static br.com.nicolebertolo.shared.RecordConverter.convertFromEntity;
import static br.com.nicolebertolo.shared.RecordConverter.convertToEntity;

@RestController
@RequestMapping("/listings")
public class ListingController {

    private final ListingUseCase listingService;
    private final ExecutorService virtualExecutor =
            Executors.newVirtualThreadPerTaskExecutor();

    public ListingController(ListingUseCase listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/{id}")
    public CompletableFuture<PropertyListing> getListing(@PathVariable String id) {
        return CompletableFuture.supplyAsync(() -> {
                return convertFromEntity(listingService.getListing(id).orElseThrow(() ->
                        new PropertyListingNotFoundException("Property listing with ID " + id + " not found.")));
        }, virtualExecutor);
    }

    @GetMapping
    public CompletableFuture<List<PropertyListing>> streamListings(
            @RequestParam String city,
            @RequestParam(required = false) List<String> tags
    ) {
        return CompletableFuture.supplyAsync(() -> listingService.getAllListings(
                        new ListingUseCase.ListingFilter(city, tags)).stream().map(
                        RecordConverter::convertFromEntity
                ).toList()
                , virtualExecutor);
    }

    @PostMapping("/inspections")
    public CompletableFuture<UploadStatus> uploadInspections(@RequestBody List<Inspection> inspections) {
        return CompletableFuture.supplyAsync(() -> {
            var receivedCount = inspections.size();
            var successCount = new java.util.concurrent.atomic.AtomicInteger(0);

            inspections.forEach(inspection -> {
                var status = listingService.uploadInspection(inspection);
                if (status.isOk()) successCount.incrementAndGet();
            });

            return new UploadStatus(
                    java.util.UUID.randomUUID().toString(),
                    true,
                    String.format("Processed %d inspections successfully (%d total).",
                            successCount.get(), receivedCount)
            );
        }, virtualExecutor);
    }
}