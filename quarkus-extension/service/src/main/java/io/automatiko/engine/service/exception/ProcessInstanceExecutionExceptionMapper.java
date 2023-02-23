
package io.automatiko.engine.service.exception;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;

@Provider
public class ProcessInstanceExecutionExceptionMapper extends BaseExceptionMapper<ProcessInstanceExecutionException>
        implements ExceptionMapper<ProcessInstanceExecutionException> {

    @Override
    public Response toResponse(ProcessInstanceExecutionException ex) {
        ProcessInstanceExecutionException exception = (ProcessInstanceExecutionException) ex;
        Map<String, String> response = new HashMap<>();
        response.put(ID, exception.getProcessInstanceId());
        response.put(FAILED_NODE_ID, exception.getFailedNodeId());
        response.put(MESSAGE, exception.getErrorMessage());
        return internalError(response);
    }
}
