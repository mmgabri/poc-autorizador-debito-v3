package br.com.mmgabri.application.utils;

import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import br.com.itau.debit.authorizer.common.v1.ReasonCodeOptions;
import br.com.itau.debit.authorizer.common.v1.ReasonType;
import com.google.protobuf.DescriptorProtos;

import java.util.Optional;

/**
 * Reads the annotations of the reason code catalog.
 */
public final class ReasonCodes {

    private ReasonCodes() {
    }

    /** Catalog code (e.g. "AC-B001"). */
    public static String codeOf(ReasonCode reasonCode) {
        var options = optionsOf(reasonCode);
        return options.getExtension(ReasonCodeOptions.code);
    }

    public static boolean isBusiness(ReasonCode reasonCode) {
        var options = optionsOf(reasonCode);
        var type = options.getExtension(ReasonCodeOptions.type);
        return type == ReasonType.REASON_TYPE_BUSINESS;
    }

    /** Empty when the code does not exist in the catalog version known by this service. */
    public static Optional<ReasonCode> fromCode(String code) {
        for (var reasonCode : ReasonCode.values()) {
            if (reasonCode == ReasonCode.UNRECOGNIZED || reasonCode == ReasonCode.REASON_CODE_UNSPECIFIED) {
                continue;
            }
            if (codeOf(reasonCode).equals(code)) {
                return Optional.of(reasonCode);
            }
        }
        return Optional.empty();
    }

    private static DescriptorProtos.EnumValueOptions optionsOf(ReasonCode reasonCode) {
        var descriptor = reasonCode.getValueDescriptor();
        return descriptor.getOptions();
    }
}
