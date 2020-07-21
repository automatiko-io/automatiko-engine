
package io.automatik.engine.addons.process.management.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import io.automatik.engine.api.workflow.VariableViolationException;

@Provider
public class VariableViolationExceptionMapper extends BaseExceptionMapper<VariableViolationException> {

	@Override
	public Response toResponse(VariableViolationException exception) {
		return exceptionsHandler.mapException(exception);
	}
}
