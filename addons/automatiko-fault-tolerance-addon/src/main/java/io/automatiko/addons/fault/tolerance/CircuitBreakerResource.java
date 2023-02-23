package io.automatiko.addons.fault.tolerance;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
