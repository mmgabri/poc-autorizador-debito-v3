package br.com.mmgabri.application.services;

import br.com.mmgabri.config.ProductCatalogProperties;
import br.com.mmgabri.config.ProductConfig;
import br.com.mmgabri.application.exceptions.ProductNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductCatalogService {

    private final ProductCatalogProperties properties;
    private Map<String, ProductConfig> byProductName = Map.of();

    @PostConstruct
    void init() {
        Map<String, ProductConfig> tmp = new HashMap<>();

        for (ProductConfig p : properties.getProducts()) {
            if (p.getProductName() == null || p.getProductName().isBlank()) continue;

            if (tmp.put(p.getProductName(), p) != null) {
                throw new IllegalStateException("productName duplicado no YAML: " + p.getProductName());
            }
        }

        this.byProductName = Map.copyOf(tmp);
    }

    public ProductConfig findByProductName(String productName) {
        var cfg = byProductName.get(productName);
        if (cfg == null) {
            throw new ProductNotFoundException(productName);
        }
        return cfg;
    }
}

