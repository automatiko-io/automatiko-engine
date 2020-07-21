
package io.automatik.engine.api.workflow.flexible;

import java.io.Serializable;

public abstract class ItemDescription implements Serializable {

	public enum Status {
		AVAILABLE, ACTIVE, COMPLETED
	}

	private final String id;
	private final String name;
	private final Status status;

	ItemDescription(String id, String name, Status status) {
		this.id = id;
		this.name = name;
		this.status = status;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Status getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return "ItemDescription{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", status=" + status + '}';
	}
}
