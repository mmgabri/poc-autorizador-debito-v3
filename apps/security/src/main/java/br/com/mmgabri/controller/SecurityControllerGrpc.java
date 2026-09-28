package br.com.mmgabri.controller;

import br.com.itau.debit.authorizer.security.v1.ValidateSecurityRequest;
import br.com.itau.debit.authorizer.security.v1.ValidateSecurityResponse;
import br.com.itau.debit.authorizer.security.v1.SecurityServiceGrpc;
import br.com.mmgabri.services.SecurityService;
import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import br.com.mmgabri.errors.GrpcErrors;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.grpc.server.service.GrpcService;

import java.time.Duration;
import java.time.OffsetDateTime;

@GrpcService
@RequiredArgsConstructor
public class SecurityControllerGrpc extends SecurityServiceGrpc.SecurityServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(SecurityControllerGrpc.class);

    private final SecurityService securityService;

    @Override
    public void validateSecurity(ValidateSecurityRequest request, StreamObserver<ValidateSecurityResponse> responseObserver) {
        var startTime = OffsetDateTime.now();
        logger.debug("Received validateSecurity");
        try {
            var response = securityService.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime);

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error on validateSecurity: {}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error on validateSecurity", e);
            var error = GrpcErrors.toStatusException(ReasonCode.REASON_CODE_SECURITY_INTERNAL_ERROR, Status.Code.INTERNAL);
            responseObserver.onError(error);
        }
    }

    private void onSuccess(OffsetDateTime startTime) {
        logger.debug("Processing completed in {} ms", Duration.between(startTime, OffsetDateTime.now()).toMillis());
    }
}
