
package io.automatik.engine.workflow.process.core;

/**
 * Represents a constraint in a RuleFlow. Can be used to specify conditions in
 * (X)OR-splits.
 * 
 */
public interface Constraint {
	/**
	 * Typically this method returns the constraint as a String
	 * 
	 * @return the constraint
	 */
	String getConstraint();

	/**
	 * Method for setting the constraint
	 * 
	 * @param constraint the constraint
	 */
	void setConstraint(String constraint);

	/**
	 * Returns the name of the constraint
	 * 
	 * @return the name of the constraint
	 */
	String getName();

	/**
	 * Sets the name of the constraint
	 * 
	 * @param name the name of the constraint
	 */
	void setName(String name);

	/**
	 * Returns the priority of the constraint
	 * 
	 * @return the priority of the constraint
	 */
	int getPriority();

	/**
	 * Method for setting the priority of the constraint
	 * 
	 * @param priority the priority of the constraint
	 */
	void setPriority(int priority);

	/**
	 * Returns the type of the constraint, e.g. "code" or "rule"
	 * 
	 * @return the type of the constraint
	 */
	String getType();

	/**
	 * Method for setting the type of the constraint, e.g. "code" or "rule"
	 * 
	 * @param type the type of the constraint
	 */
	void setType(String type);

	/**
	 * Returns the dialect of the constraint, e.g. "mvel" or "java"
	 * 
	 * @return the dialect of the constraint
	 */
	String getDialect();

	/**
	 * Method for setting the dialect of the constraint, e.g. "mvel" or "java"
	 * 
	 * @param dialect the dialect of the constraint
	 */
	void setDialect(String dialect);

	public boolean isDefault();

	public void setDefault(boolean isDefault);

	void setMetaData(String name, Object value);

	Object getMetaData(String name);

}
