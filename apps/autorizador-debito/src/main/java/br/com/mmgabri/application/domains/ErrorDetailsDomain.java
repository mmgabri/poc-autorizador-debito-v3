package br.com.mmgabri.application.domains;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorDetailsDomain {
    private String errorCode;
    private String errorDescription;
    private String de39;
    private String action;
}
