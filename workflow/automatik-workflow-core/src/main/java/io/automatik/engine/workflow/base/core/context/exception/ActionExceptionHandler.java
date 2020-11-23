
package io.automatik.engine.workflow.base.core.context.exception;

import java.io.Serializable;

import io.automatik.engine.workflow.process.core.ProcessAction;

public class ActionExceptionHandler implements ExceptionHandler, Serializable {

    private static final long serialVersionUID = 510l;

    private String faultVariable;
    private ProcessAction action;

    private Integer retryAfter;

    private Integer retryLimit = 3;

    public String getFaultVariable() {
        return faultVariable;
    }

    public void setFaultVariable(String faultVariable) {
        this.faultVariable = faultVariable;
    }

    public ProcessAction getAction() {
        return action;
    }

    public void setAction(ProcessAction action) {
        this.action = action;
    }

    public Integer getRetryAfter() {
        return retryAfter;
    }

    public void setRetryAfter(Integer retryAfter) {
        if (retryLimit != null && retryLimit > 0) {
            this.retryAfter = retryAfter;
        }
    }

    public Integer getRetryLimit() {
        return retryLimit;
    }

    public void setRetryLimit(Integer retryLimit) {
        if (retryLimit != null && retryLimit > 0) {
            this.retryLimit = retryLimit;
        }
    }

    @Override
    public String toString() {
        return "ActionExceptionHandler [faultVariable=" + faultVariable + ", action=" + action + ", retryAfter=" + retryAfter
                + ", retryLimit=" + retryLimit + "]";
    }

}
