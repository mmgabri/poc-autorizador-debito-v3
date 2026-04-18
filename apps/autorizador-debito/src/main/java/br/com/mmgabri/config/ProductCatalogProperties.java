package br.com.mmgabri.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "product-catalog")
public class ProductCatalogProperties {

    private List<ProductConfig> products = new ArrayList<>();

    public List<ProductConfig> getProducts() {
        return products;
    }

    public void setProducts(List<ProductConfig> products) {
        this.products = products;
    }
}
