package io.automatiko.engine.api.workflow;

import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError;

public class HandledServiceExecutionError extends ServiceExecutionError {

    private static final long serialVersionUID = -4329076954902540083L;

    public HandledServiceExecutionError(WorkItemExecutionError error) {
        super(error.getErrorCode(), error.getErrorDetails(), error.getErrorData());
        fillInStackTrace();
        if (error.getCause() != null) {
            initCause(error.getCause());
        }
    }

}
