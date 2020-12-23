
package io.automatiko.engine.workflow.base.instance.impl;

import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.api.runtime.process.WorkItem;

public interface AssignmentAction {

	void execute(WorkItem workItem, ProcessContext context) throws Exception;

}
