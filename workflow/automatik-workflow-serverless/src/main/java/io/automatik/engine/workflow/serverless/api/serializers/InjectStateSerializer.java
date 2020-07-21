
package io.automatik.engine.workflow.serverless.api.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.automatik.engine.workflow.serverless.api.states.DefaultState;
import io.automatik.engine.workflow.serverless.api.states.InjectState;

public class InjectStateSerializer extends StdSerializer<InjectState> {

	public InjectStateSerializer() {
		this(InjectState.class);
	}

	protected InjectStateSerializer(Class<InjectState> t) {
		super(t);
	}

	@Override
	public void serialize(InjectState relayState, JsonGenerator gen, SerializerProvider provider) throws IOException {

		// set defaults for relay state
		relayState.setType(DefaultState.Type.INJECT);

		// serialize after setting default bean values...
		BeanSerializerFactory.instance
				.createSerializer(provider, TypeFactory.defaultInstance().constructType(InjectState.class))
				.serialize(relayState, gen, provider);
	}
}