
package io.automatik.engine.workflow.serverless.api.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.automatik.engine.workflow.serverless.api.states.DefaultState;
import io.automatik.engine.workflow.serverless.api.states.SubflowState;

public class SubflowStateSerializer extends StdSerializer<SubflowState> {

	public SubflowStateSerializer() {
		this(SubflowState.class);
	}

	protected SubflowStateSerializer(Class<SubflowState> t) {
		super(t);
	}

	@Override
	public void serialize(SubflowState subflowState, JsonGenerator gen, SerializerProvider provider)
			throws IOException {

		// set defaults for end state
		subflowState.setType(DefaultState.Type.SUBFLOW);

		// serialize after setting default bean values...
		BeanSerializerFactory.instance
				.createSerializer(provider, TypeFactory.defaultInstance().constructType(SubflowState.class))
				.serialize(subflowState, gen, provider);
	}
}