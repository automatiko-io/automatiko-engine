package io.automatiko.engine.api.workflow;

public interface ProcessError {

    String failedNodeId();

    String errorId();

    String errorMessage();

    String errorDetails();

    void retrigger();

    void skip();
}
