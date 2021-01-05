
package io.automatiko.engine.service.exception;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.ProcessInstanceNotFoundException;

@Provider
public class ProcessInstanceNotFoundExceptionMapper extends BaseExceptionMapper<ProcessInstanceNotFoundException>
        implements ExceptionMapper<ProcessInstanceNotFoundException> {

    @Override
    public Response toResponse(ProcessInstanceNotFoundException ex) {
        ProcessInstanceNotFoundException exception = (ProcessInstanceNotFoundException) ex;
        Map<String, String> response = new HashMap<>();
        response.put(MESSAGE, exception.getMessage());
        response.put(PROCESS_INSTANCE_ID, exception.getProcessInstanceId());
        return notFound(response);
    }
}
