
package io.automatik.engine.workflow.serverless.api.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.automatik.engine.workflow.serverless.api.states.DefaultState;
import io.automatik.engine.workflow.serverless.api.states.SwitchState;

public class SwitchStateSerializer extends StdSerializer<SwitchState> {

	public SwitchStateSerializer() {
		this(SwitchState.class);
	}

	protected SwitchStateSerializer(Class<SwitchState> t) {
		super(t);
	}

	@Override
	public void serialize(SwitchState switchState, JsonGenerator gen, SerializerProvider provider) throws IOException {

		// set defaults for end state
		switchState.setType(DefaultState.Type.SWITCH);

		// serialize after setting default bean values...
		BeanSerializerFactory.instance
				.createSerializer(provider, TypeFactory.defaultInstance().constructType(SwitchState.class))
				.serialize(switchState, gen, provider);
	}
}