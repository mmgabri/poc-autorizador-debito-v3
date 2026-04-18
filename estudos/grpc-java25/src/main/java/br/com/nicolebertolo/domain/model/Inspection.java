package br.com.nicolebertolo.domain.model;

import java.util.List;


public record Inspection(
        String id,
        String propertyId,
        String inspector,
        String notes,
        List<Image> photos
) {}