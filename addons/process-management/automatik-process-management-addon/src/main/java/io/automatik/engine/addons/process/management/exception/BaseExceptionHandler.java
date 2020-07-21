
package io.automatik.engine.addons.process.management.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.automatik.engine.api.workflow.NodeInstanceNotFoundException;
import io.automatik.engine.api.workflow.NodeNotFoundException;
import io.automatik.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatik.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatik.engine.api.workflow.ProcessInstanceNotFoundException;
import io.automatik.engine.api.workflow.VariableViolationException;
import io.automatik.engine.api.workflow.workitem.InvalidLifeCyclePhaseException;
import io.automatik.engine.api.workflow.workitem.InvalidTransitionException;
import io.automatik.engine.api.workflow.workitem.NotAuthorizedException;

public abstract class BaseExceptionHandler<T> {

	public static final String MESSAGE = "message";
	public static final String PROCESS_INSTANCE_ID = "processInstanceId";
	public static final String VARIABLE = "variable";
	public static final String NODE_INSTANCE_ID = "nodeInstanceId";
	public static final String NODE_ID = "nodeId";
	public static final String FAILED_NODE_ID = "failedNodeId";
	public static final String ID = "id";
	private final Map<Class<? extends Exception>, Function<Exception, T>> mapper;

	public BaseExceptionHandler() {
		mapper = new HashMap<>();
		mapper.put(InvalidLifeCyclePhaseException.class,
				ex -> badRequest(Collections.singletonMap(MESSAGE, ex.getMessage())));

		mapper.put(InvalidTransitionException.class,
				ex -> badRequest(Collections.singletonMap(MESSAGE, ex.getMessage())));

		mapper.put(NodeInstanceNotFoundException.class, ex -> {
			NodeInstanceNotFoundException exception = (NodeInstanceNotFoundException) ex;
			Map<String, String> response = new HashMap<>();
			response.put(MESSAGE, exception.getMessage());
			response.put(PROCESS_INSTANCE_ID, exception.getProcessInstanceId());
			response.put(NODE_INSTANCE_ID, exception.getNodeInstanceId());
			return notFound(response);
		});

		mapper.put(NodeNotFoundException.class, ex -> {
			NodeNotFoundException exception = (NodeNotFoundException) ex;
			Map<String, String> response = new HashMap<>();
			response.put(MESSAGE, exception.getMessage());
			response.put(PROCESS_INSTANCE_ID, exception.getProcessInstanceId());
			response.put(NODE_ID, exception.getNodeId());
			return notFound(response);
		});

		mapper.put(NotAuthorizedException.class, ex -> forbidden(Collections.singletonMap(MESSAGE, ex.getMessage())));

		mapper.put(ProcessInstanceDuplicatedException.class, ex -> {
			ProcessInstanceDuplicatedException exception = (ProcessInstanceDuplicatedException) ex;
			Map<String, String> response = new HashMap<>();
			response.put(MESSAGE, exception.getMessage());
			response.put(PROCESS_INSTANCE_ID, exception.getProcessInstanceId());
			return conflict(response);
		});

		mapper.put(ProcessInstanceExecutionException.class, ex -> {
			ProcessInstanceExecutionException exception = (ProcessInstanceExecutionException) ex;
			Map<String, String> response = new HashMap<>();
			response.put(ID, exception.getProcessInstanceId());
			response.put(FAILED_NODE_ID, exception.getFailedNodeId());
			response.put(MESSAGE, exception.getErrorMessage());
			return internalError(response);
		});

		mapper.put(ProcessInstanceNotFoundException.class, ex -> {
			ProcessInstanceNotFoundException exception = (ProcessInstanceNotFoundException) ex;
			Map<String, String> response = new HashMap<>();
			response.put(MESSAGE, exception.getMessage());
			response.put(PROCESS_INSTANCE_ID, exception.getProcessInstanceId());
			return notFound(response);
		});

		mapper.put(VariableViolationException.class, ex -> {
			VariableViolationException exception = (VariableViolationException) ex;
			Map<String, String> response = new HashMap<>();
			response.put(MESSAGE, exception.getMessage() + " : " + exception.getErrorMessage());
			response.put(PROCESS_INSTANCE_ID, exception.getProcessInstanceId());
			response.put(VARIABLE, exception.getVariableName());
			return badRequest(response);
		});
	}

	protected abstract <R> T badRequest(R body);

	protected abstract <R> T conflict(R body);

	protected abstract <R> T internalError(R body);

	protected abstract <R> T notFound(R body);

	protected abstract <R> T forbidden(R body);

	public <R extends Exception> T mapException(R exception) {
		return mapper.getOrDefault(exception.getClass(), this::internalError).apply(exception);
	}
}
