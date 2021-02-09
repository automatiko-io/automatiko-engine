package io.automatiko.engine.service.dev;

public class WorkflowInfo {

    private String id;
    private String name;
    private boolean publicProcess;

    private String description;

    public WorkflowInfo() {

    }

    public WorkflowInfo(String id, String name, boolean publicProcess, String description) {
        this.id = id;
        this.name = name;
        this.publicProcess = publicProcess;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPublicProcess() {
        return publicProcess;
    }

    public void setPublicProcess(boolean publicProcess) {
        this.publicProcess = publicProcess;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "WorkflowInfo [id=" + id + ", name=" + name + ", publicProcess=" + publicProcess + ", description=" + description
                + "]";
    }

}
