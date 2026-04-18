package br.com.mmgabri.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
@Setter
public class ProductConfig {
    private String productName;
    private String operation;
    private boolean enabled;
    private String roteiroContabil;
    private String codigoLiteral;
    private List<String> configSeguranca = new ArrayList<>();
    private List<String> servicesToExecute = new ArrayList<>();
}

