package io.automatiko.engine.workflow.base.instance.impl.actions;

import java.io.Serializable;

import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.workflow.base.core.event.EventTransformerImpl;
import io.automatiko.engine.workflow.base.instance.impl.Action;
import io.automatiko.engine.workflow.base.instance.impl.util.VariableUtil;
import io.automatiko.engine.workflow.base.instance.impl.workitem.DefaultWorkItemManager;
import io.automatiko.engine.workflow.base.instance.impl.workitem.WorkItemImpl;
import io.automatiko.engine.workflow.process.core.node.Transformation;

public class HandleMessageAction implements Action, Serializable {

	private static final long serialVersionUID = 1L;

	private final String messageType;
	private String variableName;

	private Transformation transformation;

	public HandleMessageAction(String messageType, String variableName) {
		this.messageType = messageType;
		this.variableName = variableName;
	}

	public HandleMessageAction(String messageType, String variableName, Transformation transformation) {
		this.messageType = messageType;
		this.variableName = variableName;
		this.transformation = transformation;
	}

	public void execute(ProcessContext context) throws Exception {
		Object variable = VariableUtil.resolveVariable(variableName, context.getNodeInstance());

		if (transformation != null) {
			variable = new EventTransformerImpl(transformation).transformEvent(variable);
		}

		WorkItemImpl workItem = new WorkItemImpl();
		workItem.setName("Send Task");
		workItem.setNodeInstanceId(context.getNodeInstance().getId());
		workItem.setProcessInstanceId(context.getProcessInstance().getId());
		workItem.setNodeId(context.getNodeInstance().getNodeId());
		workItem.setParameter("MessageType", messageType);
		if (variable != null) {
			workItem.setParameter("Message", variable);
		}

		((DefaultWorkItemManager) context.getProcessRuntime().getWorkItemManager()).internalExecuteWorkItem(workItem);
	}

}
