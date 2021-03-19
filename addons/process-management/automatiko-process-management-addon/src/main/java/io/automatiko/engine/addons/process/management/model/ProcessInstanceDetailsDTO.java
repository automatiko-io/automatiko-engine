package io.automatiko.engine.addons.process.management.model;

import java.util.Collection;
import java.util.List;

public class ProcessInstanceDetailsDTO {

    private String id;

    private String processId;

    private String businessKey;

    private String description;

    private Collection<String> tags;

    private boolean failed;

    private String image;

    private List<ProcessInstanceDTO> subprocesses;

    private Object variables;

    private String errorId;

    private String errorMessage;

    private String errorDetails;

    private String failedNodeId;

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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorId() {
        return errorId;
    }

    public void setErrorId(String errorId) {
        this.errorId = errorId;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public String getFailedNodeId() {
        return failedNodeId;
    }

    public void setFailedNodeId(String failedNodeId) {
        this.failedNodeId = failedNodeId;
    }

    @Override
    public String toString() {
        return "ProcessInstanceDetails [id=" + id + ", businessKey=" + businessKey + ", description=" + description
                + ", tags=" + tags + ", failed=" + failed + ", image=" + image + ", subprocesses=" + subprocesses
                + ", variables=" + variables + "]";
    }

}
