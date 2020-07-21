
package io.automatik.engine.addons.persistence.infinispan.marshallers;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class DoubleMessageMarshaller implements MessageMarshaller<Double> {

	@Override
	public Class<? extends Double> getJavaClass() {
		return Double.class;
	}

	@Override
	public String getTypeName() {
		return "kogito.Double";
	}

	@Override
	public Double readFrom(ProtoStreamReader reader) throws IOException {
		return reader.readDouble("data");
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, Double t) throws IOException {
		writer.writeDouble("data", t);

	}

}
