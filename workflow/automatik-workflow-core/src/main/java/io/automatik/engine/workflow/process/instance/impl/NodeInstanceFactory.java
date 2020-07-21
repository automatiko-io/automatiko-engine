
package io.automatik.engine.workflow.process.instance.impl;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.NodeInstanceContainer;
import io.automatik.engine.workflow.process.instance.WorkflowProcessInstance;

public interface NodeInstanceFactory {

	NodeInstance getNodeInstance(Node node, WorkflowProcessInstance processInstance,
			NodeInstanceContainer nodeInstanceContainer);

}
