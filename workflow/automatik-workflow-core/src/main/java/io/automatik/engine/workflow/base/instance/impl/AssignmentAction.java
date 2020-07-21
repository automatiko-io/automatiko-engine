
package io.automatik.engine.workflow.base.instance.impl;

import io.automatik.engine.api.runtime.process.ProcessContext;
import io.automatik.engine.api.runtime.process.WorkItem;

public interface AssignmentAction {

	void execute(WorkItem workItem, ProcessContext context) throws Exception;

}
