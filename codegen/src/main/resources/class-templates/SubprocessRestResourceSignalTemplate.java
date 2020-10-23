
package com.myspace.demo;

import java.util.List;

import io.automatik.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.workflow.Sig;

public class $Type$Resource {


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
                    responseCode = "200",
                    description = "Successfully aborted instance of $taskName$ task with given id",
                        content = @Content(mediaType = "application/json",
                                schema = @Schema(implementation = $Type$Output.class))) })
        @Operation(
            summary = "Signals '$signalName$' on instance with given id")  
    @POST
    @Path("$prefix$/$name$/{id_$name$}/$signalPath$")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "Trigger on $name$ with signal '$signalName$'", description = "Number of instances of $name$ triggered with signal '$signalName$'")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "Duration of triggering $name$ instance with signal '$signalName$'", description = "A measure of how long it takes to trigger instance of $name$ with signal '$signalName$'.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of triggering instances of $name$ with signal '$signalName$'", description="Rate of triggering instances of $name$ with signal '$signalName$'")   
    public $Type$Output signal(@PathParam("id") String id, @PathParam("id_$name$") String id_$name$, final $signalType$ data) {
        return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            pi.send(Sig.of("$signalName$", data));
            return getModel(pi);
        });
    }

}
