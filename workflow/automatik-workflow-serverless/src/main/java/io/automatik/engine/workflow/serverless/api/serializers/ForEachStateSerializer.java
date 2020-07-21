
package io.automatik.engine.workflow.serverless.api.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.automatik.engine.workflow.serverless.api.states.DefaultState;
import io.automatik.engine.workflow.serverless.api.states.ForEachState;

public class ForEachStateSerializer extends StdSerializer<ForEachState> {

	public ForEachStateSerializer() {
		this(ForEachState.class);
	}

	protected ForEachStateSerializer(Class<ForEachState> t) {
		super(t);
	}

	@Override
	public void serialize(ForEachState forEachState, JsonGenerator gen, SerializerProvider provider)
			throws IOException {

		// set defaults for foreach state
		forEachState.setType(DefaultState.Type.FOREACH);

		// serialize after setting default bean values...
		BeanSerializerFactory.instance
				.createSerializer(provider, TypeFactory.defaultInstance().constructType(ForEachState.class))
				.serialize(forEachState, gen, provider);
	}
}