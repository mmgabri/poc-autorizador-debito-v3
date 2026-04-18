package br.com.mmgabri.application.exceptions;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String productName) {
        super("Produto não encontrado no YAML: " + productName);
    }
}
