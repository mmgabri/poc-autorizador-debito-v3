package br.com.nicolebertolo.domain.exception;

public class PropertyListingNotFoundException extends RuntimeException {
    public PropertyListingNotFoundException(String message) {
        super(message);
    }
}
