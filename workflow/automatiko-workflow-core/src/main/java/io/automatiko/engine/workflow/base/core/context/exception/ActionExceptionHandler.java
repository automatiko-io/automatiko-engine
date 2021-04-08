
package io.automatiko.engine.workflow.base.core.context.exception;

import java.io.Serializable;

import io.automatiko.engine.workflow.process.core.ProcessAction;

public class ActionExceptionHandler implements ExceptionHandler, Serializable {

    private static final long serialVersionUID = 510l;

    private String faultVariable;
    private ProcessAction action;

    private Integer retryAfter;

    private Integer retryIncrement;

    private Float retryIncrementMultiplier;

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
        if (retryLimit != null) {
            this.retryAfter = retryAfter;
        }
    }

    public Integer getRetryLimit() {
        return retryLimit;
    }

    public void setRetryLimit(Integer retryLimit) {
        if (retryLimit != null) {
            this.retryLimit = retryLimit;
        }
    }

    public Integer getRetryIncrement() {
        return retryIncrement;
    }

    public void setRetryIncrement(Integer retryIncrement) {
        this.retryIncrement = retryIncrement;
    }

    public Float getRetryIncrementMultiplier() {
        return retryIncrementMultiplier;
    }

    public void setRetryIncrementMultiplier(Float retryIncrementMultiplier) {
        this.retryIncrementMultiplier = retryIncrementMultiplier;
    }

    @Override
    public String toString() {
        return "ActionExceptionHandler [faultVariable=" + faultVariable + ", action=" + action + ", retryAfter=" + retryAfter
                + ", retryLimit=" + retryLimit + "]";
    }

}
