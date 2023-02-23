package io.automatiko.engine.service.rest;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import io.automatiko.engine.api.workflow.HandledServiceExecutionError;
import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError;

public class WebErrorMapper implements Function<Throwable, Throwable> {

    private Set<String> handledCodes = new HashSet<>();

    public WebErrorMapper(String... codes) {
        Stream.of(codes).forEach(code -> handledCodes.add(code));
    }

    public Throwable apply(Throwable error) {
        if (error instanceof WorkItemExecutionError) {
            String errorCode = ((WorkItemExecutionError) error).getErrorCode();

            if (handledCodes.contains(errorCode)) {
                return new HandledServiceExecutionError((WorkItemExecutionError) error);
            }
        } else if (error instanceof jakarta.ws.rs.WebApplicationException) {
            jakarta.ws.rs.WebApplicationException wex = (jakarta.ws.rs.WebApplicationException) error;
            return new io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError(wex.getMessage(),
                    String.valueOf(wex.getResponse().getStatus()),
                    io.automatiko.engine.services.utils.IoUtils.valueOf(wex.getResponse().getEntity()));
        } else if (error instanceof jakarta.ws.rs.ProcessingException) {
            jakarta.ws.rs.ProcessingException ex = (jakarta.ws.rs.ProcessingException) error;
            return new io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError("503", ex.getMessage(), ex);
        }

        return error;
    }
}