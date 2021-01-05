
package io.automatiko.engine.service.exception;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
