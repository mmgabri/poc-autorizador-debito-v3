package br.com.nicolebertolo.infrastructure.adapter.inbound.soap.schema;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "inspection", namespace = "http://nicolebertolo.com.br/listings")
@XmlAccessorType(XmlAccessType.FIELD)
public class InspectionSoap {

    @XmlElement(namespace = "http://nicolebertolo.com.br/listings")
    private String id;

    @XmlElement(namespace = "http://nicolebertolo.com.br/listings")
    private String propertyId;

    @XmlElement(namespace = "http://nicolebertolo.com.br/listings")
    private String inspector;

    @XmlElement(namespace = "http://nicolebertolo.com.br/listings")
    private String notes;

    @XmlElementWrapper(name = "photos", namespace = "http://nicolebertolo.com.br/listings")
    @XmlElement(name = "photos", namespace = "http://nicolebertolo.com.br/listings")
    private List<ImageSoap> photos;

    // ✅ Construtor vazio obrigatório para JAXB
    public InspectionSoap() {}

    public InspectionSoap(String id, String propertyId, String inspector, String notes, List<ImageSoap> photos) {
        this.id = id;
        this.propertyId = propertyId;
        this.inspector = inspector;
        this.notes = notes;
        this.photos = photos;
    }

    // ✅ Getters e Setters obrigatórios
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getInspector() {
        return inspector;
    }

    public void setInspector(String inspector) {
        this.inspector = inspector;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<ImageSoap> getPhotos() {
        return photos;
    }

    public void setPhotos(List<ImageSoap> photos) {
        this.photos = photos;
    }
}
