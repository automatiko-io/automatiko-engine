package io.automatiko.engine.workflow.marshalling.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map.Entry;

import com.google.protobuf.ByteString;
import com.google.protobuf.ByteString.Output;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;

import io.automatiko.engine.api.marshalling.ObjectMarshallingStrategy;
import io.automatiko.engine.api.marshalling.ObjectMarshallingStrategy.Context;
import io.automatiko.engine.workflow.marshalling.impl.AutomatikMessages.Header.StrategyIndex.Builder;

public class PersisterHelper {

	public static void writeToStreamWithHeader(MarshallerWriteContext context, Message payload) throws IOException {
		AutomatikMessages.Header.Builder _header = AutomatikMessages.Header.newBuilder();
		_header.setVersion(AutomatikMessages.Version.newBuilder().setVersionMajor(1).setVersionMinor(0)
				.setVersionRevision(0).build());

		writeStrategiesIndex(context, _header);

		byte[] buff = payload.toByteArray();

		_header.setPayload(ByteString.copyFrom(buff));

		context.stream.write(_header.build().toByteArray());
	}

	private static void writeStrategiesIndex(MarshallerWriteContext context, AutomatikMessages.Header.Builder _header)
			throws IOException {
		for (Entry<ObjectMarshallingStrategy, Integer> entry : context.usedStrategies.entrySet()) {
			Builder _strat = AutomatikMessages.Header.StrategyIndex.newBuilder().setId(entry.getValue().intValue())
					.setName(entry.getKey().getName());

			Context ctx = context.strategyContext.get(entry.getKey());
			if (ctx != null) {
				Output os = ByteString.newOutput();
				ctx.write(new ObjectOutputStream(os));
				_strat.setData(os.toByteString());
				os.close();
			}
			_header.addStrategy(_strat.build());
		}
	}

	private static AutomatikMessages.Header loadStrategiesCheckSignature(MarshallerReaderContext context,
			AutomatikMessages.Header _header) throws ClassNotFoundException, IOException {
		loadStrategiesIndex(context, _header);

		byte[] sessionbuff = _header.getPayload().toByteArray();

		// should we check version as well here?

		return _header;
	}

	public static AutomatikMessages.Header readFromStreamWithHeaderPreloaded(MarshallerReaderContext context,
			ExtensionRegistry registry) throws IOException, ClassNotFoundException {
		// we preload the stream into a byte[] to overcome a message size limit
		// imposed by protobuf
		byte[] preloaded = preload(context.stream);
		AutomatikMessages.Header _header = AutomatikMessages.Header.parseFrom(preloaded, registry);

		return loadStrategiesCheckSignature(context, _header);
	}

	private static byte[] preload(InputStream stream) throws IOException {
		byte[] buf = new byte[4096];
		ByteArrayOutputStream preloaded = new ByteArrayOutputStream();

		int read;
		while ((read = stream.read(buf)) != -1) {
			preloaded.write(buf, 0, read);
		}

		return preloaded.toByteArray();
	}

	private static void loadStrategiesIndex(MarshallerReaderContext context, AutomatikMessages.Header _header)
			throws IOException, ClassNotFoundException {
		for (AutomatikMessages.Header.StrategyIndex _entry : _header.getStrategyList()) {
			ObjectMarshallingStrategy strategyObject = context.resolverStrategyFactory
					.getStrategyObject(_entry.getName());
			if (strategyObject == null) {
				throw new IllegalStateException("No strategy of type " + _entry.getName() + " available.");
			}
			context.usedStrategies.put(_entry.getId(), strategyObject);
			Context ctx = strategyObject.createContext();
			context.strategyContexts.put(strategyObject, ctx);
			if (_entry.hasData() && ctx != null) {
				ClassLoader classLoader = null;
				if (context.classLoader != null) {
					classLoader = context.classLoader;
				}
				ctx.read(new ObjectInputStream(_entry.getData().newInput()));
			}
		}
	}

	public static ExtensionRegistry buildRegistry(MarshallerReaderContext context,
			ProcessMarshaller processMarshaller) {
		ExtensionRegistry registry = ExtensionRegistry.newInstance();
		if (processMarshaller != null) {
			context.parameterObject = registry;
			processMarshaller.init(context);
		}
		return registry;
	}

	public static final byte[] intToByteArray(int value) {
		return new byte[] { (byte) ((value >>> 24) & 0xFF), (byte) ((value >>> 16) & 0xFF),
				(byte) ((value >>> 8) & 0xFF), (byte) (value & 0xFF) };
	}

	public static final int byteArrayToInt(byte[] b) {
		return (b[0] << 24) + ((b[1] & 0xFF) << 16) + ((b[2] & 0xFF) << 8) + (b[3] & 0xFF);
	}

	// more efficient than instantiating byte buffers and opening streams
	public static final byte[] longToByteArray(long value) {
		return new byte[] { (byte) ((value >>> 56) & 0xFF), (byte) ((value >>> 48) & 0xFF),
				(byte) ((value >>> 40) & 0xFF), (byte) ((value >>> 32) & 0xFF), (byte) ((value >>> 24) & 0xFF),
				(byte) ((value >>> 16) & 0xFF), (byte) ((value >>> 8) & 0xFF), (byte) (value & 0xFF) };
	}

	public static final long byteArrayToLong(byte[] b) {
		return ((((long) b[0]) & 0xFF) << 56) + ((((long) b[1]) & 0xFF) << 48) + ((((long) b[2]) & 0xFF) << 40)
				+ ((((long) b[3]) & 0xFF) << 32) + ((((long) b[4]) & 0xFF) << 24) + ((((long) b[5]) & 0xFF) << 16)
				+ ((((long) b[6]) & 0xFF) << 8) + (((long) b[7]) & 0xFF);
	}

}
