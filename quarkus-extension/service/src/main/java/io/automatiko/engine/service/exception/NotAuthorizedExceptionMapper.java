
package io.automatiko.engine.service.exception;

import java.util.Collections;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.workitem.NotAuthorizedException;

@Provider
public class NotAuthorizedExceptionMapper extends BaseExceptionMapper<NotAuthorizedException>
        implements ExceptionMapper<NotAuthorizedException> {

    @Override
    public Response toResponse(NotAuthorizedException exception) {
        return forbidden(Collections.singletonMap(MESSAGE, exception.getMessage()));
    }
}
