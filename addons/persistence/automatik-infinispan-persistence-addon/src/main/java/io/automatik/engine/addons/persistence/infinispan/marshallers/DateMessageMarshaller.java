
package io.automatik.engine.addons.persistence.infinispan.marshallers;

import java.io.IOException;
import java.util.Date;

import org.infinispan.protostream.MessageMarshaller;

public class DateMessageMarshaller implements MessageMarshaller<Date> {

	@Override
	public Class<? extends Date> getJavaClass() {
		return Date.class;
	}

	@Override
	public String getTypeName() {
		return "kogito.Date";
	}

	@Override
	public Date readFrom(ProtoStreamReader reader) throws IOException {
		return new Date(reader.readLong("data"));
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, Date t) throws IOException {
		writer.writeLong("data", t.getTime());

	}

}
