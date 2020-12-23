
package io.automatiko.engine.workflow.base.core.datatype.impl.type;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import io.automatiko.engine.api.workflow.datatype.DataType;

/**
 * Representation of a boolean datatype.
 */
public final class BooleanDataType implements DataType {

	private static final long serialVersionUID = 510l;

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}

	public void writeExternal(ObjectOutput out) throws IOException {
	}

	public boolean verifyDataType(final Object value) {
		if (value instanceof Boolean) {
			return true;
		}
		return false;
	}

	public Object readValue(String value) {
		return Boolean.parseBoolean(value);
	}

	public String writeValue(Object value) {
		return (Boolean) value ? "true" : "false";
	}

	public String getStringType() {
		return "java.lang.Boolean";
	}

	@Override
	public Class<?> getClassType() {
		return Boolean.class;
	}
}
