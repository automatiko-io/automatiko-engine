package io.automatik.engine.addons.process.management.model;

public class ProcessDTO {

    private String id;

    private String version;

    private String name;

    private String description;

    private String image;

    private long activeInstances;

    public ProcessDTO() {

    }

    public ProcessDTO(String id, String version, String name, String description, String image, long activeInstances) {
        this.id = id;
        this.version = version == null ? "" : version;
        this.name = name;
        this.description = description;
        this.image = image;
        this.activeInstances = activeInstances;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public long getActiveInstances() {
        return activeInstances;
    }

    public void setActiveInstances(long activeInstances) {
        this.activeInstances = activeInstances;
    }

    @Override
    public String toString() {
        return "Process [id=" + id + ", version=" + version + ", name=" + name + ", description=" + description + ", image="
                + image + "]";
    }
}
