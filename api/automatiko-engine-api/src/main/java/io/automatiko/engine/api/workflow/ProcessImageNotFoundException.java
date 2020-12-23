package io.automatiko.engine.api.workflow;

public class ProcessImageNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProcessImageNotFoundException(String message) {
        super("Image for " + message + " was not found");
    }

}
