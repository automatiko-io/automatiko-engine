
package io.automatiko.engine.service.exception;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.VariableNotFoundException;

@Provider
public class VariableNotFoundExceptionMapper extends BaseExceptionMapper<VariableNotFoundException>
        implements ExceptionMapper<VariableNotFoundException> {

    @Override
    public Response toResponse(VariableNotFoundException ex) {
        VariableNotFoundException exception = (VariableNotFoundException) ex;
        Map<String, String> response = new HashMap<>();
        response.put(MESSAGE, exception.getMessage());
        response.put(PROCESS_INSTANCE_ID, exception.getProcessInstanceId());
        response.put(VARIABLE, exception.getName());
        return notFound(response);
    }
}
