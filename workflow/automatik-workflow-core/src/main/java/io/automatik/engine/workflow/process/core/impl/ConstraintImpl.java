
package io.automatik.engine.workflow.process.core.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import io.automatik.engine.workflow.process.core.Constraint;

/**
 * Default implementation of a constraint.
 * 
 */
public class ConstraintImpl implements Constraint, Serializable {

	private static final long serialVersionUID = 510l;

	private Map<String, Object> metaData = new HashMap<String, Object>();

	private String name;
	private String constraint;
	private int priority;
	private String dialect = "mvel";
	private String type = "rule";
	private boolean isDefault = false;

	public String getConstraint() {
		return this.constraint;
	}

	public void setConstraint(final String constraint) {
		this.constraint = constraint;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String toString() {
		return this.name;
	}

	public int getPriority() {
		return this.priority;
	}

	public void setPriority(final int priority) {
		this.priority = priority;
	}

	public String getDialect() {
		return dialect;
	}

	public void setDialect(String dialect) {
		this.dialect = dialect;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public void setMetaData(String name, Object value) {
		this.metaData.put(name, value);
	}

	public Object getMetaData(String name) {
		return this.metaData.get(name);
	}

}
