package br.com.mmgabri.domains;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response of POST /authorization (public REST contract).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AuthorizationResponse {
    private Map<String, String> messageIso;
    private HeaderMessage header;
}
