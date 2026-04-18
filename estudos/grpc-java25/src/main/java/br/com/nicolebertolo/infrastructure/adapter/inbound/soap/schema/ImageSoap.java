package br.com.nicolebertolo.infrastructure.adapter.inbound.soap.schema;

import jakarta.xml.bind.annotation.*;

import java.util.Arrays;
import java.util.Base64;

@XmlRootElement(name = "Image", namespace = "http://nicolebertolo.com.br/listings")
@XmlAccessorType(XmlAccessType.FIELD)
public class ImageSoap {

    @XmlElement(namespace = "http://nicolebertolo.com.br/listings")
    private String id;

    @XmlElement(namespace = "http://nicolebertolo.com.br/listings")
    private byte[] data;

    @XmlElement(namespace = "http://nicolebertolo.com.br/listings")
    private String mime;

    @XmlElement(namespace = "http://nicolebertolo.com.br/listings")
    private String caption;

    public ImageSoap() {}

    public ImageSoap(String id, byte[] data, String mime, String caption) {
        this.id = id;
        this.data = data;
        this.mime = mime;
        this.caption = caption;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    @Override
    public String toString() {
        return "Image{" +
                "id='" + id + '\'' +
                ", data=" + Arrays.toString(data) +
                ", mime='" + mime + '\'' +
                ", caption='" + caption + '\'' +
                '}';
    }
}