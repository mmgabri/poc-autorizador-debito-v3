package br.com.nicolebertolo.infrastructure.adapter.inbound.grpc;

import br.com.nicolebertolo.application.port.inbound.ListingUseCase;
import br.com.nicolebertolo.proto.listing.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static br.com.nicolebertolo.shared.ProtoConverter.convertFromDomain;
import static br.com.nicolebertolo.shared.ProtoConverter.convertToDomain;
import static br.com.nicolebertolo.shared.RecordConverter.convertFromEntity;
import static br.com.nicolebertolo.shared.RecordConverter.convertToEntity;
import static io.grpc.Status.INTERNAL;
import static io.grpc.Status.NOT_FOUND;


public class ListingGrpc extends ListingServiceGrpc.ListingServiceImplBase {

    Logger logger = Logger.getLogger(ListingGrpc.class.getName());

    private final ListingUseCase listingUseCase;

    public ListingGrpc(ListingUseCase listingUseCase) {
        this.listingUseCase = listingUseCase;
    }

    @Override
    public void getListing(GetRequest request, StreamObserver<PropertyListing> responseObserver) {
        logger.info("[ListingGRPC] - Received GetListing request for ID: " + request.getId());
        try {
            listingUseCase.getListing(request.getId())
                    .ifPresentOrElse(
                            listing -> {
                                responseObserver.onNext(convertFromDomain(convertFromEntity(listing)));
                                logger.info(
                                        "[ListingGRPC] - Successfully retrieved listing for ID: " + request.getId()
                                );
                                responseObserver.onCompleted();
                            },
                            () -> {
                                responseObserver.onError(
                                        NOT_FOUND
                                                .withDescription("Listing not found for ID: " + request.getId())
                                                .asRuntimeException()
                                );
                            }
                    );
        } catch (RuntimeException e) {
            responseObserver.onError(
                    INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void streamListings(ListingsFilter request, StreamObserver<PropertyListing> responseObserver) {
        logger.info("[ListingGRPC] - Received StreamListings request with filter: " + request);

        try {
            var filter = new ListingUseCase.ListingFilter(
                    request.getCity(),
                    request.getTagsList()
            );

            listingUseCase.getAllListings(filter).forEach(listing -> {
                responseObserver.onNext(convertFromDomain(convertFromEntity(listing)));
                logger.info(
                        "[ListingGRPC] - Streaming listing ID: " + listing.getId()
                );
            });
        } catch (RuntimeException e) {
            responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        } finally {
            responseObserver.onCompleted();
            logger.info("[ListingGRPC] - Completed StreamListings request.");
        }
    }

    @Override
    public StreamObserver<Inspection> uploadInspections(StreamObserver<UploadStatus> responseObserver) {
        logger.info("[ListingGRPC] - Received UploadInspections streaming request.");

        var receivedCount = new AtomicInteger(0);
        var successCount = new AtomicInteger(0);

        return new StreamObserver<>() {

            @Override
            public void onNext(Inspection inspection) {
                receivedCount.incrementAndGet();
                try {
                    var status = listingUseCase.uploadInspection(convertToDomain(inspection));
                    if (status.isOk()) successCount.incrementAndGet();

                    logger.info(String.format("[ListingGRPC] - Processed inspection ID: %s (success=%s)",
                            inspection.getId(), status.isOk()));

                } catch (RuntimeException e) {
                    logger.severe(
                            String.format("[ListingGRPC] - Error processing inspection ID: %s, err: %s",
                                    inspection.getId(), e)
                    );
                    responseObserver.onError(
                            INTERNAL
                                    .withDescription("Error processing inspection: " + e.getMessage())
                                    .asRuntimeException()
                    );
                }
            }
            @Override
            public void onError(Throwable t) {
                logger.severe( String.format("[ListingGRPC] - Streaming aborted: %s", t.getMessage()));
                responseObserver.onError(
                        Status.INTERNAL
                                .withDescription("Stream interrupted: " + t.getMessage())
                                .asRuntimeException()
                );
            }
            @Override
            public void onCompleted() {
                var summary = UploadStatus.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setOk(true)
                        .setMessage(String.format("Processed %d inspections successfully (%d total).",
                                successCount.get(), receivedCount.get()))
                        .build();

                responseObserver.onNext(summary);
                responseObserver.onCompleted();
                logger.info(String.format("[ListingGRPC] - Completed UploadInspections stream. Total: %s, Success: %s",
                        receivedCount.get(), successCount.get()));
            }
        };
    }

    @Override
    public StreamObserver<Inspection> liveInspection(StreamObserver<UploadStatus> responseObserver) {
        logger.info("[ListingGRPC] - Received LiveInspection streaming request.");

        return new StreamObserver<>() {
            @Override
            public void onNext(Inspection inspection) {
                try {
                    var domainInspection = convertToDomain(inspection);
                    var status = listingUseCase.liveInspection(convertToEntity(domainInspection));

                    var protoStatus = UploadStatus.newBuilder()
                            .setId(status.getId())
                            .setOk(status.isOk())
                            .setMessage(status.getMessage())
                            .build();

                    responseObserver.onNext(protoStatus);

                    logger.info(String.format(
                            "[ListingGRPC] - Processed live inspection ID: %s (success=%s)",
                            inspection.getId(), status.isOk()
                    ));
                } catch (Exception e) {
                    logger.severe(String.format(
                            "[ListingGRPC] - Error processing live inspection ID: %s -> %s",
                            inspection.getId(), e.getMessage()
                    ));
                    responseObserver.onError(
                            INTERNAL.withDescription("Error processing live inspection: " + e.getMessage())
                                    .asRuntimeException()
                    );
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.severe(String.format(
                        "[ListingGRPC] - LiveInspection stream aborted: %s", t.getMessage()
                ));
                responseObserver.onError(
                        INTERNAL.withDescription("LiveInspection stream interrupted: " + t.getMessage())
                                .asRuntimeException()
                );
            }

            @Override
            public void onCompleted() {
                logger.info("[ListingGRPC] - Completed LiveInspection stream.");
                responseObserver.onCompleted();
            }
        };
    }
}
