package io.automatik.engine.workflow.base.instance.impl.actions;

import java.io.Serializable;
import java.util.Collection;

import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.ProcessContext;
import io.automatik.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatik.engine.workflow.base.instance.impl.Action;
import io.automatik.engine.workflow.process.instance.node.CompositeNodeInstance;

public class CancelNodeInstanceAction implements Action, Serializable {

	private static final long serialVersionUID = 1L;

	private String attachedToNodeId;

	public CancelNodeInstanceAction(String attachedToNodeId) {
		super();
		this.attachedToNodeId = attachedToNodeId;
	}

	public void execute(ProcessContext context) throws Exception {
		WorkflowProcessInstance pi = context.getNodeInstance().getProcessInstance();
		NodeInstance nodeInstance = findNodeByUniqueId(pi.getNodeInstances(), attachedToNodeId);
		if (nodeInstance != null) {
			((io.automatik.engine.workflow.process.instance.NodeInstance) nodeInstance).cancel();
		}
	}

	private NodeInstance findNodeByUniqueId(Collection<NodeInstance> nodeInstances, String uniqueId) {

		if (nodeInstances != null && !nodeInstances.isEmpty()) {
			for (NodeInstance nInstance : nodeInstances) {
				String nodeUniqueId = (String) nInstance.getNode().getMetaData().get("UniqueId");
				if (uniqueId.equals(nodeUniqueId)) {
					return nInstance;
				}
				if (nInstance instanceof CompositeNodeInstance) {
					NodeInstance nodeInstance = findNodeByUniqueId(
							((CompositeNodeInstance) nInstance).getNodeInstances(), uniqueId);
					if (nodeInstance != null) {
						return nodeInstance;
					}
				}
			}
		}
		return null;
	}

}
