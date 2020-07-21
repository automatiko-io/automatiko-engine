
package io.automatik.engine.workflow.serverless.api.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.automatik.engine.workflow.serverless.api.states.CallbackState;
import io.automatik.engine.workflow.serverless.api.states.DefaultState;

public class CallbackStateSerializer extends StdSerializer<CallbackState> {

	public CallbackStateSerializer() {
		this(CallbackState.class);
	}

	protected CallbackStateSerializer(Class<CallbackState> t) {
		super(t);
	}

	@Override
	public void serialize(CallbackState callbackState, JsonGenerator gen, SerializerProvider provider)
			throws IOException {

		// set defaults for callback state
		callbackState.setType(DefaultState.Type.CALLBACK);

		// serialize after setting default bean values...
		BeanSerializerFactory.instance
				.createSerializer(provider, TypeFactory.defaultInstance().constructType(CallbackState.class))
				.serialize(callbackState, gen, provider);
	}
}