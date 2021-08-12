
package io.automatiko.engine.service.exception;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ResourceNotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException>
        implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException ex) {

        return notFound(ex.getMessage());
    }
}
