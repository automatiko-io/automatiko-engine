
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
    public Response signal(@Context HttpHeaders httpHeaders, @PathParam("id") String id, @PathParam("id_$name$") String id_$name$, 
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            @Parameter(description = "Indicates if instance metadata should be included", required = false) @QueryParam("metadata") @DefaultValue("false") final boolean metadata,
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
                    ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                    tracing(pi);
                    pi.send(Sig.of("$signalName$", data));
                    
                    io.automatiko.engine.workflow.http.HttpCallbacks.get().post(callbackUrl, pi.abortCode() != null ? pi.abortData() : getSubModel_$name$(pi, metadata), httpAuth.produce(headers), pi.status());

                    return null;
                });
  
            });
               
            ResponseBuilder builder = Response.accepted().entity(Collections.singletonMap("id", id));
            
            return builder.build();
        } else {
        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                tracing(pi);
                pi.send(Sig.of("$signalName$", data));

                ResponseBuilder builder = Response.ok().entity(getSubModel_$name$(pi, metadata));
                
                return builder.build();
            });
        }
    }

}
