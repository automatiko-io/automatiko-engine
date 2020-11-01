
package io.automatik.engine.quarkus.exception;

import java.util.Collections;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.automatik.engine.api.auth.AccessDeniedException;

@Provider
public class AccessDeniedExceptionMapper extends BaseExceptionMapper<AccessDeniedException>
        implements ExceptionMapper<AccessDeniedException> {

    @Override
    public Response toResponse(AccessDeniedException exception) {
        return forbidden(Collections.singletonMap(MESSAGE, exception.getMessage()));
    }
}
