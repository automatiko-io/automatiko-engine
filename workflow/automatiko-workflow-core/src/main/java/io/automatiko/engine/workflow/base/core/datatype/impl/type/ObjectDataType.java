
package io.automatiko.engine.workflow.base.core.datatype.impl.type;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import io.automatiko.engine.api.workflow.datatype.DataType;
import io.automatiko.engine.workflow.base.core.datatype.impl.coverter.TypeConverterRegistry;

/**
 * Representation of an object datatype.
 */
public class ObjectDataType implements DataType {

	private static final long serialVersionUID = 510l;

	private String className;

	private ClassLoader classLoader;

	private Class<?> clazz;

	public ObjectDataType() {
	}

	public ObjectDataType(Class<?> clazz) {
		setClassName(clazz.getCanonicalName());
		this.clazz = clazz;
	}

	public ObjectDataType(Class<?> clazz, String className) {
		setClassLoader(classLoader);
		setClassName(className);
		this.clazz = clazz;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className == null ? "java.lang.Object" : className;
	}

	public ClassLoader getClassLoader() {
		return classLoader == null ? this.getClass().getClassLoader() : classLoader;
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

		if (clazz.isInstance(value)) {
			return true;
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

	@Override
	public Class<?> getClassType() {
		return clazz == null ? Object.class : clazz;
	}
}
