
package io.automatiko.engine.addons.process.management.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.automatiko.engine.addons.process.management.exception.BaseExceptionHandler;
import io.automatiko.engine.api.workflow.NodeInstanceNotFoundException;
import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatiko.engine.api.workflow.ProcessInstanceNotFoundException;
import io.automatiko.engine.api.workflow.VariableViolationException;
import io.automatiko.engine.api.workflow.workitem.InvalidLifeCyclePhaseException;
import io.automatiko.engine.api.workflow.workitem.InvalidTransitionException;
import io.automatiko.engine.api.workflow.workitem.NotAuthorizedException;

@ExtendWith(MockitoExtension.class)
class BaseExceptionHandlerTest {

	private BaseExceptionHandler tested;

	@Mock
	private Object badRequestResponse;

	@Mock
	private Object conflictResponse;

	@Mock
	private Object internalErrorResponse;

	@Mock
	private Object notFoundResponse;

	@Mock
	private Object forbiddenResponse;

	@BeforeEach
	void setUp() {
		tested = spy(new BaseExceptionHandler() {
			@Override
			protected Object badRequest(Object body) {
				return badRequestResponse;
			}

			@Override
			protected Object conflict(Object body) {
				return conflictResponse;
			}

			@Override
			protected Object internalError(Object body) {
				return internalErrorResponse;
			}

			@Override
			protected Object notFound(Object body) {
				return notFoundResponse;
			}

			@Override
			protected Object forbidden(Object body) {
				return forbiddenResponse;
			}
		});
	}

	@Test
	void testMapInvalidLifeCyclePhaseException() {
		Object response = tested.mapException(new InvalidLifeCyclePhaseException("message"));
		assertThat(response).isEqualTo(badRequestResponse);
	}

	@Test
	void testMapInvalidTransitionException() {
		Object response = tested.mapException(new InvalidTransitionException("message"));
		assertThat(response).isEqualTo(badRequestResponse);
	}

	@Test
	void testMapNodeInstanceNotFoundException() {
		Object response = tested.mapException(new NodeInstanceNotFoundException("processInstanceId", "nodeInstanceId"));
		assertThat(response).isEqualTo(notFoundResponse);
	}

	@Test
	void testMapNotAuthorizedException() {
		Object response = tested.mapException(new NotAuthorizedException("message"));
		assertThat(response).isEqualTo(forbiddenResponse);
	}

	@Test
	void testMapProcessInstanceDuplicatedException() {
		Object response = tested.mapException(new ProcessInstanceDuplicatedException("processInstanceId"));
		assertThat(response).isEqualTo(conflictResponse);
	}

	@Test
	void testMapProcessInstanceExecutionException() {
		Object response = tested
				.mapException(new ProcessInstanceExecutionException("processInstanceId", "nodeId", "message"));
		assertThat(response).isEqualTo(internalErrorResponse);
	}

	@Test
	void testMapProcessInstanceNotFoundException() {
		Object response = tested.mapException(new ProcessInstanceNotFoundException("processInstanceId"));
		assertThat(response).isEqualTo(notFoundResponse);
	}

	@Test
	void testMapVariableViolationException() {
		Object response = tested
				.mapException(new VariableViolationException("processInstanceId", "variable", "message"));
		assertThat(response).isEqualTo(badRequestResponse);
	}
}