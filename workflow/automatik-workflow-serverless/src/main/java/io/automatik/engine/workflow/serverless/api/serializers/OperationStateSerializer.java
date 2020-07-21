
package io.automatik.engine.workflow.serverless.api.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.automatik.engine.workflow.serverless.api.states.DefaultState;
import io.automatik.engine.workflow.serverless.api.states.OperationState;

public class OperationStateSerializer extends StdSerializer<OperationState> {

	public OperationStateSerializer() {
		this(OperationState.class);
	}

	protected OperationStateSerializer(Class<OperationState> t) {
		super(t);
	}

	@Override
	public void serialize(OperationState operationState, JsonGenerator gen, SerializerProvider provider)
			throws IOException {

		// set defaults for delay state
		operationState.setType(DefaultState.Type.OPERATION);

		// serialize after setting default bean values...
		BeanSerializerFactory.instance
				.createSerializer(provider, TypeFactory.defaultInstance().constructType(OperationState.class))
				.serialize(operationState, gen, provider);
	}
}