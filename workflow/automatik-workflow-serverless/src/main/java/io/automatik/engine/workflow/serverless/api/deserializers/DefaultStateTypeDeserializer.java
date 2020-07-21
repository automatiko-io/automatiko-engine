
package io.automatik.engine.workflow.serverless.api.deserializers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.automatik.engine.workflow.serverless.api.WorkflowPropertySource;
import io.automatik.engine.workflow.serverless.api.states.DefaultState;

public class DefaultStateTypeDeserializer extends StdDeserializer<DefaultState.Type> {

	private static final long serialVersionUID = 510l;
	private static Logger logger = LoggerFactory.getLogger(DefaultStateTypeDeserializer.class);

	private WorkflowPropertySource context;

	public DefaultStateTypeDeserializer() {
		this(DefaultState.Type.class);
	}

	public DefaultStateTypeDeserializer(WorkflowPropertySource context) {
		this(DefaultState.Type.class);
		this.context = context;
	}

	public DefaultStateTypeDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public DefaultState.Type deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

		String value = jp.getText();

		if (context != null) {
			try {
				String result = context.getPropertySource().getProperty(value);

				if (result != null) {
					return DefaultState.Type.fromValue(result);
				} else {
					return DefaultState.Type.fromValue(jp.getText());
				}
			} catch (Exception e) {
				logger.info("Exception trying to evaluate property: {}", e.getMessage());
				return DefaultState.Type.fromValue(jp.getText());
			}
		} else {
			return DefaultState.Type.fromValue(jp.getText());
		}
	}
}