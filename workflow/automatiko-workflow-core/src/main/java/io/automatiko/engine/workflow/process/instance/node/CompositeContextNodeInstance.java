
package io.automatiko.engine.workflow.process.instance.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.instance.ContextInstance;
import io.automatiko.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatiko.engine.workflow.base.instance.ContextableInstance;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.impl.ContextInstanceFactory;
import io.automatiko.engine.workflow.base.instance.impl.ContextInstanceFactoryRegistry;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;

public class CompositeContextNodeInstance extends CompositeNodeInstance
		implements ContextInstanceContainer, ContextableInstance {

	private static final long serialVersionUID = 510l;

	private Map<String, ContextInstance> contextInstances = new HashMap<String, ContextInstance>();
	private Map<String, List<ContextInstance>> subContextInstances = new HashMap<String, List<ContextInstance>>();

	protected CompositeContextNode getCompositeContextNode() {
		return (CompositeContextNode) getNode();
	}

	public ContextContainer getContextContainer() {
		return getCompositeContextNode();
	}

	public void setContextInstance(String contextId, ContextInstance contextInstance) {
		this.contextInstances.put(contextId, contextInstance);
	}

	public ContextInstance getContextInstance(String contextId) {
		ContextInstance contextInstance = this.contextInstances.get(contextId);
		if (contextInstance != null) {
			return contextInstance;
		}
		Context context = getCompositeContextNode().getDefaultContext(contextId);
		if (context != null) {
			contextInstance = getContextInstance(context);
			return contextInstance;
		}
		return null;
	}

	public List<ContextInstance> getContextInstances(String contextId) {
		return this.subContextInstances.get(contextId);
	}

	public void addContextInstance(String contextId, ContextInstance contextInstance) {
		List<ContextInstance> list = this.subContextInstances.get(contextId);
		if (list == null) {
			list = new ArrayList<ContextInstance>();
			this.subContextInstances.put(contextId, list);
		}
		list.add(contextInstance);
	}

	public void removeContextInstance(String contextId, ContextInstance contextInstance) {
		List<ContextInstance> list = this.subContextInstances.get(contextId);
		if (list != null) {
			list.remove(contextInstance);
		}
	}

	public ContextInstance getContextInstance(String contextId, long id) {
		List<ContextInstance> contextInstances = subContextInstances.get(contextId);
		if (contextInstances != null) {
			for (ContextInstance contextInstance : contextInstances) {
				if (contextInstance.getContextId() == id) {
					return contextInstance;
				}
			}
		}
		return null;
	}

	public ContextInstance getContextInstance(final Context context) {
		ContextInstanceFactory conf = ContextInstanceFactoryRegistry.INSTANCE.getContextInstanceFactory(context);
		if (conf == null) {
			throw new IllegalArgumentException("Illegal context type (registry not found): " + context.getClass());
		}
		ContextInstance contextInstance = (ContextInstance) conf.getContextInstance(context, this,
				(ProcessInstance) getProcessInstance());
		if (contextInstance == null) {
			throw new IllegalArgumentException("Illegal context type (instance not found): " + context.getClass());
		}
		return contextInstance;
	}

}
