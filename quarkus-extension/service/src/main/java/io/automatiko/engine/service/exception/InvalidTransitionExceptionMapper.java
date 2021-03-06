
package io.automatiko.engine.service.exception;

import java.util.Collections;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.workitem.InvalidTransitionException;

@Provider
public class InvalidTransitionExceptionMapper extends BaseExceptionMapper<InvalidTransitionException>
        implements ExceptionMapper<InvalidTransitionException> {

    @Override
    public Response toResponse(InvalidTransitionException exception) {
        return badRequest(Collections.singletonMap(MESSAGE, exception.getMessage()));
    }
}
