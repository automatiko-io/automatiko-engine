
package io.automatiko.engine.service.exception;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.VariableViolationException;

@Provider
public class VariableViolationExceptionMapper extends BaseExceptionMapper<VariableViolationException>
        implements ExceptionMapper<VariableViolationException> {

    @Override
    public Response toResponse(VariableViolationException ex) {
        VariableViolationException exception = (VariableViolationException) ex;
        Map<String, String> response = new HashMap<>();
        response.put(MESSAGE, exception.getMessage() + " : " + exception.getErrorMessage());
        response.put(PROCESS_INSTANCE_ID, exception.getProcessInstanceId());
        response.put(VARIABLE, exception.getVariableName());
        return badRequest(response);
    }
}
