
package io.automatiko.engine.service.exception;

import java.util.Collections;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.workitem.InvalidTransitionException;

@Provider
public class InvalidTransitionExceptionMapper extends BaseExceptionMapper<InvalidTransitionException>
        implements ExceptionMapper<InvalidTransitionException> {

    @Override
    public Response toResponse(InvalidTransitionException exception) {
        return badRequest(Collections.singletonMap(MESSAGE, exception.getMessage()));
    }
}
