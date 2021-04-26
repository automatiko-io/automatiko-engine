
package io.automatiko.engine.workflow;

import java.util.HashMap;
import java.util.Map;

import io.automatiko.engine.api.workflow.WorkItem;

public class BaseWorkItem implements WorkItem {

    private final String id;
    private final String processInstanceId;
    private final String nodeInstanceId;
    private final String name;

    private final int state;
    private String phase;
    private String phaseStatus;

    private String referenceId;

    private String formLink;

    private Map<String, Object> parameters;
    private Map<String, Object> results;

    public BaseWorkItem(String processInstanceId, String nodeInstanceId, String id, String referenceId, String name,
            int state, String phase, String phaseStatus, Map<String, Object> results, String formLink) {
        this.id = id;
        this.processInstanceId = processInstanceId;
        this.nodeInstanceId = nodeInstanceId;
        this.referenceId = referenceId;
        this.name = name;
        this.state = state;
        this.phase = phase;
        this.phaseStatus = phaseStatus;
        this.results = results;
        this.formLink = formLink;
    }

    public BaseWorkItem(String processInstanceId, String nodeInstanceId, String id, String referenceId, String name,
            int state, String phase, String phaseStatus, Map<String, Object> parameters, Map<String, Object> results,
            String formLink) {
        this.id = id;
        this.processInstanceId = processInstanceId;
        this.nodeInstanceId = nodeInstanceId;
        this.referenceId = referenceId;
        this.name = name;
        this.state = state;
        this.phase = phase;
        this.phaseStatus = phaseStatus;
        this.parameters = parameters;
        this.results = results;
        this.formLink = formLink;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public Map<String, Object> getParameters() {
        if (parameters == null) {
            return new HashMap<>();
        }
        return parameters;
    }

    @Override
    public String getPhase() {
        return phase;
    }

    @Override
    public String getPhaseStatus() {
        return phaseStatus;
    }

    @Override
    public Map<String, Object> getResults() {
        if (results == null) {
            return new HashMap<>();
        }
        return results;
    }

    @Override
    public String getNodeInstanceId() {
        return nodeInstanceId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public String getReferenceId() {
        return referenceId;
    }

    @Override
    public String getFormLink() {
        return formLink;
    }

    @Override
    public String toString() {
        return "WorkItem [id=" + id + ", name=" + name + ", state=" + state + ", phase=" + phase + ", phaseStatus="
                + phaseStatus + "]";
    }

}
