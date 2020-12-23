
package io.automatiko.engine.workflow.bpmn2.core;

import java.io.Serializable;

public class Signal implements Serializable {

	private static final long serialVersionUID = 510l;

	private String id;
	private String name;
	private String structureRef;

	public Signal(String id, String structureRef) {
		this.id = id;
		this.structureRef = structureRef;
	}

	public Signal(String id, String name, String structureRef) {
		this(id, structureRef);
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getStructureRef() {
		return structureRef;
	}

	public String getName() {
		return name;
	}

}
