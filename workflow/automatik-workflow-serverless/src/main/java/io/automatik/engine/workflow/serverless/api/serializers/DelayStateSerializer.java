
package io.automatik.engine.workflow.serverless.api.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.automatik.engine.workflow.serverless.api.states.DefaultState;
import io.automatik.engine.workflow.serverless.api.states.DelayState;

public class DelayStateSerializer extends StdSerializer<DelayState> {

	public DelayStateSerializer() {
		this(DelayState.class);
	}

	protected DelayStateSerializer(Class<DelayState> t) {
		super(t);
	}

	@Override
	public void serialize(DelayState delayState, JsonGenerator gen, SerializerProvider provider) throws IOException {

		// set defaults for delay state
		delayState.setType(DefaultState.Type.DELAY);

		// serialize after setting default bean values...
		BeanSerializerFactory.instance
				.createSerializer(provider, TypeFactory.defaultInstance().constructType(DelayState.class))
				.serialize(delayState, gen, provider);
	}
}