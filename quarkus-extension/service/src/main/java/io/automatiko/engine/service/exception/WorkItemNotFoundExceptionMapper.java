
package io.automatiko.engine.service.exception;

import java.util.Collections;

import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class WorkItemNotFoundExceptionMapper extends BaseExceptionMapper<WorkItemNotFoundException>
        implements ExceptionMapper<WorkItemNotFoundException> {

    @Override
    public Response toResponse(WorkItemNotFoundException ex) {

        return notFound(Collections.singletonMap(MESSAGE, ex.getMessage()));
    }
}
