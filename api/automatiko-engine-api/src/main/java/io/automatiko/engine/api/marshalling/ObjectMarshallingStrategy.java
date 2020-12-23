
package io.automatiko.engine.api.marshalling;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface ObjectMarshallingStrategy {

	/**
	 * Override this method if you want multiple marshalling strategies of the same
	 * implementation in environment
	 * 
	 * @return the unique name in runtime environment of the
	 *         ObjectMarshallingStrategy
	 */
	default public String getName() {
		return getClass().getName();
	}

	public boolean accept(Object object);

	/**
	 * This method is analogous to the write() method, but instead of writing the
	 * object into an output stream, it returns the marshalled object as a byte[].
	 *
	 * @param context the context for this strategy created by the method
	 *                #createContext()
	 * @param object  the object to be marshalled
	 *
	 * @return the marshalled byte[] of the input object
	 */
	public byte[] marshal(Context context, ObjectOutputStream os, Object object) throws IOException;

	/**
	 * This method is analogous to the read method, but instead of reading it from
	 * an input stream, it reads it from a byte[]
	 *
	 * @param context the context for this strategy created by the method
	 *                #createContext()
	 * @param object  the marshalled object in a byte[]
	 *
	 * @return the unmarshalled Object
	 */
	public Object unmarshal(String dataType, Context context, ObjectInputStream is, byte[] object,
			ClassLoader classloader) throws IOException, ClassNotFoundException;

	/**
	 * Creates a new marshalling context
	 */
	public Context createContext();

	default String getType(Class<?> clazz) {
		return clazz.getCanonicalName();
	}

	public static interface Context {
		/**
		 * Loads the context from the given object input stream
		 */
		public void read(ObjectInputStream ois) throws IOException, ClassNotFoundException;

		/**
		 * Writes the context to the given object output stream
		 */
		public void write(ObjectOutputStream oos) throws IOException;
	}

}
