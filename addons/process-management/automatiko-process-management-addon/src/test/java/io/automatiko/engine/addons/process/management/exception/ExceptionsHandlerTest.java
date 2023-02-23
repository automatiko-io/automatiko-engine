
package io.automatiko.engine.addons.process.management.exception;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.automatiko.engine.addons.process.management.exception.ExceptionsHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExceptionsHandlerTest {

	private ExceptionsHandler tested;

	@Mock
	private Object body;

	@Mock
	private RuntimeDelegate runtimeDelegate;

	@Mock
	private Response.ResponseBuilder builder;

	@Mock
	private Response response;

	@BeforeEach
	void setUp() {
		tested = new ExceptionsHandler();
		RuntimeDelegate.setInstance(runtimeDelegate);
		when(runtimeDelegate.createResponseBuilder()).thenReturn(builder);
		when(builder.status(any(Response.StatusType.class))).thenReturn(builder);
		when(builder.header(anyString(), any())).thenReturn(builder);
		when(builder.entity(any())).thenReturn(builder);
		when(builder.build()).thenReturn(response);
	}

	@Test
	void testBadRequest() {
		tested.badRequest(body);
		assertRequest(Response.Status.BAD_REQUEST);
	}

	private void assertRequest(Response.Status status) {
		verify(builder).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
		verify(builder).status((Response.StatusType) status);
		verify(builder).entity(body);
	}

	@Test
	void testConflict() {
		tested.conflict(body);
		assertRequest(Response.Status.CONFLICT);
	}

	@Test
	void testInternalError() {
		tested.internalError(body);
		assertRequest(Response.Status.INTERNAL_SERVER_ERROR);
	}

	@Test
	void testNotFound() {
		tested.notFound(body);
		assertRequest(Response.Status.NOT_FOUND);
	}

	@Test
	void testForbidden() {
		tested.forbidden(body);
		assertRequest(Response.Status.FORBIDDEN);
	}
}