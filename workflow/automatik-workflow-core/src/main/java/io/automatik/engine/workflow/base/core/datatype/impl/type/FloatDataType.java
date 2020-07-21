
package io.automatik.engine.workflow.base.core.datatype.impl.type;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import io.automatik.engine.workflow.base.core.datatype.DataType;

/**
 * Representation of a float datatype.
 */
public final class FloatDataType implements DataType {

	private static final long serialVersionUID = 510l;

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}

	public void writeExternal(ObjectOutput out) throws IOException {
	}

	public boolean verifyDataType(final Object value) {
		if (value instanceof Float) {
			return true;
		} else if (value == null) {
			return true;
		} else {
			return false;
		}
	}

	public Object readValue(String value) {
		return new Float(value);
	}

	public String writeValue(Object value) {
		Float f = (Float) value;
		return f == null ? "" : f.toString();
	}

	public String getStringType() {
		return "java.lang.Float";
	}
}
