
package io.automatik.engine.addons.process.management.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import io.automatik.engine.api.workflow.ProcessInstanceExecutionException;

@Provider
public class ProcessInstanceExecutionExceptionMapper extends BaseExceptionMapper<ProcessInstanceExecutionException> {

	@Override
	public Response toResponse(ProcessInstanceExecutionException exception) {
		return exceptionsHandler.mapException(exception);
	}
}
