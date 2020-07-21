
package io.automatik.engine.workflow.serverless.api.deserializers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.automatik.engine.workflow.serverless.api.WorkflowPropertySource;
import io.automatik.engine.workflow.serverless.api.interfaces.State;
import io.automatik.engine.workflow.serverless.api.states.CallbackState;
import io.automatik.engine.workflow.serverless.api.states.DefaultState;
import io.automatik.engine.workflow.serverless.api.states.DelayState;
import io.automatik.engine.workflow.serverless.api.states.EventState;
import io.automatik.engine.workflow.serverless.api.states.ForEachState;
import io.automatik.engine.workflow.serverless.api.states.InjectState;
import io.automatik.engine.workflow.serverless.api.states.OperationState;
import io.automatik.engine.workflow.serverless.api.states.ParallelState;
import io.automatik.engine.workflow.serverless.api.states.SubflowState;
import io.automatik.engine.workflow.serverless.api.states.SwitchState;

public class StateDeserializer extends StdDeserializer<State> {

	private static final long serialVersionUID = 510l;
	private static Logger logger = LoggerFactory.getLogger(StateDeserializer.class);

	private WorkflowPropertySource context;

	public StateDeserializer() {
		this(State.class);
	}

	public StateDeserializer(Class<?> vc) {
		super(vc);
	}

	public StateDeserializer(WorkflowPropertySource context) {
		this(State.class);
		this.context = context;
	}

	@Override
	public State deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

		ObjectMapper mapper = (ObjectMapper) jp.getCodec();
		JsonNode node = jp.getCodec().readTree(jp);
		String typeValue = node.get("type").asText();

		if (context != null) {
			try {
				String result = context.getPropertySource().getProperty(typeValue);

				if (result != null) {
					typeValue = result;
				}
			} catch (Exception e) {
				logger.info("Exception trying to evaluate property: {}", e.getMessage());
			}
		}

		// based on statetype return the specific state impl
		DefaultState.Type type = DefaultState.Type.fromValue(typeValue);
		switch (type) {
		case EVENT:
			return mapper.treeToValue(node, EventState.class);
		case OPERATION:
			return mapper.treeToValue(node, OperationState.class);
		case SWITCH:
			return mapper.treeToValue(node, SwitchState.class);
		case DELAY:
			return mapper.treeToValue(node, DelayState.class);
		case PARALLEL:
			return mapper.treeToValue(node, ParallelState.class);

		case SUBFLOW:
			return mapper.treeToValue(node, SubflowState.class);

		case INJECT:
			return mapper.treeToValue(node, InjectState.class);

		case FOREACH:
			return mapper.treeToValue(node, ForEachState.class);

		case CALLBACK:
			return mapper.treeToValue(node, CallbackState.class);
		default:
			return mapper.treeToValue(node, DefaultState.class);
		}
	}
}