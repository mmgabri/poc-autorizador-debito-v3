package br.com.mmgabri.domains;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Header of the REST response (public contract): the JSON names are kept as-is
 * via {@link JsonProperty}. {@code reversal} is serialized as "reversal", same as before.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class HeaderMessage {
    private String transactionId;
    private String correlationId;

    @JsonProperty("bandeira")
    private String cardBrand;

    @JsonProperty("plataforma")
    private String platform;

    private String timestamp;
    private String message;
    private boolean reversal;
}
