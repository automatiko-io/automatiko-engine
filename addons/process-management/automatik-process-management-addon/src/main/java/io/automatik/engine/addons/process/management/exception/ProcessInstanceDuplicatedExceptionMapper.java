
package io.automatik.engine.addons.process.management.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import io.automatik.engine.api.workflow.ProcessInstanceDuplicatedException;

@Provider
public class ProcessInstanceDuplicatedExceptionMapper extends BaseExceptionMapper<ProcessInstanceDuplicatedException> {

	@Override
	public Response toResponse(ProcessInstanceDuplicatedException exception) {
		return exceptionsHandler.mapException(exception);
	}
}
