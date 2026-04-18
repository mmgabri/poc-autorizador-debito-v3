package br.com.mmgabri.domains;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class FormatadorResponse {
    private Map<String, String> messageIso;
    private HeaderMessage header;
}

