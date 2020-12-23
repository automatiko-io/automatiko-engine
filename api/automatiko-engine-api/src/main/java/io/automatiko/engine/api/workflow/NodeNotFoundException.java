
package io.automatiko.engine.api.workflow;

public class NodeNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 8031225233775014572L;

	private final String processInstanceId;
	private final String nodeId;

	public NodeNotFoundException(String processInstanceId, String nodeId) {
		super("Node with id " + nodeId + " not found within process instance " + processInstanceId);
		this.processInstanceId = processInstanceId;
		this.nodeId = nodeId;
	}

	public NodeNotFoundException(String processInstanceId, String nodeId, Throwable cause) {
		super("Node with id " + nodeId + " not found within process instance " + processInstanceId, cause);
		this.processInstanceId = processInstanceId;
		this.nodeId = nodeId;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public String getNodeId() {
		return nodeId;
	}

}
