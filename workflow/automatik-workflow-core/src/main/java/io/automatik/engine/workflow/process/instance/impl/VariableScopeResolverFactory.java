
package io.automatik.engine.workflow.process.instance.impl;

import java.util.HashMap;
import java.util.Map;

import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.impl.ImmutableDefaultFactory;
import org.mvel2.integration.impl.SimpleValueResolver;

import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;

public class VariableScopeResolverFactory extends ImmutableDefaultFactory {

	private static final long serialVersionUID = 510l;

	private VariableScopeInstance variableScope;
	private Map<String, Object> extraParameters = new HashMap<String, Object>();

	public VariableScopeResolverFactory(VariableScopeInstance variableScope) {
		this.variableScope = variableScope;
	}

	public boolean isResolveable(String name) {
		boolean found = variableScope.getVariable(name) != null;
		if (!found) {
			return extraParameters.containsKey(name);
		}

		return found;
	}

	public VariableResolver getVariableResolver(String name) {
		if (extraParameters.containsKey(name)) {
			return new SimpleValueResolver(extraParameters.get(name));
		}

		Object value = variableScope.getVariable(name);
		return new SimpleValueResolver(value);
	}

	public void addExtraParameters(Map<String, Object> parameters) {
		this.extraParameters.putAll(parameters);
	}
}
