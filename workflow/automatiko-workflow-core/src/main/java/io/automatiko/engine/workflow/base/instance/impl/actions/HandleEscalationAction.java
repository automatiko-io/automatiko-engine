package io.automatiko.engine.workflow.base.instance.impl.actions;

import static io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_ABORTED;

import java.io.Serializable;

import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatiko.engine.workflow.base.core.event.EventTransformerImpl;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.context.exception.ExceptionScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.Action;
import io.automatiko.engine.workflow.process.instance.NodeInstance;

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
            io.automatiko.engine.workflow.process.core.node.Transformation transformation = (io.automatiko.engine.workflow.process.core.node.Transformation) context
                    .getNodeInstance().getNode().getMetaData().get("Transformation");
            if (transformation != null) {
                tVariable = new EventTransformerImpl(transformation)
                        .transformEvent(context.getProcessInstance().getVariables());
            }
            scopeInstance.handleException(context.getNodeInstance(), faultName, tVariable);
        } else {

            ((ProcessInstance) context.getProcessInstance()).setState(STATE_ABORTED);
        }
    }

}
