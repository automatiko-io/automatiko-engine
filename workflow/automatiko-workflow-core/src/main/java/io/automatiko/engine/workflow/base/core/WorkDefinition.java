
package io.automatiko.engine.workflow.base.core;

import java.util.Set;

public interface WorkDefinition {

	String getName();

	Set<ParameterDefinition> getParameters();

	String[] getParameterNames();

	ParameterDefinition getParameter(String name);

	Set<ParameterDefinition> getResults();

	String[] getResultNames();

	ParameterDefinition getResult(String name);

}
