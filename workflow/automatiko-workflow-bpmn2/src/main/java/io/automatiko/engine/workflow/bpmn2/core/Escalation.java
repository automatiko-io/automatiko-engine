
package io.automatiko.engine.workflow.bpmn2.core;

import java.io.Serializable;

public class Escalation extends Signal implements Serializable {

	private static final long serialVersionUID = 510l;

	private String escalationCode;

	public Escalation(String id, String structureRef, String escalationCode) {
		super(id, structureRef);
		this.escalationCode = escalationCode;
	}

	public Escalation(String id, String escalationCode) {
		super(id, null);
		this.escalationCode = escalationCode;
	}

	public String getEscalationCode() {
		return escalationCode;
	}

}
