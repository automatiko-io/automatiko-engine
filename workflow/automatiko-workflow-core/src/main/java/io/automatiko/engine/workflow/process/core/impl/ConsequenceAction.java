
package io.automatiko.engine.workflow.process.core.impl;

import java.io.Serializable;

import io.automatiko.engine.workflow.process.core.ProcessAction;

public class ConsequenceAction extends ProcessAction implements Serializable {

	private static final long serialVersionUID = 510l;

	private String dialect = "mvel";
	private String consequence;

	public ConsequenceAction() {
	}

	public ConsequenceAction(String dialect, String consequence) {
		this.dialect = dialect;
		this.consequence = consequence;
	}

	public void setConsequence(String consequence) {
		this.consequence = consequence;
	}

	public String getConsequence() {
		return consequence;
	}

	public void setDialect(String dialect) {
		this.dialect = dialect;
	}

	public String getDialect() {
		return dialect;
	}

	public String toString() {
		return consequence;
	}
}
