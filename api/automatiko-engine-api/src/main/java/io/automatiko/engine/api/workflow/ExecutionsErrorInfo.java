package io.automatiko.engine.api.workflow;

public class ExecutionsErrorInfo {

    private final String failedNodeId;

    private final String errorId;

    private final String errorMessage;

    private final String errorDetails;

    public ExecutionsErrorInfo(String failedNodeId, String errorId, String errorMessage, String errorDetails) {
        this.failedNodeId = failedNodeId;
        this.errorId = errorId;
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails;
    }

    public String getFailedNodeId() {
        return failedNodeId;
    }

    public String getErrorId() {
        return errorId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    @Override
    public String toString() {
        return "ExecutionsErrorInfo [failedNodeId=" + failedNodeId + ", errorId=" + errorId + ", errorMessage=" + errorMessage
                + ", errorDetails=" + errorDetails + "]";
    }

}
