
package io.automatik.engine.workflow;

import java.util.Map;

import io.automatik.engine.api.workflow.WorkItem;

public class BaseWorkItem implements WorkItem {

	private final String id;
	private final String nodeInstanceId;
	private final String name;

	private final int state;
	private String phase;
	private String phaseStatus;

	private Map<String, Object> parameters;
	private Map<String, Object> results;

	public BaseWorkItem(String nodeInstanceId, String id, String name, int state, String phase, String phaseStatus,
			Map<String, Object> results) {
		this.id = id;
		this.nodeInstanceId = nodeInstanceId;
		this.name = name;
		this.state = state;
		this.phase = phase;
		this.phaseStatus = phaseStatus;
		this.results = results;
	}

	public BaseWorkItem(String nodeInstanceId, String id, String name, int state, String phase, String phaseStatus,
			Map<String, Object> parameters, Map<String, Object> results) {
		this.id = id;
		this.nodeInstanceId = nodeInstanceId;
		this.name = name;
		this.state = state;
		this.phase = phase;
		this.phaseStatus = phaseStatus;
		this.parameters = parameters;
		this.results = results;
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
		return results;
	}

	@Override
	public String getNodeInstanceId() {
		return nodeInstanceId;
	}

	@Override
	public String toString() {
		return "WorkItem [id=" + id + ", name=" + name + ", state=" + state + ", phase=" + phase + ", phaseStatus="
				+ phaseStatus + "]";
	}

}
