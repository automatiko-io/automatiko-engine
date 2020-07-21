
package io.automatik.engine.services.event.impl;

public class ProcessErrorEventBody {

	private String nodeDefinitionId;
	private String errorMessage;

	private ProcessErrorEventBody() {
	}

	public String getNodeDefinitionId() {
		return nodeDefinitionId;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public String toString() {
		return "ProcessError [nodeDefinitionId=" + nodeDefinitionId + ", errorMessage=" + errorMessage + "]";
	}

	public static Builder create() {
		return new Builder(new ProcessErrorEventBody());
	}

	static class Builder {

		private ProcessErrorEventBody instance;

		private Builder(ProcessErrorEventBody instance) {
			this.instance = instance;
		}

		public Builder nodeDefinitionId(String nodeDefinitionId) {
			instance.nodeDefinitionId = nodeDefinitionId;
			return this;
		}

		public Builder errorMessage(String errorMessage) {
			instance.errorMessage = errorMessage;
			return this;
		}

		public ProcessErrorEventBody build() {
			return instance;
		}
	}
}
