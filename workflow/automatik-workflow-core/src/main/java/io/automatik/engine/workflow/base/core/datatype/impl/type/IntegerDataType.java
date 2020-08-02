
package io.automatik.engine.workflow.base.core.datatype.impl.type;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import io.automatik.engine.workflow.base.core.datatype.DataType;

/**
 * Representation of an integer datatype.
 */
public class IntegerDataType implements DataType {

	private static final long serialVersionUID = 510l;

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}

	public void writeExternal(ObjectOutput out) throws IOException {
	}

	public boolean verifyDataType(final Object value) {
		if (value instanceof Integer) {
			return true;
		} else if (value == null) {
			return true;
		} else {
			return false;
		}
	}

	public Object readValue(String value) {
		return Integer.parseInt(value);
	}

	public String writeValue(Object value) {
		Integer i = (Integer) value;
		return i == null ? "" : i.toString();
	}

	public String getStringType() {
		return "java.lang.Integer";
	}

	@Override
	public Class<?> getClassType() {
		return Integer.class;
	}

}
