
package io.automatik.engine.workflow.serverless.api.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.automatik.engine.workflow.serverless.api.states.DefaultState;
import io.automatik.engine.workflow.serverless.api.states.ParallelState;

public class ParallelStateSerializer extends StdSerializer<ParallelState> {

	public ParallelStateSerializer() {
		this(ParallelState.class);
	}

	protected ParallelStateSerializer(Class<ParallelState> t) {
		super(t);
	}

	@Override
	public void serialize(ParallelState parallelState, JsonGenerator gen, SerializerProvider provider)
			throws IOException {

		// set defaults for end state
		parallelState.setType(DefaultState.Type.PARALLEL);

		// serialize after setting default bean values...
		BeanSerializerFactory.instance
				.createSerializer(provider, TypeFactory.defaultInstance().constructType(ParallelState.class))
				.serialize(parallelState, gen, provider);
	}
}