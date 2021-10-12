package io.automatiko.addons.fault.tolerance;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Fault Tolerance Management", description = "Fault Tolerance operations on top of the service")
@Path("/management/faults")
public class CircuitBreakerResource {

    CircuitClosedManager manager;

    @Inject
    public CircuitBreakerResource(CircuitClosedManager manager) {
        this.manager = manager;
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "List of circuit breakers", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Lists available circuit breakers in the service")
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<CircuitBrakerDTO> get() {

        return manager.info();
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Accepts the request to process asynchronously", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Retriggers failed instances for given circuit breaker")
    @POST
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(
            @Parameter(description = "Name of the circuit breaker to retrigger", required = true) @PathParam("name") String name) {
        manager.retriggerErroredInstances(name);
        return Response.accepted().build();
    }
}
