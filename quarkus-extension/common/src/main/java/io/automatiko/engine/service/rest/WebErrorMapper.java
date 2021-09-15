package io.automatiko.engine.service.rest;

import java.util.function.Function;

public class WebErrorMapper implements Function<Throwable, Throwable> {

    public Throwable apply(Throwable error) {
        if (error instanceof javax.ws.rs.WebApplicationException) {
            javax.ws.rs.WebApplicationException wex = (javax.ws.rs.WebApplicationException) error;
            return new io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError(wex.getMessage(),
                    String.valueOf(wex.getResponse().getStatus()),
                    io.automatiko.engine.services.utils.IoUtils.valueOf(wex.getResponse().getEntity()));
        } else if (error instanceof javax.ws.rs.ProcessingException) {
            javax.ws.rs.ProcessingException ex = (javax.ws.rs.ProcessingException) error;
            return new io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError("503", ex.getMessage(), ex);
        }

        return error;
    }
}