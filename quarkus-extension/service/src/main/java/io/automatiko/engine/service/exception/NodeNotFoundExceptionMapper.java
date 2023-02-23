
package io.automatiko.engine.service.exception;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.NodeNotFoundException;

@Provider
public class NodeNotFoundExceptionMapper extends BaseExceptionMapper<NodeNotFoundException>
        implements ExceptionMapper<NodeNotFoundException> {

    @Override
    public Response toResponse(NodeNotFoundException ex) {
        NodeNotFoundException exception = (NodeNotFoundException) ex;
        Map<String, String> response = new HashMap<>();
        response.put(MESSAGE, exception.getMessage());
        response.put(PROCESS_INSTANCE_ID, exception.getProcessInstanceId());
        response.put(NODE_ID, exception.getNodeId());
        return notFound(response);
    }
}
