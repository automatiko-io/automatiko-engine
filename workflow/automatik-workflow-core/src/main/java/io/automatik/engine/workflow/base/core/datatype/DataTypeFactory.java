
package io.automatik.engine.workflow.base.core.datatype;

import java.io.Serializable;

/**
 * A factory for creating a datatype.
 */
public interface DataTypeFactory extends Serializable {

	DataType createDataType();

}
