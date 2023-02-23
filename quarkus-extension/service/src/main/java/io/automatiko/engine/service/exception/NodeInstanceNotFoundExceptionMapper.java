
package io.automatiko.engine.service.exception;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.NodeInstanceNotFoundException;

@Provider
public class NodeInstanceNotFoundExceptionMapper extends BaseExceptionMapper<NodeInstanceNotFoundException>
        implements ExceptionMapper<NodeInstanceNotFoundException> {

    @Override
    public Response toResponse(NodeInstanceNotFoundException ex) {
        NodeInstanceNotFoundException exception = (NodeInstanceNotFoundException) ex;
        Map<String, String> response = new HashMap<>();
        response.put(MESSAGE, exception.getMessage());
        response.put(PROCESS_INSTANCE_ID, exception.getProcessInstanceId());
        response.put(NODE_INSTANCE_ID, exception.getNodeInstanceId());
        return notFound(response);
    }
}
