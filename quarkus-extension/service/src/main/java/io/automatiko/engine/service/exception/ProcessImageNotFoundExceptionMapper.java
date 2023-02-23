
package io.automatiko.engine.service.exception;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.ProcessImageNotFoundException;

@Provider
public class ProcessImageNotFoundExceptionMapper extends BaseExceptionMapper<ProcessImageNotFoundException>
        implements ExceptionMapper<ProcessImageNotFoundException> {

    @Override
    public Response toResponse(ProcessImageNotFoundException ex) {
        ProcessImageNotFoundException exception = (ProcessImageNotFoundException) ex;
        Map<String, String> response = new HashMap<>();
        response.put(MESSAGE, exception.getMessage());
        return notFound(response);
    }
}
