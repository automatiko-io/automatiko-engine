
package io.automatiko.engine.workflow.process.instance.context;

import io.automatiko.engine.workflow.base.instance.ContextInstance;
import io.automatiko.engine.workflow.process.instance.NodeInstanceContainer;

public interface WorkflowContextInstance extends ContextInstance {

	NodeInstanceContainer getNodeInstanceContainer();

	void setNodeInstanceContainer(NodeInstanceContainer nodeInstanceContainer);

}
