package io.automatiko.engine.workflow.base.instance.impl.actions;

import java.io.Serializable;
import java.util.function.Function;

import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.workflow.base.instance.impl.Action;
import io.automatiko.engine.workflow.base.instance.impl.util.VariableUtil;
import io.automatiko.engine.workflow.base.instance.impl.workitem.DefaultWorkItemManager;
import io.automatiko.engine.workflow.base.instance.impl.workitem.WorkItemImpl;
import io.automatiko.engine.workflow.process.core.node.Transformation;

public class SignalProcessInstanceAction implements Action, Serializable {

    public static final String DEFAULT_SCOPE = "default";
    public static final String PROCESS_INSTANCE_SCOPE = "processInstance";
    public static final String EXTERNAL_SCOPE = "external";

    public static final String UNSET_SCOPE = System.getProperty("org.jbpm.signals.defaultscope",
            PROCESS_INSTANCE_SCOPE);

    private static final long serialVersionUID = 1L;

    private final String signalName;
    private String variableName;
    private Function<ProcessContext, Object> eventDataSupplier = (kcontext) -> null;

    private String scope = UNSET_SCOPE;
    private Transformation transformation;

    public SignalProcessInstanceAction(String signalName, String variableName) {
        this.signalName = signalName;
        this.variableName = variableName;
    }

    public SignalProcessInstanceAction(String signalName, String variableName, Transformation transformation) {
        this.signalName = signalName;
        this.variableName = variableName;
        this.transformation = transformation;
    }

    public SignalProcessInstanceAction(String signalName, String variableName, String scope) {
        this.signalName = signalName;
        this.variableName = variableName;
        if (scope != null) {
            this.scope = scope;
        }
    }

    public SignalProcessInstanceAction(String signalName, String variableName, String scope,
            Transformation transformation) {
        this.signalName = signalName;
        this.variableName = variableName;
        if (scope != null) {
            this.scope = scope;
        }
        this.transformation = transformation;
    }

    public SignalProcessInstanceAction(String signalName, Function<ProcessContext, Object> eventDataSupplier,
            String scope) {
        this.signalName = signalName;
        this.eventDataSupplier = eventDataSupplier;
        if (scope != null) {
            this.scope = scope;
        }
    }

    public void execute(ProcessContext context) throws Exception {
        String variableName = VariableUtil.resolveVariable(this.variableName, context.getNodeInstance());
        Object variable = variableName == null ? eventDataSupplier.apply(context) : context.getVariable(variableName);

        if (transformation != null) {
            variable = new io.automatiko.engine.workflow.base.core.event.EventTransformerImpl(transformation)
                    .transformEvent(context.getProcessInstance().getVariables());
        }
        if (DEFAULT_SCOPE.equals(scope)) {
            context.getProcessRuntime().signalEvent(VariableUtil.resolveVariable(signalName, context.getNodeInstance()),
                    variable);
        } else if (PROCESS_INSTANCE_SCOPE.equals(scope)) {
            context.getProcessInstance()
                    .signalEvent(VariableUtil.resolveVariable(signalName, context.getNodeInstance()), variable);
        } else if (EXTERNAL_SCOPE.equals(scope)) {
            WorkItemImpl workItem = new WorkItemImpl();
            workItem.setName("External Send Task");
            workItem.setNodeInstanceId(context.getNodeInstance().getId());
            workItem.setProcessInstanceId(context.getProcessInstance().getId());
            workItem.setProcessInstanceId(context.getProcessInstance().getParentProcessInstanceId());
            workItem.setNodeId(context.getNodeInstance().getNodeId());
            workItem.setParameter("Signal", VariableUtil.resolveVariable(signalName, context.getNodeInstance()));
            workItem.setParameter("SignalProcessInstanceId", context.getVariable("SignalProcessInstanceId"));
            workItem.setParameter("SignalWorkItemId", context.getVariable("SignalWorkItemId"));
            workItem.setParameter("SignalDeploymentId", context.getVariable("SignalDeploymentId"));
            if (variable == null) {
                workItem.setParameter("Data", variable);
            }
            ((DefaultWorkItemManager) context.getProcessRuntime().getWorkItemManager())
                    .internalExecuteWorkItem(workItem);
        }
    }

}
