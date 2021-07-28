package io.automatiko.engine.api.workflow;

import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError;

public class ServiceExecutionError extends WorkItemExecutionError {

    private static final long serialVersionUID = 7916428765937877492L;

    public ServiceExecutionError(String errorCode, Throwable e) {
        super(errorCode, e);
    }

    public ServiceExecutionError(String errorCode) {
        super(errorCode);
    }

    public ServiceExecutionError(String errorCode, String errorDetails, Throwable e) {
        super(errorCode, errorDetails, e);
    }

    public ServiceExecutionError(String errorCode, String errorDetails) {
        super(errorCode, errorDetails);
    }

    public ServiceExecutionError(String errorCode, Object errorData) {
        super(errorCode, errorData);
    }

    public ServiceExecutionError(String errorCode, String errorDetails, Object errorData) {
        super(errorCode, errorDetails, errorData);
    }

    public ServiceExecutionError(String message, String errorCode, String errorDetails, Object errorData) {
        super(message, errorCode, errorDetails, errorData);
    }

    public ServiceExecutionError(String message, String errorCode, String errorDetails, Throwable e) {
        super(message, errorCode, errorDetails, e);
    }

    public ServiceExecutionError(String message, String errorCode, String errorDetails) {
        super(message, errorCode, errorDetails);
    }

}
