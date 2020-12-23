
package io.automatiko.engine.workflow.bpmn2.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemHandler;
import io.automatiko.engine.api.runtime.process.WorkItemManager;

public class SendTaskHandler implements WorkItemHandler {

	private static final Logger logger = LoggerFactory.getLogger(SendTaskHandler.class);

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		String message = (String) workItem.getParameter("Message");
		logger.debug("Sending message: {}", message);
		manager.completeWorkItem(workItem.getId(), null);
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, cannot be aborted
	}

}
