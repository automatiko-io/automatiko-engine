package io.automatiko.engine.api.workflow;

public interface ProcessError {

	String failedNodeId();

	String errorMessage();

	void retrigger();

	void skip();
}
