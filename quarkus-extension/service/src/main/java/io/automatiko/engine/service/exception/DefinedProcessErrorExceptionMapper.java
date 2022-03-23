
package io.automatiko.engine.service.exception;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.automatiko.engine.api.workflow.DefinedProcessErrorException;

@Provider
public class DefinedProcessErrorExceptionMapper extends BaseExceptionMapper<DefinedProcessErrorException>
        implements ExceptionMapper<DefinedProcessErrorException> {

    @Override
    public Response toResponse(DefinedProcessErrorException ex) {
        int status = 500;

        try {
            status = Integer.parseInt(ex.getErrorCode());
            if (status < 100 || status > 999) {
                // invalid http response code, fallbacks to 500
                status = 500;
            }
        } catch (NumberFormatException e) {

        }

        Response.ResponseBuilder builder = Response.status(status, ex.getMessage())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        if (ex.getError() != null) {
            builder.entity(ex.getError());
        }

        return builder.build();
    }
}
