
/**
 * 
 */
package io.automatik.engine.workflow.workflow.instance.node;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.NodeInstanceContainer;
import io.automatik.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceFactory;

public class MockNodeInstanceFactory implements NodeInstanceFactory {

	private MockNodeInstance instance;

	public MockNodeInstanceFactory(MockNodeInstance instance) {
		this.instance = instance;
	}

	public MockNodeInstance getMockNodeInstance() {
		return this.instance;
	}

	public NodeInstance getNodeInstance(Node node, WorkflowProcessInstance processInstance,
			NodeInstanceContainer nodeInstanceContainer) {
		instance.setProcessInstance(processInstance);
		instance.setNodeInstanceContainer(nodeInstanceContainer);
		return instance;
	}

}
