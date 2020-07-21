
package io.automatik.engine.addons.process.management.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import io.automatik.engine.api.workflow.workitem.InvalidTransitionException;

@Provider
public class InvalidTransitionExceptionMapper extends BaseExceptionMapper<InvalidTransitionException> {

	@Override
	public Response toResponse(InvalidTransitionException exception) {
		return exceptionsHandler.mapException(exception);
	}
}
