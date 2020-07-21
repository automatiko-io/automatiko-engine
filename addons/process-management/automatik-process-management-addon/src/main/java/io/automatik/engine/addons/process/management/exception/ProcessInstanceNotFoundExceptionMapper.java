
package io.automatik.engine.addons.process.management.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import io.automatik.engine.api.workflow.ProcessInstanceNotFoundException;

@Provider
public class ProcessInstanceNotFoundExceptionMapper extends BaseExceptionMapper<ProcessInstanceNotFoundException> {

	@Override
	public Response toResponse(ProcessInstanceNotFoundException exception) {
		return exceptionsHandler.mapException(exception);
	}
}
