
package io.automatik.engine.workflow.serverless.api.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.automatik.engine.workflow.serverless.api.states.DefaultState;
import io.automatik.engine.workflow.serverless.api.states.EventState;

public class EventStateSerializer extends StdSerializer<EventState> {

	public EventStateSerializer() {
		this(EventState.class);
	}

	protected EventStateSerializer(Class<EventState> t) {
		super(t);
	}

	@Override
	public void serialize(EventState eventState, JsonGenerator gen, SerializerProvider provider) throws IOException {

		// set defaults for end state
		eventState.setType(DefaultState.Type.EVENT);

		// serialize after setting default bean values...
		BeanSerializerFactory.instance
				.createSerializer(provider, TypeFactory.defaultInstance().constructType(EventState.class))
				.serialize(eventState, gen, provider);
	}
}