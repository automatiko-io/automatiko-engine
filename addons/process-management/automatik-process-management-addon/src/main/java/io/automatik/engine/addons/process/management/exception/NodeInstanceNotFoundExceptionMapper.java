
package io.automatik.engine.addons.process.management.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import io.automatik.engine.api.workflow.NodeInstanceNotFoundException;

@Provider
public class NodeInstanceNotFoundExceptionMapper extends BaseExceptionMapper<NodeInstanceNotFoundException> {

	@Override
	public Response toResponse(NodeInstanceNotFoundException exception) {
		return exceptionsHandler.mapException(exception);
	}
}
