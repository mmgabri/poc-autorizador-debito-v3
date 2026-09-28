package br.com.itau.debit.authorizer.common.v1;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Ensures catalog consistency: the annotations (source, type) are redundant with
 * the code itself, so this test prevents them from diverging; it also enforces
 * that every code carries its ISO 8583 response code mapping.
 */
class ReasonCodeCatalogTest {

    private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z]{2}-[BT][0-9]{3}");
    private static final Pattern ISO_RESPONSE_CODE_FORMAT = Pattern.compile("[0-9A-Z]{2}");

    private static final Map<String, Source> SOURCE_BY_PREFIX = Map.of(
            "AU", Source.SOURCE_AUTHORIZER,
            "EN", Source.SOURCE_ENRICHMENT,
            "RE", Source.SOURCE_RULES,
            "SE", Source.SOURCE_SECURITY,
            "LI", Source.SOURCE_LIMIT,
            "AP", Source.SOURCE_ACCOUNT_POSTING,
            "AC", Source.SOURCE_ACCOUNT,
            "AF", Source.SOURCE_ANTIFRAUD
    );

    private static final List<GeneratedExtension<DescriptorProtos.EnumValueOptions, String>> ISO_RESPONSE_CODES = List.of(
            ReasonCodeOptions.isoResponseCodeVisaNac,
            ReasonCodeOptions.isoResponseCodeVisaInt,
            ReasonCodeOptions.isoResponseCodeMastNac,
            ReasonCodeOptions.isoResponseCodeMastInt
    );

    @Test
    @DisplayName("every code follows the format <source>-<B|T><3 digits>")
    void code_format() {
        for (var reasonCode : catalog()) {
            var code = optionsOf(reasonCode).getExtension(ReasonCodeOptions.code);
            assertTrue(CODE_FORMAT.matcher(code).matches(), reasonCode + " has an invalid code: '" + code + "'");
        }
    }

    @Test
    @DisplayName("no code is repeated")
    void codes_are_unique() {
        var seen = new HashSet<String>();
        for (var reasonCode : catalog()) {
            var code = optionsOf(reasonCode).getExtension(ReasonCodeOptions.code);
            if (!seen.add(code)) {
                fail("Duplicate code: " + code + " (" + reasonCode + ")");
            }
        }
    }

    @Test
    @DisplayName("prefix matches the source")
    void prefix_matches_source() {
        for (var reasonCode : catalog()) {
            var options = optionsOf(reasonCode);
            var code = options.getExtension(ReasonCodeOptions.code);
            var expectedSource = SOURCE_BY_PREFIX.get(code.substring(0, 2));
            var actualSource = options.getExtension(ReasonCodeOptions.source);
            assertEquals(expectedSource, actualSource, reasonCode + " (" + code + ")");
        }
    }

    @Test
    @DisplayName("B/T letter matches the type (B = business, T = technical)")
    void type_letter_matches_type() {
        for (var reasonCode : catalog()) {
            var options = optionsOf(reasonCode);
            var code = options.getExtension(ReasonCodeOptions.code);
            var expectedType = code.charAt(3) == 'B' ? ReasonType.REASON_TYPE_BUSINESS : ReasonType.REASON_TYPE_TECHNICAL;
            var actualType = options.getExtension(ReasonCodeOptions.type);
            assertEquals(expectedType, actualType, reasonCode + " (" + code + ")");
        }
    }

    @Test
    @DisplayName("every code has a description")
    void description_is_present() {
        for (var reasonCode : catalog()) {
            var description = optionsOf(reasonCode).getExtension(ReasonCodeOptions.description);
            assertFalse(description.isBlank(), reasonCode + " without description");
        }
    }

    @Test
    @DisplayName("every code maps to a DE39 response code for each brand and region")
    void iso_response_codes_are_present() {
        for (var reasonCode : catalog()) {
            var options = optionsOf(reasonCode);
            for (var extension : ISO_RESPONSE_CODES) {
                var isoCode = options.getExtension(extension);
                assertTrue(ISO_RESPONSE_CODE_FORMAT.matcher(isoCode).matches(),
                        reasonCode + " has an invalid " + extension.getDescriptor().getName() + ": '" + isoCode + "'");
            }
        }
    }

    @Test
    @DisplayName("only technical codes may have an uncertain outcome")
    void outcome_uncertain_only_on_technical_codes() {
        for (var reasonCode : catalog()) {
            var options = optionsOf(reasonCode);
            if (options.getExtension(ReasonCodeOptions.outcomeUncertain)) {
                assertEquals(ReasonType.REASON_TYPE_TECHNICAL, options.getExtension(ReasonCodeOptions.type), reasonCode.toString());
            }
        }
    }

    private static List<ReasonCode> catalog() {
        return Stream.of(ReasonCode.values())
                .filter(reasonCode -> reasonCode != ReasonCode.REASON_CODE_UNSPECIFIED && reasonCode != ReasonCode.UNRECOGNIZED)
                .toList();
    }

    private static DescriptorProtos.EnumValueOptions optionsOf(ReasonCode reasonCode) {
        var descriptor = reasonCode.getValueDescriptor();
        return descriptor.getOptions();
    }
}
