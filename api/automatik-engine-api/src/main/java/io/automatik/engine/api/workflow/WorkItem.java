
package io.automatik.engine.api.workflow;

import java.util.Map;

public interface WorkItem {

	String getId();

	String getNodeInstanceId();

	String getProcessInstanceId();

	String getName();

	int getState();

	String getPhase();

	String getPhaseStatus();

	Map<String, Object> getParameters();

	Map<String, Object> getResults();

	/**
	 * Reference id gives a complete path like id to this work item. It will
	 * represent a complete navigation to work item traversing subprocesses <br/>
	 * - root level work item <code>Name/ID</code> <br/>
	 * - first subprocess level work item
	 * <code>subprocess-name/subprocess-id/Name/ID</code> <br/>
	 * - second subprocess level work item
	 * <code>subprocess-name/subprocess-id/subprocess-name2/subprocess-id2/Name/ID</code>
	 * <br/>
	 * 
	 * @return returns complete path like reference id of this work item
	 */
	String getReferenceId();

	default Descriptor toMap() {

		return new Descriptor(getId(), getName().replaceAll("\\s", "_"), getReferenceId().replaceAll("\\s", "_"));
	}

	public static class Descriptor {
		String id;
		String name;
		String reference;

		public Descriptor(String id, String name, String reference) {
			this.id = id;
			this.name = name;
			this.reference = reference;
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

		public String getReference() {
			return reference;
		}

		public void setReference(String reference) {
			this.reference = reference;
		}

	}
}
