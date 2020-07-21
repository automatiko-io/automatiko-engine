package io.automatik.engine.workflow.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import io.automatik.engine.workflow.base.core.datatype.DataType;

public class CustomDataType implements DataType {

	private static final long serialVersionUID = 510l;

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}

	public void writeExternal(ObjectOutput out) throws IOException {
	}

	public boolean verifyDataType(final Object value) {
		if (value instanceof String) {
			return true;
		} else if (value == null) {
			return true;
		} else {
			return false;
		}
	}

	public Object readValue(String value) {
		return value;
	}

	public String writeValue(Object value) {
		return (String) value;
	}

	public String getStringType() {
		return "java.lang.String";
	}
}
