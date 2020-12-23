
package io.automatiko.engine.workflow.process.instance.context;

import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.instance.ContextInstance;
import io.automatiko.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.context.AbstractContextInstance;
import io.automatiko.engine.workflow.base.instance.impl.ContextInstanceFactory;
import io.automatiko.engine.workflow.process.instance.NodeInstanceContainer;

public class WorkflowReuseContextInstanceFactory implements ContextInstanceFactory {

	public final Class<? extends ContextInstance> cls;

	public WorkflowReuseContextInstanceFactory(Class<? extends ContextInstance> cls) {
		this.cls = cls;
	}

	public ContextInstance getContextInstance(Context context, ContextInstanceContainer contextInstanceContainer,
			ProcessInstance processInstance) {
		ContextInstance result = contextInstanceContainer.getContextInstance(context.getType(), context.getId());
		if (result != null) {
			return result;
		}
		try {
			AbstractContextInstance contextInstance = (AbstractContextInstance) cls.newInstance();
			contextInstance.setContextId(context.getId());
			contextInstance.setContextInstanceContainer(contextInstanceContainer);
			contextInstance.setProcessInstance(processInstance);
			contextInstanceContainer.addContextInstance(context.getType(), contextInstance);
			NodeInstanceContainer nodeInstanceContainer = null;
			if (contextInstanceContainer instanceof NodeInstanceContainer) {
				nodeInstanceContainer = (NodeInstanceContainer) contextInstanceContainer;
			} else if (contextInstanceContainer instanceof ContextInstance) {
				ContextInstanceContainer parent = ((ContextInstance) contextInstanceContainer)
						.getContextInstanceContainer();
				while (parent != null) {
					if (parent instanceof NodeInstanceContainer) {
						nodeInstanceContainer = (NodeInstanceContainer) parent;
					} else if (contextInstanceContainer instanceof ContextInstance) {
						parent = ((ContextInstance) contextInstanceContainer).getContextInstanceContainer();
					} else {
						parent = null;
					}
				}
			}
			((WorkflowContextInstance) contextInstance).setNodeInstanceContainer(nodeInstanceContainer);
			return contextInstance;
		} catch (Exception e) {
			throw new RuntimeException("Unable to instantiate context '" + this.cls.getName() + "': " + e.getMessage());
		}
	}

}
