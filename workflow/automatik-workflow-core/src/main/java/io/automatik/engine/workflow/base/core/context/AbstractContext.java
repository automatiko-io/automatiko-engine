
package io.automatik.engine.workflow.base.core.context;

import io.automatik.engine.workflow.base.core.Context;
import io.automatik.engine.workflow.base.core.ContextContainer;

public abstract class AbstractContext implements Context {

	private long id;
	private ContextContainer contextContainer;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public ContextContainer getContextContainer() {
		return contextContainer;
	}

	public void setContextContainer(ContextContainer contextContainer) {
		this.contextContainer = contextContainer;
	}

}
