
package io.automatik.engine.addons.persistence.infinispan.marshallers;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class LongMessageMarshaller implements MessageMarshaller<Long> {

	@Override
	public Class<? extends Long> getJavaClass() {
		return Long.class;
	}

	@Override
	public String getTypeName() {
		return "kogito.Long";
	}

	@Override
	public Long readFrom(ProtoStreamReader reader) throws IOException {
		return reader.readLong("data");
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, Long t) throws IOException {
		writer.writeLong("data", t);

	}

}
