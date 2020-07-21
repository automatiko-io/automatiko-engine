
package io.automatik.engine.workflow.base.core;

import java.util.Map;
import java.util.Set;

public interface Work {

	void setName(String name);

	String getName();

	void setParameter(String name, Object value);

	void setParameters(Map<String, Object> parameters);

	Object getParameter(String name);

	Map<String, Object> getParameters();

	void addParameterDefinition(ParameterDefinition parameterDefinition);

	void setParameterDefinitions(Set<ParameterDefinition> parameterDefinitions);

	Set<ParameterDefinition> getParameterDefinitions();

	String[] getParameterNames();

	ParameterDefinition getParameterDefinition(String name);

}
