package io.automatik.engine.workflow.serverless.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.workflow.serverless.api.WorkflowPropertySource;

public class WorkflowPropertySourceProvider {

	private WorkflowPropertySource workflowPropertySource;

	private static Logger logger = LoggerFactory.getLogger(WorkflowPropertySourceProvider.class);

	public WorkflowPropertySourceProvider() {
		ServiceLoader<WorkflowPropertySource> foundPropertyContext = ServiceLoader.load(WorkflowPropertySource.class);
		Iterator<WorkflowPropertySource> it = foundPropertyContext.iterator();
		if (it.hasNext()) {
			workflowPropertySource = it.next();
			logger.info("Found property source: {}", workflowPropertySource);
		}
	}

	private static class LazyHolder {

		static final WorkflowPropertySourceProvider INSTANCE = new WorkflowPropertySourceProvider();
	}

	public static WorkflowPropertySourceProvider getInstance() {
		return WorkflowPropertySourceProvider.LazyHolder.INSTANCE;
	}

	public WorkflowPropertySource get() {
		return workflowPropertySource;
	}
}