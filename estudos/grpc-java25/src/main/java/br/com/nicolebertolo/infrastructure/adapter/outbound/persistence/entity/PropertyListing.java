package br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity;

import br.com.nicolebertolo.shared.TagsConverter;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "property_listings")
public class  PropertyListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Convert(converter = TagsConverter.class)
    @Column(name = "features", columnDefinition = "TEXT")
    private List<String> features;

    @OneToMany(
            mappedBy = "property",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Image> images;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Convert(converter = TagsConverter.class)
    @Column(name = "tags", columnDefinition = "TEXT")
    private List<String> tags;

    // Getters e setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }

    public List<Image> getImages() { return images; }
    public void setImages(List<Image> images) { this.images = images; }

    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}