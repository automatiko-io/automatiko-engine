
package io.automatiko.engine.service.exception;

import java.util.Collections;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.workitem.NotAuthorizedException;

@Provider
public class NotAuthorizedExceptionMapper extends BaseExceptionMapper<NotAuthorizedException>
        implements ExceptionMapper<NotAuthorizedException> {

    @Override
    public Response toResponse(NotAuthorizedException exception) {
        return forbidden(Collections.singletonMap(MESSAGE, exception.getMessage()));
    }
}
