
package io.automatik.engine.workflow.serverless.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.workflow.serverless.api.WorkflowManager;

public class WorkflowManagerProvider {

	private WorkflowManager workflowManager;

	private static Logger logger = LoggerFactory.getLogger(WorkflowManagerProvider.class);

	public WorkflowManagerProvider() {
		ServiceLoader<WorkflowManager> foundWorkflowManagers = ServiceLoader.load(WorkflowManager.class);
		Iterator<WorkflowManager> it = foundWorkflowManagers.iterator();
		if (it.hasNext()) {
			workflowManager = it.next();
			logger.info("Found workflow manager: {}", workflowManager);
		}
	}

	private static class LazyHolder {

		static final WorkflowManagerProvider INSTANCE = new WorkflowManagerProvider();
	}

	public static WorkflowManagerProvider getInstance() {
		return WorkflowManagerProvider.LazyHolder.INSTANCE;
	}

	public WorkflowManager get() {
		// always reset the manager validator and expression validator
		if (workflowManager.getWorkflowValidator() != null) {
			workflowManager.getWorkflowValidator().reset();
			workflowManager.resetExpressionValidator();
		}
		return workflowManager;
	}
}