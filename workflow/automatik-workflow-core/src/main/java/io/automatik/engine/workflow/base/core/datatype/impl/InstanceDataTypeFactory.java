
package io.automatik.engine.workflow.base.core.datatype.impl;

import io.automatik.engine.api.workflow.datatype.DataType;
import io.automatik.engine.workflow.base.core.datatype.DataTypeFactory;

/**
 * A data type factory that always returns the same instance of a given class.
 */
public class InstanceDataTypeFactory implements DataTypeFactory {

	private static final long serialVersionUID = 510l;

	private Class<?> dataTypeClass;
	private DataType instance;

	public InstanceDataTypeFactory(final Class<?> dataTypeClass) {
		this.dataTypeClass = dataTypeClass;
	}

	public DataType createDataType() {
		if (this.instance == null) {
			try {
				this.instance = (DataType) this.dataTypeClass.newInstance();
			} catch (final IllegalAccessException e) {
				throw new RuntimeException("Could not create data type for class " + this.dataTypeClass, e);
			} catch (final InstantiationException e) {
				throw new RuntimeException("Could not create data type for class " + this.dataTypeClass, e);
			}
		}
		return this.instance;
	}

}
