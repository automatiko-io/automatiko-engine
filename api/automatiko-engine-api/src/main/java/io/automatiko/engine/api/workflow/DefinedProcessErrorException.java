
package io.automatiko.engine.api.workflow;

/**
 * Thrown when there is error end node defined in the process
 * 
 */
public class DefinedProcessErrorException extends RuntimeException {

    private static final long serialVersionUID = 8031225233775014572L;

    private String processInstanceId;
    private String errorCode;
    private Object error;

    public DefinedProcessErrorException(String processInstanceId, String errorCode, Object error) {
        super("Process instance with id " + processInstanceId + " was aborted with defined error code " + errorCode);
        this.processInstanceId = processInstanceId;
        this.errorCode = errorCode;
        this.error = error;
    }

    /**
     * Returns process instance id of the instance that failed.
     * 
     * @return process instance id
     */
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getError() {
        return error;
    }

}
