
package io.automatiko.engine.workflow.process.core;

/**
 * Represents a connection between two nodes in a workflow.
 * 
 */
public interface Connection extends io.automatiko.engine.api.definition.process.Connection {

	void setMetaData(String name, Object value);

}
