
package io.automatik.engine.workflow.base.core.datatype.impl;

import io.automatik.engine.api.workflow.datatype.DataType;
import io.automatik.engine.workflow.base.core.datatype.DataTypeFactory;

/**
 * A data type factory that always returns a new instance of a given class.
 */
public class NewInstanceDataTypeFactory implements DataTypeFactory {

	private static final long serialVersionUID = 510l;

	private Class<? extends DataType> dataTypeClass;

	public NewInstanceDataTypeFactory(final Class<? extends DataType> dataTypeClass) {
		this.dataTypeClass = dataTypeClass;
	}

	public DataType createDataType() {
		try {
			return this.dataTypeClass.newInstance();
		} catch (final IllegalAccessException e) {
			throw new RuntimeException("Could not create data type for class " + this.dataTypeClass, e);
		} catch (final InstantiationException e) {
			throw new RuntimeException("Could not create data type for class " + this.dataTypeClass, e);
		}
	}

}
