
package io.automatik.engine.workflow.process.instance.node;

import java.util.Date;
import java.util.List;

import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.workflow.base.core.context.ProcessContext;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.base.instance.impl.Action;
import io.automatik.engine.workflow.process.core.node.ActionNode;
import io.automatik.engine.workflow.process.core.node.DataAssociation;
import io.automatik.engine.workflow.process.instance.WorkflowRuntimeException;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceImpl;

/**
 * Runtime counterpart of an action node.
 * 
 */
public class ActionNodeInstance extends NodeInstanceImpl {

	private static final long serialVersionUID = 510l;

	protected ActionNode getActionNode() {
		return (ActionNode) getNode();
	}

	public void internalTrigger(final NodeInstance from, String type) {
		triggerTime = new Date();
		if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
			throw new IllegalArgumentException("An ActionNode only accepts default incoming connections!");
		}
		Action action = (Action) getActionNode().getAction().getMetaData("Action");
		try {
			ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime());
			context.setNodeInstance(this);
			executeAction(action);
		} catch (WorkflowRuntimeException wre) {
			throw wre;
		} catch (Exception e) {
			// for the case that one of the following throws an exception
			// - the ProcessContext() constructor
			// - or context.setNodeInstance(this)
			throw new WorkflowRuntimeException(this, getProcessInstance(),
					"Unable to execute Action: " + e.getMessage(), e);
		}
		triggerCompleted();
	}

	public void setOutputVariable(Object variable) {
		List<DataAssociation> outputs = getActionNode().getOutAssociations();
		if (outputs != null && !outputs.isEmpty()) {

			for (DataAssociation output : outputs) {

				VariableScopeInstance variableScopeInstance = (VariableScopeInstance) getProcessInstance()
						.getContextInstance(VariableScope.VARIABLE_SCOPE);
				if (variableScopeInstance != null) {

					Variable var = variableScopeInstance.getVariableScope().getVariables().stream()
							.filter(v -> v.getId().equals(output.getTarget())).findFirst().orElse(null);
					if (var != null) {
						variableScopeInstance.setVariable(var.getName(), variable);
					}
				}
			}
		}
	}

	public void triggerCompleted() {
		triggerCompleted(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, true);
	}

}
