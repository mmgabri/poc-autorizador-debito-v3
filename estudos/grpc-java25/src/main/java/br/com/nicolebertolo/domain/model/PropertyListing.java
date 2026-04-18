package br.com.nicolebertolo.domain.model;

import java.util.List;

public record PropertyListing(
        String id,
        String title,
        String description,
        List<String> features,
        List<Image> images
) {}
