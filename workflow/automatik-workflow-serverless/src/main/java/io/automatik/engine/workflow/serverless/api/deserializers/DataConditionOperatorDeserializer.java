
package io.automatik.engine.workflow.serverless.api.deserializers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.automatik.engine.workflow.serverless.api.WorkflowPropertySource;
import io.automatik.engine.workflow.serverless.api.switchconditions.DataCondition;

public class DataConditionOperatorDeserializer extends StdDeserializer<DataCondition.Operator> {

	private static final long serialVersionUID = 510l;
	private static Logger logger = LoggerFactory.getLogger(DataConditionOperatorDeserializer.class);

	private WorkflowPropertySource context;

	public DataConditionOperatorDeserializer() {
		this(DataCondition.Operator.class);
	}

	public DataConditionOperatorDeserializer(WorkflowPropertySource context) {
		this(DataCondition.Operator.class);
		this.context = context;
	}

	public DataConditionOperatorDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public DataCondition.Operator deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

		String value = jp.getText();
		if (context != null) {
			try {
				String result = context.getPropertySource().getProperty(value);

				if (result != null) {
					return DataCondition.Operator.fromValue(result);
				} else {
					return DataCondition.Operator.fromValue(jp.getText());
				}
			} catch (Exception e) {
				logger.info("Exception trying to evaluate property: {}", e.getMessage());
				return DataCondition.Operator.fromValue(jp.getText());
			}
		} else {
			return DataCondition.Operator.fromValue(jp.getText());
		}
	}
}