
package io.automatiko.engine.service.exception;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ResourceNotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException>
        implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException ex) {

        return notFound(ex.getMessage());
    }
}
