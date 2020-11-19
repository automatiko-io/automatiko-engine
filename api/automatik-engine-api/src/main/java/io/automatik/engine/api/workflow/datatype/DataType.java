
package io.automatik.engine.api.workflow.datatype;

import java.io.Externalizable;

/**
 * Abstract representation of a datatype.
 */
public interface DataType extends Externalizable {

	/**
	 * Returns true if the given value is a valid value of this data type.
	 */
	boolean verifyDataType(Object value);

	String writeValue(Object value);

	Object readValue(String value);

	/**
	 * Returns the corresponding Java type of this datatype
	 */
	String getStringType();

	Class<?> getClassType();

}
