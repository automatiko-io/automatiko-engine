
package io.automatik.engine.workflow.serverless.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.workflow.serverless.api.WorkflowValidator;

public class WorkflowValidatorProvider {

	private WorkflowValidator workflowValidator;

	private static Logger logger = LoggerFactory.getLogger(WorkflowValidatorProvider.class);

	public WorkflowValidatorProvider() {
		ServiceLoader<WorkflowValidator> foundWorkflowValidators = ServiceLoader.load(WorkflowValidator.class);
		Iterator<WorkflowValidator> it = foundWorkflowValidators.iterator();
		if (it.hasNext()) {
			workflowValidator = it.next();
			logger.info("Found workflow validator: {}", workflowValidator);
		}
	}

	private static class LazyHolder {

		static final WorkflowValidatorProvider INSTANCE = new WorkflowValidatorProvider();
	}

	public static WorkflowValidatorProvider getInstance() {
		return LazyHolder.INSTANCE;
	}

	public WorkflowValidator get() {
		return workflowValidator;
	}
}