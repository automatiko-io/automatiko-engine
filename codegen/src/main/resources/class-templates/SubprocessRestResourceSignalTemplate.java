
package com.myspace.demo;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.Sig;

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
    public Response signal(@Context HttpHeaders httpHeaders, @PathParam("id") String id, @PathParam("id_$name$") String id_$name$, 
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            final $signalType$ data) {
        String execMode = httpHeaders.getHeaderString("X-ATK-Mode");

        if ("async".equalsIgnoreCase(execMode)) {
            String callbackUrl = httpHeaders.getHeaderString("X-ATK-Callback");
            Map<String, String> headers = httpHeaders.getRequestHeaders().entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().get(0)));
            
            IdentityProvider identity = identitySupplier.buildIdentityProvider(user, groups);
            IdentityProvider.set(null);
            
            CompletableFuture.runAsync(() -> {
                IdentityProvider.set(identity);
                io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                    ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                    tracing(pi);
                    pi.send(Sig.of("$signalName$", data));
                    
                    io.automatiko.engine.workflow.http.HttpCallbacks.get().post(callbackUrl, getSubModel_$name$(pi), httpAuth.produce(headers), pi.status());

                    return null;
                });
  
            });
               
            ResponseBuilder builder = Response.accepted().entity(Collections.singletonMap("id", id));
            
            return builder.build();
        } else {
        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                tracing(pi);
                pi.send(Sig.of("$signalName$", data));

                ResponseBuilder builder = Response.ok().entity(getSubModel_$name$(pi));
                
                return builder.build();
            });
        }
    }

}
