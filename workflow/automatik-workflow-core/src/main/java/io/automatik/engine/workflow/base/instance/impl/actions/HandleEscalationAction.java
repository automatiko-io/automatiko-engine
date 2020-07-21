package io.automatik.engine.workflow.base.instance.impl.actions;

import static io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ABORTED;

import java.io.Serializable;

import io.automatik.engine.api.runtime.process.ProcessContext;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatik.engine.workflow.base.core.event.EventTransformerImpl;
import io.automatik.engine.workflow.base.instance.ProcessInstance;
import io.automatik.engine.workflow.base.instance.context.exception.ExceptionScopeInstance;
import io.automatik.engine.workflow.base.instance.impl.Action;
import io.automatik.engine.workflow.process.instance.NodeInstance;

public class HandleEscalationAction implements Action, Serializable {

	private static final long serialVersionUID = 1L;

	private String faultName;
	private String variableName;

	public HandleEscalationAction(String faultName, String variableName) {
		this.faultName = faultName;
		this.variableName = variableName;
	}

	public void execute(ProcessContext context) throws Exception {
		ExceptionScopeInstance scopeInstance = (ExceptionScopeInstance) ((NodeInstance) context.getNodeInstance())
				.resolveContextInstance(ExceptionScope.EXCEPTION_SCOPE, faultName);
		if (scopeInstance != null) {

			Object tVariable = variableName == null ? null : context.getVariable(variableName);
			io.automatik.engine.workflow.process.core.node.Transformation transformation = (io.automatik.engine.workflow.process.core.node.Transformation) context
					.getNodeInstance().getNode().getMetaData().get("Transformation");
			if (transformation != null) {
				tVariable = new EventTransformerImpl(transformation)
						.transformEvent(context.getProcessInstance().getVariables());
			}
			scopeInstance.handleException(faultName, tVariable);
		} else {

			((ProcessInstance) context.getProcessInstance()).setState(STATE_ABORTED);
		}
	}

}
