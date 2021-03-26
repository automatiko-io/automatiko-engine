
package io.automatiko.engine.api.workflow;

public class VariableNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 8031225233775014572L;

    private String processInstanceId;

    private String name;

    public VariableNotFoundException(String processInstanceId, String name) {
        super("Process instance with id " + processInstanceId + " does not have variable " + name);
        this.processInstanceId = processInstanceId;
        this.name = name;
    }

    public VariableNotFoundException(String processInstanceId, String name, Throwable cause) {
        super("Process instance with id " + processInstanceId + " does not have variable " + name);
        this.processInstanceId = processInstanceId;
        this.name = name;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getName() {
        return name;
    }

}
