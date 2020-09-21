package io.automatik.engine.api.workflow;

public interface VariableInitializer {

	/**
	 * Initializes new instance of the given class
	 * 
	 * @param clazz type of object to initialize
	 * @return new instance of given class
	 */
	Object initialize(String name, Class<?> clazz);
}
