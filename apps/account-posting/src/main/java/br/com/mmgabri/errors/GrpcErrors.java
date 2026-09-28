package br.com.mmgabri.errors;

import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import br.com.itau.debit.authorizer.common.v1.ReasonCodeOptions;
import com.google.protobuf.Any;
import com.google.rpc.ErrorInfo;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Technical error in the native gRPC model: status code + google.rpc.ErrorInfo
 * (reason = catalog code, domain = this service).
 * Business errors do NOT go through here - they travel as OK + BusinessResult.
 */
public final class GrpcErrors {

    private static final String DOMAIN = "account-posting";

    // Catalog annotations read once (class loading); read-only afterwards.
    private static final Map<ReasonCode, String> CODE_BY_REASON = new EnumMap<>(ReasonCode.class);
    private static final Map<ReasonCode, String> DESCRIPTION_BY_REASON = new EnumMap<>(ReasonCode.class);
    private static final Map<String, ReasonCode> REASON_BY_CODE = new HashMap<>();

    static {
        for (var reasonCode : ReasonCode.values()) {
            if (reasonCode == ReasonCode.UNRECOGNIZED || reasonCode == ReasonCode.REASON_CODE_UNSPECIFIED) {
                continue;
            }
            var descriptor = reasonCode.getValueDescriptor();
            var options = descriptor.getOptions();
            var code = options.getExtension(ReasonCodeOptions.code);
            var description = options.getExtension(ReasonCodeOptions.description);

            CODE_BY_REASON.put(reasonCode, code);
            DESCRIPTION_BY_REASON.put(reasonCode, description);
            REASON_BY_CODE.put(code, reasonCode);
        }
    }

    private GrpcErrors() {
    }

    public static StatusRuntimeException toStatusException(ReasonCode reasonCode, Status.Code statusCode) {
        var description = descriptionOf(reasonCode);
        return toStatusException(reasonCode, statusCode, description);
    }

    public static StatusRuntimeException toStatusException(ReasonCode reasonCode, Status.Code statusCode, String message) {
        var errorInfo = ErrorInfo.newBuilder()
                .setReason(codeOf(reasonCode))
                .setDomain(DOMAIN)
                .build();
        var detail = Any.pack(errorInfo);

        var status = com.google.rpc.Status.newBuilder()
                .setCode(statusCode.value())
                .setMessage(message)
                .addDetails(detail)
                .build();

        return StatusProto.toStatusRuntimeException(status);
    }

    /** Catalog code (e.g. "SE-B001"). */
    public static String codeOf(ReasonCode reasonCode) {
        return CODE_BY_REASON.get(reasonCode);
    }

    public static String descriptionOf(ReasonCode reasonCode) {
        return DESCRIPTION_BY_REASON.get(reasonCode);
    }

    /** Empty when the code does not exist in the catalog version known by this service. */
    public static Optional<ReasonCode> fromCode(String code) {
        var reasonCode = REASON_BY_CODE.get(code);
        return Optional.ofNullable(reasonCode);
    }
}
