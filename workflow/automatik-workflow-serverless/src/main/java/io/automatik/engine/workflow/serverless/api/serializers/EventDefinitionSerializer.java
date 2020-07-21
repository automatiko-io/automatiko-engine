
package io.automatik.engine.workflow.serverless.api.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.automatik.engine.workflow.serverless.api.events.EventDefinition;

public class EventDefinitionSerializer extends StdSerializer<EventDefinition> {

	public EventDefinitionSerializer() {
		this(EventDefinition.class);
	}

	protected EventDefinitionSerializer(Class<EventDefinition> t) {
		super(t);
	}

	@Override
	public void serialize(EventDefinition triggerEvent, JsonGenerator gen, SerializerProvider provider)
			throws IOException {

		// serialize after setting default bean values...
		BeanSerializerFactory.instance
				.createSerializer(provider, TypeFactory.defaultInstance().constructType(EventDefinition.class))
				.serialize(triggerEvent, gen, provider);
	}
}