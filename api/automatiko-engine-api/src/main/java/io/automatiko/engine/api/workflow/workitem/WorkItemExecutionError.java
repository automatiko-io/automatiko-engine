
package io.automatiko.engine.api.workflow.workitem;

public class WorkItemExecutionError extends RuntimeException {

    private static final long serialVersionUID = 4739415822214766299L;

    private final String errorCode;

    private final String errorDetails;

    public WorkItemExecutionError(String errorCode) {
        super("WorkItem execution failed with error code " + errorCode);
        this.errorCode = errorCode;
        this.errorDetails = "";
    }

    public WorkItemExecutionError(String errorCode, String errorDetails) {
        super("WorkItem execution failed with error code " + errorCode);
        this.errorCode = errorCode;
        this.errorDetails = errorDetails;
    }

    public WorkItemExecutionError(String message, String errorCode, String errorDetails) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetails = errorDetails;
    }

    public WorkItemExecutionError(String errorCode, Throwable e) {
        super("WorkItem execution failed with error code " + errorCode, e);
        this.errorCode = errorCode;
        this.errorDetails = "";
    }

    public WorkItemExecutionError(String errorCode, String errorDetails, Throwable e) {
        super("WorkItem execution failed with error code " + errorCode, e);
        this.errorCode = errorCode;
        this.errorDetails = errorDetails;
    }

    public WorkItemExecutionError(String message, String errorCode, String errorDetails, Throwable e) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetails = errorDetails;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    @Override
    public String toString() {
        return "WorkItemExecutionError [errorCode=" + errorCode + "]";
    }

}
