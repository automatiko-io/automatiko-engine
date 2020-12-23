
package io.automatiko.engine.workflow.bpmn2.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Interface implements Serializable {

	private static final long serialVersionUID = 510l;

	private String id;
	private String name;
	private String implementationRef;
	private Map<String, Operation> operations = new HashMap<String, Operation>();

	public Interface(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Operation addOperation(String id, String name) {
		Operation operation = new Operation(id, name);
		operations.put(id, operation);
		return operation;
	}

	public Operation getOperation(String name) {
		return operations.get(name);
	}

	public void setImplementationRef(String implementationRef) {
		this.implementationRef = implementationRef;
	}

	public String getImplementationRef() {
		return implementationRef;
	}

	public class Operation {
		private String id;
		private String name;
		private Message message;
		private String implementationRef;

		public Operation(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Message getMessage() {
			return message;
		}

		public void setMessage(Message message) {
			this.message = message;
		}

		public Interface getInterface() {
			return Interface.this;
		}

		public void setImplementationRef(String implementationRef) {
			this.implementationRef = implementationRef;
		}

		public String getImplementationRef() {
			return implementationRef;
		}
	}

}
