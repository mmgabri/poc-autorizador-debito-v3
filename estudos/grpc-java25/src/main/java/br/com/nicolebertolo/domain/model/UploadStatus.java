package br.com.nicolebertolo.domain.model;


public class UploadStatus {
    private String id;
    private boolean ok;
    private String message;

    public UploadStatus(String id, boolean ok, String message) {
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