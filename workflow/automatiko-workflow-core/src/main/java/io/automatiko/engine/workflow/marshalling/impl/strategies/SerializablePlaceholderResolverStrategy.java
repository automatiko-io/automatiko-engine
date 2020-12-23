package io.automatiko.engine.workflow.marshalling.impl.strategies;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.automatiko.engine.api.marshalling.ObjectMarshallingStrategy;
import io.automatiko.engine.api.marshalling.ObjectMarshallingStrategyAcceptor;
import io.automatiko.engine.workflow.marshalling.impl.PersisterHelper;

public class SerializablePlaceholderResolverStrategy implements ObjectMarshallingStrategy {

	private int index;

	private ObjectMarshallingStrategyAcceptor acceptor;

	public SerializablePlaceholderResolverStrategy(ObjectMarshallingStrategyAcceptor acceptor) {
		this.acceptor = acceptor;
	}

	public int getIndex() {
		return this.index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public boolean accept(Object object) {
		return acceptor.accept(object);
	}

	public byte[] marshal(Context context, ObjectOutputStream os, Object object) throws IOException {

		SerializablePlaceholderStrategyContext ctx = (SerializablePlaceholderStrategyContext) context;
		int index = ctx.data.size();
		ctx.data.add(object);
		return PersisterHelper.intToByteArray(index);
	}

	public Object unmarshal(String dataType, Context context, ObjectInputStream is, byte[] object,
			ClassLoader classloader) throws IOException, ClassNotFoundException {
		SerializablePlaceholderStrategyContext ctx = (SerializablePlaceholderStrategyContext) context;
		return ctx.data.get(PersisterHelper.byteArrayToInt(object));
	}

	public Context createContext() {
		return new SerializablePlaceholderStrategyContext();
	}

	protected static class SerializablePlaceholderStrategyContext implements Context {
		// this data map is used when marshalling out objects in order
		// to preserve graph references without cloning objects all over
		// the place.
		public List<Object> data = new ArrayList<Object>();

		@SuppressWarnings("unchecked")
		public void read(ObjectInputStream ois) throws IOException, ClassNotFoundException {
			this.data = (List<Object>) ois.readObject();
		}

		public void write(ObjectOutputStream oos) throws IOException {
			oos.writeObject(this.data);
		}
	}

	@Override
	public String toString() {
		return "SerializablePlaceholderResolverStrategy{" + "acceptor=" + acceptor + '}';
	}
}
