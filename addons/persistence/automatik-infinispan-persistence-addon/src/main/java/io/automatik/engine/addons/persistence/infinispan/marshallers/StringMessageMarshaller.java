
package io.automatik.engine.addons.persistence.infinispan.marshallers;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class StringMessageMarshaller implements MessageMarshaller<String> {

	@Override
	public Class<? extends String> getJavaClass() {
		return String.class;
	}

	@Override
	public String getTypeName() {
		return "kogito.String";
	}

	@Override
	public String readFrom(ProtoStreamReader reader) throws IOException {
		return reader.readString("data");
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, String t) throws IOException {
		writer.writeString("data", t);

	}

}
