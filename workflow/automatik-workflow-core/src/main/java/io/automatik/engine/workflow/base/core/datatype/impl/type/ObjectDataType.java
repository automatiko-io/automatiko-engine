
package io.automatik.engine.workflow.base.core.datatype.impl.type;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import io.automatik.engine.workflow.base.core.datatype.DataType;
import io.automatik.engine.workflow.base.core.datatype.impl.coverter.TypeConverterRegistry;

/**
 * Representation of an object datatype.
 */
public class ObjectDataType implements DataType {

	private static final long serialVersionUID = 510l;

	private String className;

	private ClassLoader classLoader;

	public ObjectDataType() {
	}

	public ObjectDataType(String className) {
		setClassName(className);
	}

	public ObjectDataType(String className, ClassLoader classLoader) {
		setClassName(className);
		setClassLoader(classLoader);
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		className = (String) in.readObject();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(className);
	}

	public boolean verifyDataType(final Object value) {
		if (value == null) {
			return true;
		}
		try {
			Class<?> clazz = Class.forName(className, true, value.getClass().getClassLoader());
			if (clazz.isInstance(value)) {
				return true;
			}
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Could not find data type " + className);
		}
		return false;
	}

	public Object readValue(String value) {
		return TypeConverterRegistry.get().forType(getStringType()).apply(value);
	}

	public String writeValue(Object value) {
		return value.toString();
	}

	public String getStringType() {
		return className == null ? "java.lang.Object" : className;
	}
}
