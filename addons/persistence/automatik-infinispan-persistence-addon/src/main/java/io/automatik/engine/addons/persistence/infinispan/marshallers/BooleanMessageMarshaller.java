
package io.automatik.engine.addons.persistence.infinispan.marshallers;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class BooleanMessageMarshaller implements MessageMarshaller<Boolean> {

	@Override
	public Class<? extends Boolean> getJavaClass() {
		return Boolean.class;
	}

	@Override
	public String getTypeName() {
		return "kogito.Boolean";
	}

	@Override
	public Boolean readFrom(ProtoStreamReader reader) throws IOException {
		return reader.readBoolean("data");
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, Boolean t) throws IOException {
		writer.writeBoolean("data", t);

	}

}
