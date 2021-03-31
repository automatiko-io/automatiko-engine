package io.automatiko.engine.api.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessErrors {

    private List<ProcessError> errors = new ArrayList<>();

    public ProcessErrors(List<ProcessError> errors) {
        this.errors = errors;
    }

    public List<ProcessError> errors() {
        return errors;
    }

    public void retrigger(String nodeId) {
        this.errors.stream().filter(e -> e.failedNodeId().equals(nodeId)).findFirst().ifPresent(e -> e.retrigger());
    }

    public void skip(String nodeId) {
        this.errors.stream().filter(e -> e.failedNodeId().equals(nodeId)).findFirst().ifPresent(e -> e.skip());
    }

    public void retrigger() {
        for (ProcessError error : errors) {
            try {
                error.retrigger();
            } catch (Exception e) {

            }
        }
    }

    public void skip() {
        for (ProcessError error : errors) {
            try {
                error.skip();
            } catch (Exception e) {

            }
        }
    }

    public String failedNodeIds() {
        return this.errors.stream().map(e -> e.failedNodeId()).collect(Collectors.joining(","));
    }

    public String errorMessages() {
        return this.errors.stream().map(e -> e.errorMessage()).collect(Collectors.joining(","));
    }
}
