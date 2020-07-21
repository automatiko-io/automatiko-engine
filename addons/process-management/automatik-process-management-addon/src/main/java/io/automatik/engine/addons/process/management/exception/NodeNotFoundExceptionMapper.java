
package io.automatik.engine.addons.process.management.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import io.automatik.engine.api.workflow.NodeNotFoundException;

@Provider
public class NodeNotFoundExceptionMapper extends BaseExceptionMapper<NodeNotFoundException> {

	@Override
	public Response toResponse(NodeNotFoundException exception) {
		return exceptionsHandler.mapException(exception);
	}
}
