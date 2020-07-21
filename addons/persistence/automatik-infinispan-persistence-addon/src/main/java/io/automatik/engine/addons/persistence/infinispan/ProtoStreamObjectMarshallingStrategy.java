package io.automatik.engine.addons.persistence.infinispan;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.SerializationContextImpl;

import io.automatik.engine.addons.persistence.infinispan.marshallers.BooleanMessageMarshaller;
import io.automatik.engine.addons.persistence.infinispan.marshallers.DateMessageMarshaller;
import io.automatik.engine.addons.persistence.infinispan.marshallers.DoubleMessageMarshaller;
import io.automatik.engine.addons.persistence.infinispan.marshallers.FloatMessageMarshaller;
import io.automatik.engine.addons.persistence.infinispan.marshallers.IntegerMessageMarshaller;
import io.automatik.engine.addons.persistence.infinispan.marshallers.LongMessageMarshaller;
import io.automatik.engine.addons.persistence.infinispan.marshallers.StringMessageMarshaller;
import io.automatik.engine.api.marshalling.ObjectMarshallingStrategy;

public class ProtoStreamObjectMarshallingStrategy implements ObjectMarshallingStrategy {

	private SerializationContext serializationContext;
	private Map<String, Class<?>> typeToClassMapping = new ConcurrentHashMap<>();

	public ProtoStreamObjectMarshallingStrategy(String proto, MessageMarshaller<?>... marshallers) {
		serializationContext = new SerializationContextImpl(Configuration.builder().build());

		try {
			serializationContext.registerProtoFiles(FileDescriptorSource.fromResources("automatik-types.proto"));
			registerMarshaller(new StringMessageMarshaller(), new IntegerMessageMarshaller(),
					new LongMessageMarshaller(), new DoubleMessageMarshaller(), new FloatMessageMarshaller(),
					new BooleanMessageMarshaller(), new DateMessageMarshaller());

			if (proto != null) {
				serializationContext
						.registerProtoFiles(FileDescriptorSource.fromString(UUID.randomUUID().toString(), proto));

				registerMarshaller(marshallers);

			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public boolean accept(Object object) {
		if (object == null) {
			return false;
		}
		return serializationContext.canMarshall(object.getClass());
	}

	@Override
	public byte[] marshal(Context context, ObjectOutputStream os, Object object) throws IOException {
		return ProtobufUtil.toByteArray(serializationContext, object);

	}

	@Override
	public Object unmarshal(String dataType, Context context, ObjectInputStream is, byte[] object,
			ClassLoader classloader) throws IOException, ClassNotFoundException {

		return ProtobufUtil.fromByteArray(serializationContext, object,
				serializationContext.getMarshaller(dataType).getJavaClass());
	}

	@Override
	public String getType(Class<?> clazz) {
		BaseMarshaller<?> marshaller = serializationContext.getMarshaller(clazz);
		if (marshaller == null) {
			throw new IllegalStateException("No marshaller found for class " + clazz.getCanonicalName());
		}
		return marshaller.getTypeName();
	}

	public void registerMarshaller(MessageMarshaller<?>... marshallers) {
		for (MessageMarshaller<?> marshaller : marshallers) {
			serializationContext.registerMarshaller(marshaller);

			typeToClassMapping.putIfAbsent(marshaller.getTypeName(), marshaller.getJavaClass());
		}
	}

	@Override
	public Context createContext() {
		return null;
	}

	@Override
	public void write(ObjectOutputStream os, Object object) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object read(ObjectInputStream os) throws IOException, ClassNotFoundException {
		throw new UnsupportedOperationException();
	}
}
