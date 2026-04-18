package br.com.nicolebertolo.infrastructure.adapter.inbound.soap.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "UploadStatus", namespace = "http://nicolebertolo.com.br/listings")
@XmlAccessorType(XmlAccessType.FIELD)
public class UploadStatusSoap {

    @XmlElement(namespace = "http://nicolebertolo.com.br/listings")
    private String id;

    @XmlElement(namespace = "http://nicolebertolo.com.br/listings")
    private boolean ok;

    @XmlElement(namespace = "http://nicolebertolo.com.br/listings")
    private String message;

    // 🔹 JAXB precisa de construtor sem argumentos
    public UploadStatusSoap() {
    }

    public UploadStatusSoap(String id, boolean ok, String message) {
        this.id = id;
        this.ok = ok;
        this.message = message;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
