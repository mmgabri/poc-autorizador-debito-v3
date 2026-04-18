package br.com.nicolebertolo.infrastructure.adapter.outbound.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "images")
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] data;
    @Column(nullable = false, length = 100)
    private String mime;
    @Column(length = 255)
    private String caption;

    @ManyToOne
    @JoinColumn(name = "property_id")
    private PropertyListing property;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id")
    private Inspection inspection;

    // Getters e setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public PropertyListing getProperty() { return property; }
    public void setProperty(PropertyListing property) { this.property = property; }
}