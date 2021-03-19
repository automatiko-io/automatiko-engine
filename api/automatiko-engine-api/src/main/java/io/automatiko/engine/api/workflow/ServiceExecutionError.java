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

}
