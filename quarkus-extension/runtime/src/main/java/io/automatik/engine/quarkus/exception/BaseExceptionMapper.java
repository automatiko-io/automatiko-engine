
package io.automatik.engine.quarkus.exception;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public abstract class BaseExceptionMapper<E extends Throwable> {

    public static final String MESSAGE = "message";
    public static final String PROCESS_INSTANCE_ID = "processInstanceId";
    public static final String VARIABLE = "variable";
    public static final String NODE_INSTANCE_ID = "nodeInstanceId";
    public static final String NODE_ID = "nodeId";
    public static final String FAILED_NODE_ID = "failedNodeId";
    public static final String ID = "id";

    protected <R> Response badRequest(R body) {
        return Response.status(Response.Status.BAD_REQUEST).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .entity(body).build();
    }

    protected <R> Response conflict(R body) {
        return Response.status(Response.Status.CONFLICT).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .entity(body).build();
    }

    protected <R> Response internalError(R body) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).entity(body).build();
    }

    protected <R> Response notFound(R body) {
        return Response.status(Response.Status.NOT_FOUND).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .entity(body).build();
    }

    protected <R> Response forbidden(R body) {
        return Response.status(Response.Status.FORBIDDEN).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .entity(body).build();
    }
}
