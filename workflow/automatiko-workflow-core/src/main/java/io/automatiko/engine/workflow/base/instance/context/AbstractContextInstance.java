
package io.automatiko.engine.workflow.base.instance.context;

import java.io.Serializable;

import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.instance.ContextInstance;
import io.automatiko.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;

public abstract class AbstractContextInstance implements ContextInstance, Serializable {

	private long contextId;
	private ContextInstanceContainer contextInstanceContainer;
	private ProcessInstance processInstance;

	public long getContextId() {
		return contextId;
	}

	public void setContextId(long contextId) {
		this.contextId = contextId;
	}

	public ContextInstanceContainer getContextInstanceContainer() {
		return contextInstanceContainer;
	}

	public void setContextInstanceContainer(ContextInstanceContainer contextInstanceContainer) {
		this.contextInstanceContainer = contextInstanceContainer;
	}

	public Context getContext() {
		return getContextInstanceContainer().getContextContainer().getContext(getContextType(), getContextId());
	}

	public ProcessInstance getProcessInstance() {
		return processInstance;
	}

	public void setProcessInstance(ProcessInstance processInstance) {
		this.processInstance = processInstance;
	}

}
