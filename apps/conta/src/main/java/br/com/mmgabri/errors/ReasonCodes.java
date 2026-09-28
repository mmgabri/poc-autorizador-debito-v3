package br.com.mmgabri.errors;

import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import br.com.itau.debit.authorizer.common.v1.ReasonCodeOptions;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the catalog annotations (code and description).
 */
public final class ReasonCodes {

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

    private ReasonCodes() {
    }

    /** Catalog code (e.g. "AC-B001"). */
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
