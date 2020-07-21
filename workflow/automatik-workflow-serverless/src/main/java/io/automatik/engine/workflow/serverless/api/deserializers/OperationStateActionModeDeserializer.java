
package io.automatik.engine.workflow.serverless.api.deserializers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.automatik.engine.workflow.serverless.api.WorkflowPropertySource;
import io.automatik.engine.workflow.serverless.api.states.OperationState;

public class OperationStateActionModeDeserializer extends StdDeserializer<OperationState.ActionMode> {

	private static final long serialVersionUID = 510l;
	private static Logger logger = LoggerFactory.getLogger(OperationStateActionModeDeserializer.class);

	private WorkflowPropertySource context;

	public OperationStateActionModeDeserializer() {
		this(OperationState.ActionMode.class);
	}

	public OperationStateActionModeDeserializer(WorkflowPropertySource context) {
		this(OperationState.ActionMode.class);
		this.context = context;
	}

	public OperationStateActionModeDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public OperationState.ActionMode deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

		String value = jp.getText();
		if (context != null) {
			try {
				String result = context.getPropertySource().getProperty(value);

				if (result != null) {
					return OperationState.ActionMode.fromValue(result);
				} else {
					return OperationState.ActionMode.fromValue(jp.getText());
				}
			} catch (Exception e) {
				logger.info("Exception trying to evaluate property: {}", e.getMessage());
				return OperationState.ActionMode.fromValue(jp.getText());
			}
		} else {
			return OperationState.ActionMode.fromValue(jp.getText());
		}
	}
}