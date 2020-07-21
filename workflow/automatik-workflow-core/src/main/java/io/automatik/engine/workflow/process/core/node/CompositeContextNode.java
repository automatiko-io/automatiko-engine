
package io.automatik.engine.workflow.process.core.node;

import java.util.List;

import io.automatik.engine.workflow.base.core.Context;
import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.context.AbstractContext;
import io.automatik.engine.workflow.base.core.impl.ContextContainerImpl;

public class CompositeContextNode extends CompositeNode implements ContextContainer {

	private static final long serialVersionUID = 510l;

	private ContextContainer contextContainer = new ContextContainerImpl();

	public List<Context> getContexts(String contextType) {
		return this.contextContainer.getContexts(contextType);
	}

	public void addContext(Context context) {
		this.contextContainer.addContext(context);
		((AbstractContext) context).setContextContainer(this);
	}

	public Context getContext(String contextType, long id) {
		return this.contextContainer.getContext(contextType, id);
	}

	public void setDefaultContext(Context context) {
		this.contextContainer.setDefaultContext(context);
		((AbstractContext) context).setContextContainer(this);
	}

	public Context getDefaultContext(String contextType) {
		return this.contextContainer.getDefaultContext(contextType);
	}

	public Context resolveContext(String contextId, Object param) {
		Context context = getDefaultContext(contextId);
		if (context != null) {
			context = context.resolveContext(param);
			if (context != null) {
				return context;
			}
		}
		return super.resolveContext(contextId, param);
	}

}
