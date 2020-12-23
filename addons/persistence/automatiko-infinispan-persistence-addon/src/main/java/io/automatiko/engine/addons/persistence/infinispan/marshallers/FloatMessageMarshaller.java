
package io.automatiko.engine.addons.persistence.infinispan.marshallers;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class FloatMessageMarshaller implements MessageMarshaller<Float> {

	@Override
	public Class<? extends Float> getJavaClass() {
		return Float.class;
	}

	@Override
	public String getTypeName() {
		return "kogito.Float";
	}

	@Override
	public Float readFrom(ProtoStreamReader reader) throws IOException {
		return reader.readFloat("data");
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, Float t) throws IOException {
		writer.writeFloat("data", t);

	}

}
