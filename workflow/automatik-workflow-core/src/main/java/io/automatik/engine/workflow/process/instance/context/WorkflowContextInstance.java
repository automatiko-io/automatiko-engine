
package io.automatik.engine.workflow.process.instance.context;

import io.automatik.engine.workflow.base.instance.ContextInstance;
import io.automatik.engine.workflow.process.instance.NodeInstanceContainer;

public interface WorkflowContextInstance extends ContextInstance {

	NodeInstanceContainer getNodeInstanceContainer();

	void setNodeInstanceContainer(NodeInstanceContainer nodeInstanceContainer);

}
