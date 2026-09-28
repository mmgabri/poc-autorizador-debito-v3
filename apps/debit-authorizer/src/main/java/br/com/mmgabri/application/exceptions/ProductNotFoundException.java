package br.com.mmgabri.application.exceptions;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String productName) {
        super("Product not found in the YAML catalog: " + productName);
    }
}
