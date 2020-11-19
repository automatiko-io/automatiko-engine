
package io.automatik.engine.workflow.base.core.datatype;

import java.io.Serializable;

import io.automatik.engine.api.workflow.datatype.DataType;

/**
 * A factory for creating a datatype.
 */
public interface DataTypeFactory extends Serializable {

	DataType createDataType();

}
