
package io.automatik.engine.addons.process.management.exception;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ExceptionsHandler extends BaseExceptionHandler<Response> {

	@Override
	protected <R> Response badRequest(R body) {
		return Response.status(Response.Status.BAD_REQUEST).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.entity(body).build();
	}

	@Override
	protected <R> Response conflict(R body) {
		return Response.status(Response.Status.CONFLICT).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.entity(body).build();
	}

	@Override
	protected <R> Response internalError(R body) {
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).entity(body).build();
	}

	@Override
	protected <R> Response notFound(R body) {
		return Response.status(Response.Status.NOT_FOUND).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.entity(body).build();
	}

	@Override
	protected <R> Response forbidden(R body) {
		return Response.status(Response.Status.FORBIDDEN).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.entity(body).build();
	}
}
