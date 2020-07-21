
package io.automatik.engine.workflow.base.instance.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.automatik.engine.workflow.base.core.Context;
import io.automatik.engine.workflow.base.core.context.exception.CompensationScope;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatik.engine.workflow.base.core.context.swimlane.SwimlaneContext;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.instance.ContextInstance;
import io.automatik.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatik.engine.workflow.base.instance.ProcessInstance;
import io.automatik.engine.workflow.base.instance.context.AbstractContextInstance;
import io.automatik.engine.workflow.base.instance.context.exception.CompensationScopeInstance;
import io.automatik.engine.workflow.base.instance.context.exception.DefaultExceptionScopeInstance;
import io.automatik.engine.workflow.base.instance.context.swimlane.SwimlaneContextInstance;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;

public class ContextInstanceFactoryRegistry {

	public static final ContextInstanceFactoryRegistry INSTANCE = new ContextInstanceFactoryRegistry();

	private Map<Class<? extends Context>, ContextInstanceFactory> registry;

	public ContextInstanceFactoryRegistry() {
		this.registry = new HashMap<>();
	}

	public void register(Class<? extends Context> cls, ContextInstanceFactory factory) {
		this.registry.put(cls, factory);
	}

	public ContextInstanceFactory getContextInstanceFactory(Context context) {
		Class<? extends Context> cls = context.getClass();
		// hard wired contexts:
		if (cls == VariableScope.class)
			return factoryOf(VariableScopeInstance::new);
		if (cls == ExceptionScope.class)
			return factoryOf(DefaultExceptionScopeInstance::new);
		if (cls == CompensationScope.class)
			return factoryOf(CompensationScopeInstance::new);
		if (cls == SwimlaneContext.class)
			return factoryOf(SwimlaneContextInstance::new);

		return this.registry.get(cls);
	}

	private static ContextInstanceFactory factoryOf(Supplier<? extends ContextInstance> supplier) {
		return (context, contextInstanceContainer, processInstance) -> getContextInstance(supplier, context,
				contextInstanceContainer, processInstance);
	}

	private static ContextInstance getContextInstance(Supplier<? extends ContextInstance> supplier, Context context,
			ContextInstanceContainer contextInstanceContainer, ProcessInstance processInstance) {
		ContextInstance result = contextInstanceContainer.getContextInstance(context.getType(), context.getId());
		if (result != null) {
			return result;
		}
		AbstractContextInstance contextInstance = (AbstractContextInstance) supplier.get();
		contextInstance.setProcessInstance(processInstance);
		contextInstance.setContextId(context.getId());
		contextInstance.setContextInstanceContainer(contextInstanceContainer);
		contextInstanceContainer.addContextInstance(context.getType(), contextInstance);
		return contextInstance;
	}

}
