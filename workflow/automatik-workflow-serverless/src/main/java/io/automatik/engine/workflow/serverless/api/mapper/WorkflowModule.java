
package io.automatik.engine.workflow.serverless.api.mapper;

import com.fasterxml.jackson.databind.module.SimpleModule;

import io.automatik.engine.workflow.serverless.api.WorkflowPropertySource;
import io.automatik.engine.workflow.serverless.api.deserializers.DataConditionOperatorDeserializer;
import io.automatik.engine.workflow.serverless.api.deserializers.DefaultStateTypeDeserializer;
import io.automatik.engine.workflow.serverless.api.deserializers.EventsActionsActionModeDeserializer;
import io.automatik.engine.workflow.serverless.api.deserializers.ExtensionDeserializer;
import io.automatik.engine.workflow.serverless.api.deserializers.OperationStateActionModeDeserializer;
import io.automatik.engine.workflow.serverless.api.deserializers.StateDeserializer;
import io.automatik.engine.workflow.serverless.api.deserializers.StringValueDeserializer;
import io.automatik.engine.workflow.serverless.api.events.EventsActions;
import io.automatik.engine.workflow.serverless.api.interfaces.Extension;
import io.automatik.engine.workflow.serverless.api.interfaces.State;
import io.automatik.engine.workflow.serverless.api.serializers.CallbackStateSerializer;
import io.automatik.engine.workflow.serverless.api.serializers.DelayStateSerializer;
import io.automatik.engine.workflow.serverless.api.serializers.EventStateSerializer;
import io.automatik.engine.workflow.serverless.api.serializers.ExtensionSerializer;
import io.automatik.engine.workflow.serverless.api.serializers.ForEachStateSerializer;
import io.automatik.engine.workflow.serverless.api.serializers.InjectStateSerializer;
import io.automatik.engine.workflow.serverless.api.serializers.OperationStateSerializer;
import io.automatik.engine.workflow.serverless.api.serializers.ParallelStateSerializer;
import io.automatik.engine.workflow.serverless.api.serializers.SubflowStateSerializer;
import io.automatik.engine.workflow.serverless.api.serializers.SwitchStateSerializer;
import io.automatik.engine.workflow.serverless.api.serializers.WorkflowSerializer;
import io.automatik.engine.workflow.serverless.api.states.DefaultState;
import io.automatik.engine.workflow.serverless.api.states.OperationState;
import io.automatik.engine.workflow.serverless.api.switchconditions.DataCondition;

public class WorkflowModule extends SimpleModule {

	private static final long serialVersionUID = 510l;

	private WorkflowPropertySource workflowPropertySource;
	private ExtensionSerializer extensionSerializer;
	private ExtensionDeserializer extensionDeserializer;

	public WorkflowModule() {
		this(null);
	}

	public WorkflowModule(WorkflowPropertySource workflowPropertySource) {
		super("workflow-module");
		this.workflowPropertySource = workflowPropertySource;
		extensionSerializer = new ExtensionSerializer();
		extensionDeserializer = new ExtensionDeserializer(workflowPropertySource);
		addDefaultSerializers();
		addDefaultDeserializers();
	}

	private void addDefaultSerializers() {
		addSerializer(new WorkflowSerializer());
		addSerializer(new EventStateSerializer());
		addSerializer(new DelayStateSerializer());
		addSerializer(new OperationStateSerializer());
		addSerializer(new ParallelStateSerializer());
		addSerializer(new SwitchStateSerializer());
		addSerializer(new SubflowStateSerializer());
		addSerializer(new InjectStateSerializer());
		addSerializer(new ForEachStateSerializer());
		addSerializer(new CallbackStateSerializer());
		addSerializer(extensionSerializer);
	}

	private void addDefaultDeserializers() {
		addDeserializer(State.class, new StateDeserializer(workflowPropertySource));
		addDeserializer(String.class, new StringValueDeserializer(workflowPropertySource));
		addDeserializer(EventsActions.ActionMode.class,
				new EventsActionsActionModeDeserializer(workflowPropertySource));
		addDeserializer(OperationState.ActionMode.class,
				new OperationStateActionModeDeserializer(workflowPropertySource));
		addDeserializer(DefaultState.Type.class, new DefaultStateTypeDeserializer(workflowPropertySource));
		addDeserializer(DataCondition.Operator.class, new DataConditionOperatorDeserializer(workflowPropertySource));
		addDeserializer(Extension.class, extensionDeserializer);
	}

	public ExtensionSerializer getExtensionSerializer() {
		return extensionSerializer;
	}

	public ExtensionDeserializer getExtensionDeserializer() {
		return extensionDeserializer;
	}
}