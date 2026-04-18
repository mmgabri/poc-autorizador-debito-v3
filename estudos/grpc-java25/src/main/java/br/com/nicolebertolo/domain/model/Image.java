package br.com.nicolebertolo.domain.model;

public record Image(
        String id,
        byte[] data,
        String mime,
        String caption
) {}