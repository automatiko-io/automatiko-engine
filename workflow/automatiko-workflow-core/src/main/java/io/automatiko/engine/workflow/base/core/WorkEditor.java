
package io.automatiko.engine.workflow.base.core;

public interface WorkEditor {

	void setWorkDefinition(WorkDefinition definition);

	void setWork(Work work);

	boolean show();

	Work getWork();

}
