
package io.automatik.engine.workflow.process.executable.core.factory;

import io.automatik.engine.workflow.base.core.context.variable.Mappable;

public interface MappableNodeFactory {

	String METHOD_IN_MAPPING = "inMapping";
	String METHOD_OUT_MAPPING = "outMapping";

	Mappable getMappableNode();

	default MappableNodeFactory inMapping(String parameterName, String variableName) {
		getMappableNode().addInMapping(parameterName, variableName);
		return this;
	}

	default MappableNodeFactory outMapping(String parameterName, String variableName) {
		getMappableNode().addOutMapping(parameterName, variableName);
		return this;
	}
}
