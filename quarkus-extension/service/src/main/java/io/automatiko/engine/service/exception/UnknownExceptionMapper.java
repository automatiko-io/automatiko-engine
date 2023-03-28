
package io.automatiko.engine.service.exception;

import java.util.Collections;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class UnknownExceptionMapper extends BaseExceptionMapper<Exception>
        implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnknownExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        LOGGER.error("Unexpected error processing request", exception);
        return internalError(Collections.singletonMap(MESSAGE, exception.getMessage()));
    }
}
