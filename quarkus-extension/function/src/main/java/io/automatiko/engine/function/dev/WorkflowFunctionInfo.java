package io.automatiko.engine.function.dev;

public class WorkflowFunctionInfo {

    private String name;
    private String endpoint;
    private String getInstructions;
    private String postInstructions;

    public WorkflowFunctionInfo() {

    }

    public WorkflowFunctionInfo(String name, String endpoint, String getInstructions, String postInstructions) {
        this.name = name;
        this.endpoint = endpoint;
        this.getInstructions = getInstructions;
        this.postInstructions = postInstructions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getGetInstructions() {
        return getInstructions;
    }

    public void setGetInstructions(String getInstructions) {
        this.getInstructions = getInstructions;
    }

    public String getPostInstructions() {
        return postInstructions;
    }

    public void setPostInstructions(String postInstructions) {
        this.postInstructions = postInstructions;
    }

    @Override
    public String toString() {
        return "WorkflowFunctionInfo [name=" + name + ", endpoint=" + endpoint + ", getInstructions=" + getInstructions
                + ", postInstructions=" + postInstructions + "]";
    }

}
