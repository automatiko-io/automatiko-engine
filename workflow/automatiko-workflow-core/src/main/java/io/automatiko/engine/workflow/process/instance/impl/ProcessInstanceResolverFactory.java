
package io.automatiko.engine.workflow.process.instance.impl;

import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.impl.ImmutableDefaultFactory;
import org.mvel2.integration.impl.SimpleValueResolver;

import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;

public class ProcessInstanceResolverFactory extends ImmutableDefaultFactory {

	private static final long serialVersionUID = 510l;

	private WorkflowProcessInstance processInstance;

	public ProcessInstanceResolverFactory(WorkflowProcessInstance processInstance) {
		this.processInstance = processInstance;
	}

	public boolean isResolveable(String name) {
		return processInstance.getVariable(name) != null;
	}

	public VariableResolver getVariableResolver(String name) {
		return new SimpleValueResolver(processInstance.getVariable(name));
	}

}
