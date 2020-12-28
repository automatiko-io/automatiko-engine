
package io.automatiko.engine.quarkus.exception;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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