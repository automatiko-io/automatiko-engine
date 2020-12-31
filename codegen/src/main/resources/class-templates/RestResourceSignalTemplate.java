
package com.myspace.demo;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.Sig;

public class $Type$Resource {

    Process<$Type$> process;

    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "500",
                description = "In case of processing errors",
                content = @Content(mediaType = "application/json")), 
            @APIResponse(
                responseCode = "404",
                description = "In case of instance with given id was not found",
                content = @Content(mediaType = "application/json")), 
            @APIResponse(
                responseCode = "403",
                description = "In case of instance with given id is not accessible to the caller",
                content = @Content(mediaType = "application/json")),            
            @APIResponse(
                responseCode = "200",
                description = "Successfully aborted instance of $taskName$ task with given id",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Signals '$signalName$' on instance with given id")  
    @POST
    @Path("/{id}/$signalPath$")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "Trigger on $name$ with signal '$signalName$'", description = "Number of instances of $name$ triggered with signal '$signalName$'")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "Duration of triggering $name$ instance with signal '$signalName$'", description = "A measure of how long it takes to trigger instance of $name$. with signal '$signalName$'", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of triggering instances of $name$ with signal '$signalName$'", description="Rate of triggering instances of $name$ with signal '$signalName$'")   
    public $Type$Output signal(@PathParam("id") @Parameter(description = "Unique identifier of the instance", required = true) final String id, 
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            final $signalType$ data) {
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances().findById(id).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            tracing(pi);
            pi.send(Sig.of("$signalName$", data));
            return getModel(pi);
        });
    }

}
