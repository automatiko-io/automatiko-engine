
package io.automatiko.engine.api.workflow;

public class NodeInstanceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 8031225233775014572L;

	private final String processInstanceId;
	private final String nodeInstanceId;

	public NodeInstanceNotFoundException(String processInstanceId, String nodeInstanceId) {
		super("Node instance with id " + nodeInstanceId + " not found within process instance " + processInstanceId);
		this.processInstanceId = processInstanceId;
		this.nodeInstanceId = nodeInstanceId;
	}

	public NodeInstanceNotFoundException(String processInstanceId, String nodeInstanceId, Throwable cause) {
		super("Node instance with id " + nodeInstanceId + " not found within process instance " + processInstanceId,
				cause);
		this.processInstanceId = processInstanceId;
		this.nodeInstanceId = nodeInstanceId;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public String getNodeInstanceId() {
		return nodeInstanceId;
	}

}
