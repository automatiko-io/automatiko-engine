package io.automatiko.engine.addons.process.management.model;

import java.util.Collection;
import java.util.List;

public class ProcessInstanceDetailsDTO {

    private String id;

    private String processId;

    private String businessKey;

    private String description;

    private int state;

    private Collection<String> tags;

    private boolean failed;

    private String image;

    private List<ProcessInstanceDTO> subprocesses;

    private Object variables;

    private List<String> versionedVariables;

    private List<ErrorInfoDTO> errors;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<ProcessInstanceDTO> getSubprocesses() {
        return subprocesses;
    }

    public void setSubprocesses(List<ProcessInstanceDTO> subprocesses) {
        this.subprocesses = subprocesses;
    }

    public Object getVariables() {
        return variables;
    }

    public void setVariables(Object variables) {
        this.variables = variables;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public List<String> getVersionedVariables() {
        return versionedVariables;
    }

    public void setVersionedVariables(List<String> versionedVariables) {
        this.versionedVariables = versionedVariables;
    }

    public List<ErrorInfoDTO> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorInfoDTO> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "ProcessInstanceDetails [id=" + id + ", businessKey=" + businessKey + ", description=" + description
                + ", tags=" + tags + ", failed=" + failed + ", image=" + image + ", subprocesses=" + subprocesses
                + ", variables=" + variables + "]";
    }

}
