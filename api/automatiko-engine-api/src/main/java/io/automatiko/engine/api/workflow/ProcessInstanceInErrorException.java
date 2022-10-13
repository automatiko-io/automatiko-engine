
package io.automatiko.engine.api.workflow;

public class ProcessInstanceInErrorException extends RuntimeException {

    private static final long serialVersionUID = 8031225233775014572L;

    private String processInstanceId;

    public ProcessInstanceInErrorException(String processInstanceId) {
        super("Process instance with id " + processInstanceId + " in in error state");
        this.processInstanceId = processInstanceId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

}
