
package io.automatik.engine.workflow.base.instance.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import io.automatik.engine.api.runtime.process.ProcessContext;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.process.instance.WorkflowProcessInstance;

public class JavaScriptAction implements Action, Externalizable {

	private static final long serialVersionUID = 630l;

	private String expr;

	public JavaScriptAction() {
	}

	public JavaScriptAction(final String expr) {
		this.expr = expr;
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		expr = in.readUTF();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(expr);
	}

	public void execute(ProcessContext context) throws Exception {
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("JavaScript");
		engine.put("kcontext", context);

		if (context.getProcessInstance() != null && context.getProcessInstance().getProcess() != null) {
			// insert process variables
			VariableScopeInstance variableScope = (VariableScopeInstance) ((WorkflowProcessInstance) context
					.getProcessInstance()).getContextInstance(VariableScope.VARIABLE_SCOPE);

			Map<String, Object> variables = variableScope.getVariables();
			if (variables != null) {
				for (Entry<String, Object> variable : variables.entrySet()) {
					engine.put(variable.getKey(), variable.getValue());
				}
			}
		}
		engine.eval(expr);
	}

}
