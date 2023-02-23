
package io.automatiko.engine.service.exception;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;

@Provider
public class ProcessInstanceDuplicatedExceptionMapper extends BaseExceptionMapper<ProcessInstanceDuplicatedException>
        implements ExceptionMapper<ProcessInstanceDuplicatedException> {

    @Override
    public Response toResponse(ProcessInstanceDuplicatedException ex) {
        ProcessInstanceDuplicatedException exception = (ProcessInstanceDuplicatedException) ex;
        Map<String, String> response = new HashMap<>();
        response.put(MESSAGE, exception.getMessage());
        response.put(PROCESS_INSTANCE_ID, exception.getProcessInstanceId());
        return conflict(response);
    }
}
