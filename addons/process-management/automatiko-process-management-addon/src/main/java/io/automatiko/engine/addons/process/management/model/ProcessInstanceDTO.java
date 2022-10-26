package io.automatiko.engine.addons.process.management.model;

import java.util.Collection;

public class ProcessInstanceDTO {

    private String id;

    private String compositeId;

    private String businessKey;

    private String description;

    private int state;

    private Collection<String> tags;

    private boolean failed;

    private String processId;

    public ProcessInstanceDTO() {

    }

    public ProcessInstanceDTO(String id, String parentId, String businessKey, String description, Collection<String> tags,
            boolean failed,
            String processId, int state) {
        this.id = id;
        this.businessKey = businessKey;
        this.description = description;
        this.tags = tags;
        this.failed = failed;
        this.processId = processId;
        this.state = state;
        this.compositeId = parentId != null ? parentId + ":" + id : id;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCompositeId() {
        return compositeId;
    }

    public void setCompositeId(String compositeId) {
        this.compositeId = compositeId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Collection<String> getTags() {
        return tags;
    }

    public void setTags(Collection<String> tags) {
        this.tags = tags;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    @Override
    public String toString() {
        return "ProcessInstance [id=" + id + ", businessKey=" + businessKey + ", description=" + description + ", tags="
                + tags + ", failed=" + failed + "]";
    }

}
