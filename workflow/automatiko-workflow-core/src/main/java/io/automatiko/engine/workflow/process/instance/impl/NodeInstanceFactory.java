
package io.automatiko.engine.workflow.process.instance.impl;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.NodeInstanceContainer;
import io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance;

public interface NodeInstanceFactory {

	NodeInstance getNodeInstance(Node node, WorkflowProcessInstance processInstance,
			NodeInstanceContainer nodeInstanceContainer);

}
