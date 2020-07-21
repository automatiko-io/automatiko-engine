
package io.automatik.engine.workflow.base.instance.context.exclusive;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.workflow.base.core.context.exclusive.ExclusiveGroup;
import io.automatik.engine.workflow.base.instance.context.AbstractContextInstance;

public class ExclusiveGroupInstance extends AbstractContextInstance {

	private static final long serialVersionUID = 510l;

	private Map<String, NodeInstance> nodeInstances = new HashMap<String, NodeInstance>();

	public String getContextType() {
		return ExclusiveGroup.EXCLUSIVE_GROUP;
	}

	public boolean containsNodeInstance(NodeInstance nodeInstance) {
		return nodeInstances.containsKey(nodeInstance.getId());
	}

	public void addNodeInstance(NodeInstance nodeInstance) {
		nodeInstances.put(nodeInstance.getId(), nodeInstance);
	}

	public Collection<NodeInstance> getNodeInstances() {
		return nodeInstances.values();
	}

}
