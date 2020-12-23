
package io.automatiko.engine.services.event.impl;

import java.util.Objects;

public class MilestoneEventBody {

	private String id;
	private String name;
	private String status;

	private MilestoneEventBody() {
	}

	public static Builder create() {
		return new Builder(new MilestoneEventBody());
	}

	public String getName() {
		return name;
	}

	public String getStatus() {
		return status;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "MilestoneEventBody{" + "name='" + name + '\'' + ", status='" + status + '\'' + ", id='" + id + '\''
				+ '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof MilestoneEventBody)) {
			return false;
		}
		MilestoneEventBody that = (MilestoneEventBody) o;
		return getId().equals(that.getId()) && getName().equals(that.getName()) && getStatus().equals(that.getStatus());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getName(), getStatus());
	}

	public Builder update() {
		return new Builder(this);
	}

	public static class Builder {

		private MilestoneEventBody instance;

		private Builder(MilestoneEventBody instance) {
			this.instance = instance;
		}

		public Builder id(String id) {
			instance.id = id;
			return this;
		}

		public Builder name(String name) {
			instance.name = name;
			return this;
		}

		public Builder status(String status) {
			instance.status = status;
			return this;
		}

		public MilestoneEventBody build() {
			return instance;
		}
	}
}
