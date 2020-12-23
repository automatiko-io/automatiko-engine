
package io.automatiko.engine.workflow.base.instance.impl.factory;

import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.instance.ContextInstance;
import io.automatiko.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.context.AbstractContextInstance;
import io.automatiko.engine.workflow.base.instance.impl.ContextInstanceFactory;

public class ReuseContextInstanceFactory implements ContextInstanceFactory {

	private final Class<? extends ContextInstance> cls;

	public ReuseContextInstanceFactory(Class<? extends ContextInstance> cls) {
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
			contextInstance.setProcessInstance(processInstance);
			contextInstance.setContextId(context.getId());
			contextInstance.setContextInstanceContainer(contextInstanceContainer);
			contextInstanceContainer.addContextInstance(context.getType(), contextInstance);
			return contextInstance;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to instantiate context '" + this.cls.getName() + "': " + e.getMessage(),
					e);
		}
	}

}
