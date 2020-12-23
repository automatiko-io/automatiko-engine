
package io.automatiko.engine.quarkus.exception;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
