package br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "inspections")
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private PropertyListing property;

    @Column(nullable = false, length = 100)
    private String inspector;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(
            mappedBy = "inspection",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Image> images;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public PropertyListing getProperty() { return property; }

    public void setPropertyId(PropertyListing property) { this.property = property; }

    public String getInspector() { return inspector; }
    public void setInspector(String inspector) { this.inspector = inspector; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<Image> getPhotos() { return images; }
    public void setPhotos(List<Image> images) { this.images = images; }
}