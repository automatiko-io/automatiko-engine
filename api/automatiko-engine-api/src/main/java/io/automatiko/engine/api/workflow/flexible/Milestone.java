
package io.automatiko.engine.api.workflow.flexible;

public class Milestone extends ItemDescription {

	private Milestone(String id, String name, Status status) {
		super(id, name, status);
	}

	@Override
	public String toString() {
		return "Milestone{" + super.toString() + "}";
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String id;
		private String name;
		private Status status;

		public Builder() {
		}

		public Builder withId(String id) {
			this.id = id;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withStatus(Status status) {
			this.status = status;
			return this;
		}

		public Milestone build() {
			return new Milestone(id, name, status);
		}
	}

}
