
package io.automatik.engine.quarkus.exception;

import java.util.Collections;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
