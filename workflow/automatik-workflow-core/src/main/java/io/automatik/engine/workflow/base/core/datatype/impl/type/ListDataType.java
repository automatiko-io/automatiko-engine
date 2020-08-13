
package io.automatik.engine.workflow.base.core.datatype.impl.type;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

import io.automatik.engine.workflow.base.core.TypeObject;
import io.automatik.engine.workflow.base.core.datatype.DataType;

/**
 * Representation of a list datatype. All elements in the list must have the
 * same datatype.
 */
public class ListDataType extends ObjectDataType implements TypeObject {

	private static final long serialVersionUID = 510l;

	private DataType dataType;

	public ListDataType() {
		super(java.util.List.class);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		dataType = (DataType) in.readObject();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(dataType);
	}

	public ListDataType(DataType dataType) {
		setType(dataType);
	}

	public void setType(final DataType dataType) {
		this.dataType = dataType;
	}

	public DataType getType() {
		return this.dataType;
	}

	public boolean verifyDataType(final Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof List) {
			for (Object o : (List<?>) value) {
				if (dataType != null && !dataType.verifyDataType(o)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
}
