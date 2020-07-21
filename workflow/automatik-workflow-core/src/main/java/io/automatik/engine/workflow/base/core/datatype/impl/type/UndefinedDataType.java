
package io.automatik.engine.workflow.base.core.datatype.impl.type;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import io.automatik.engine.workflow.base.core.datatype.DataType;

/**
 * Representation of an undefined datatype.
 */
public final class UndefinedDataType implements DataType {

	private static final long serialVersionUID = 510l;
	private static UndefinedDataType instance;

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}

	public void writeExternal(ObjectOutput out) throws IOException {
	}

	public static UndefinedDataType getInstance() {
		if (instance == null) {
			instance = new UndefinedDataType();
		}
		return instance;
	}

	public boolean verifyDataType(final Object value) {
		if (value == null) {
			return true;
		}
		return false;
	}

	public Object readValue(String value) {
		throw new IllegalArgumentException("Undefined datatype");
	}

	public String writeValue(Object value) {
		throw new IllegalArgumentException("Undefined datatype");
	}

	public String getStringType() {
		return "java.lang.Object";
	}
}
