
package io.automatik.engine.addons.process.management.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import io.automatik.engine.api.workflow.workitem.NotAuthorizedException;

@Provider
public class NotAuthorizedExceptionMapper extends BaseExceptionMapper<NotAuthorizedException> {

	@Override
	public Response toResponse(NotAuthorizedException exception) {
		return exceptionsHandler.mapException(exception);
	}
}
