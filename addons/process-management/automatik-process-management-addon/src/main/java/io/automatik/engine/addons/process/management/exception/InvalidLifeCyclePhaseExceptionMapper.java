
package io.automatik.engine.addons.process.management.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import io.automatik.engine.api.workflow.workitem.InvalidLifeCyclePhaseException;

@Provider
public class InvalidLifeCyclePhaseExceptionMapper extends BaseExceptionMapper<InvalidLifeCyclePhaseException> {

	@Override
	public Response toResponse(InvalidLifeCyclePhaseException exception) {
		return exceptionsHandler.mapException(exception);
	}
}
