
package io.automatiko.engine.addons.persistence.infinispan.marshallers;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class IntegerMessageMarshaller implements MessageMarshaller<Integer> {

	@Override
	public Class<? extends Integer> getJavaClass() {
		return Integer.class;
	}

	@Override
	public String getTypeName() {
		return "kogito.Integer";
	}

	@Override
	public Integer readFrom(ProtoStreamReader reader) throws IOException {
		return reader.readInt("data");
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, Integer t) throws IOException {
		writer.writeInt("data", t);

	}

}
