package io.automatiko.engine.quarkus.functionflow.dev;

public class WorkflowFunctionFlowInfo {

    private String name;
    private String endpoint;
    private String binaryInstructions;
    private String structuredInstructions;

    private String curlBinary;
    private String curlStructure;

    public WorkflowFunctionFlowInfo() {

    }

    public WorkflowFunctionFlowInfo(String name, String endpoint, String binaryInstructions, String curlBinary,
            String structuredInstructions, String curlStructure) {
        this.name = name;
        this.endpoint = endpoint;
        this.binaryInstructions = binaryInstructions;
        this.curlBinary = curlBinary;
        this.structuredInstructions = structuredInstructions;
        this.curlStructure = curlStructure;
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

    public String getBinaryInstructions() {
        return binaryInstructions;
    }

    public void setBinaryInstructions(String binaryInstructions) {
        this.binaryInstructions = binaryInstructions;
    }

    public String getStructuredInstructions() {
        return structuredInstructions;
    }

    public void setStructuredInstructions(String structuredInstructions) {
        this.structuredInstructions = structuredInstructions;
    }

    public String getCurlBinary() {
        return curlBinary;
    }

    public void setCurlBinary(String curlBinary) {
        this.curlBinary = curlBinary;
    }

    public String getCurlStructure() {
        return curlStructure;
    }

    public void setCurlStructure(String curlStructure) {
        this.curlStructure = curlStructure;
    }

    @Override
    public String toString() {
        return "WorkflowFunctionFlowInfo [name=" + name + ", endpoint=" + endpoint + ", binaryInstructions="
                + binaryInstructions + ", structuredInstructions=" + structuredInstructions + "]";
    }

}
