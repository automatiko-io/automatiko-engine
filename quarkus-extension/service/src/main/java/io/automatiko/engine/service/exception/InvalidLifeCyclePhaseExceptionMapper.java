
package io.automatiko.engine.service.exception;

import java.util.Collections;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.workitem.InvalidLifeCyclePhaseException;

@Provider
public class InvalidLifeCyclePhaseExceptionMapper extends BaseExceptionMapper<InvalidLifeCyclePhaseException>
        implements ExceptionMapper<InvalidLifeCyclePhaseException> {

    @Override
    public Response toResponse(InvalidLifeCyclePhaseException exception) {
        return badRequest(Collections.singletonMap(MESSAGE, exception.getMessage()));
    }
}
