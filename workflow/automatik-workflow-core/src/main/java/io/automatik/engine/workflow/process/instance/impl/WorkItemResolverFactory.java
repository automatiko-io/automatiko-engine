
package io.automatik.engine.workflow.process.instance.impl;

import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.impl.ImmutableDefaultFactory;
import org.mvel2.integration.impl.SimpleValueResolver;

import io.automatik.engine.api.runtime.process.WorkItem;

public class WorkItemResolverFactory extends ImmutableDefaultFactory {

	private static final long serialVersionUID = 510l;

	private WorkItem workItem;

	public WorkItemResolverFactory(WorkItem workItem) {
		this.workItem = workItem;
	}

	public boolean isResolveable(String name) {
		return workItem.getResult(name) != null;
	}

	public VariableResolver getVariableResolver(String name) {
		return new SimpleValueResolver(workItem.getResult(name));
	}

}
