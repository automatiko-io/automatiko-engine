package io.automatik.engine.workflow.base.instance.impl.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.mvel2.MVEL;

import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatik.engine.workflow.util.PatternConstants;

public class VariableUtil {
	public static String resolveVariable(String s, NodeInstance nodeInstance) {
		if (s == null) {
			return null;
		}

		Map<String, String> replacements = new HashMap<String, String>();
		Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(s);
		while (matcher.find()) {
			String paramName = matcher.group(1);
			if (replacements.get(paramName) == null) {
				VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((io.automatik.engine.workflow.process.instance.NodeInstance) nodeInstance)
						.resolveContextInstance(VariableScope.VARIABLE_SCOPE, paramName);
				if (variableScopeInstance != null) {
					Object variableValue = variableScopeInstance.getVariable(paramName);
					String variableValueString = variableValue == null ? "" : variableValue.toString();
					replacements.put(paramName, variableValueString);
				} else {
					try {
						Object variableValue = MVEL.eval(paramName, new NodeInstanceResolverFactory(
								(io.automatik.engine.workflow.process.instance.NodeInstance) nodeInstance));
						String variableValueString = variableValue == null ? "" : variableValue.toString();
						replacements.put(paramName, variableValueString);
					} catch (Throwable t) {

					}
				}
			}
		}
		for (Map.Entry<String, String> replacement : replacements.entrySet()) {
			s = s.replace("#{" + replacement.getKey() + "}", replacement.getValue());
		}

		return s;
	}
}
