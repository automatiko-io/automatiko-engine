
package io.automatik.engine.api.workflow.flexible;

import java.io.Serializable;

import io.automatik.engine.api.definition.process.Node;

public class AdHocFragment implements Serializable {

	private final String type;
	private final String name;
	private final boolean autoStart;

	public AdHocFragment(String type, String name, boolean autoStart) {
		this.type = type;
		this.name = name;
		this.autoStart = autoStart;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public boolean isAutoStart() {
		return autoStart;
	}

	@Override
	public String toString() {
		return "AdHocFragment{" + "type='" + type + '\'' + ", name='" + name + '\'' + ", autoStart=" + autoStart + '}';
	}

	public static class Builder {
		private String type;
		private String name;
		private boolean autoStart;

		public Builder(Class<? extends Node> clazz) {
			this.type = clazz.getSimpleName();
		}

		public Builder(String type) {
			this.type = type;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withAutoStart(boolean autoStart) {
			this.autoStart = autoStart;
			return this;
		}

		public AdHocFragment build() {
			return new AdHocFragment(type, name, autoStart);
		}
	}
}
