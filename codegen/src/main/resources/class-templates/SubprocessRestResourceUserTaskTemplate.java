package com.myspace.demo;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.WorkItem;
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
                description = "In case of task instance with given id was not found",
                content = @Content(mediaType = "application/json")),              
            @APIResponse(
                responseCode = "200",
                description = "Successfully created new instance of $taskName$ task",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Adds new $taskName$ task instance")  
    @POST
    @Path("$prefix$/$name$/{id_$name$}/$taskName$")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response signal(@Context HttpHeaders httpHeaders, @PathParam("id") String id, @PathParam("id_$name$") String id_$name$,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            @Parameter(description = "Indicates if instance metadata should be included", required = false) @QueryParam("metadata") @DefaultValue("false") final boolean metadata) {
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
                    String combinedId;
                    if (id_$name$.contains(":")) {
                        combinedId = id_$name$;
                    } else {
                        combinedId = $parentprocessid$ + ":" + id_$name$;
                    }
                    ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById(combinedId).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                    tracing(pi);
                    pi.send(Sig.of("$taskNodeName$", java.util.Collections.emptyMap()));
                    
                    io.automatiko.engine.workflow.http.HttpCallbacks.get().post(callbackUrl, getSubModel_$name$(pi, metadata), httpAuth.produce(headers), pi.status());

                    return null;
                });
  
            });
               
            ResponseBuilder builder = Response.accepted().entity(Collections.singletonMap("id", id));
            
            return builder.build();
        } else {
        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                String combinedId;
                if (id_$name$.contains(":")) {
                    combinedId = id_$name$;
                } else {
                    combinedId = $parentprocessid$ + ":" + id_$name$;
                }
                ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById(combinedId).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                tracing(pi);
                pi.send(Sig.of("$taskNodeName$", java.util.Collections.emptyMap()));
                java.util.Optional<WorkItem> task = pi.workItems().stream().filter(wi -> wi.getName().equals("$taskName$")).findFirst();
                if(task.isPresent()) {
                    return javax.ws.rs.core.Response.ok(getSubModel_$name$(pi, metadata))
                            .header("Link", "</" + id + "/$taskName$/" + task.get().getId() + ">; rel='instance'")
                            .build();
                }
                return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
            });
        }
    }

    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "500",
                description = "In case of processing errors",
                content = @Content(mediaType = "application/json")), 
            @APIResponse(
                responseCode = "404",
                description = "In case of task instance with given id was not found",
                content = @Content(mediaType = "application/json")),              
            @APIResponse(
                responseCode = "200",
                description = "Successfully completed instance of $taskName$ task with given id",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Completes $taskName$ task instance with given id")     
    @POST()
    @Path("$prefix$/$name$/{id_$name$}/$taskName$/{workItemId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response completeTask(@Context HttpHeaders httpHeaders, @PathParam("id") String id, @PathParam("id_$name$") String id_$name$, @PathParam("workItemId") final String workItemId,
            @QueryParam("phase") @DefaultValue("complete") final String phase, @QueryParam("user") final String user, @QueryParam("group") final List<String> groups, 
            @QueryParam("metadata") @DefaultValue("false") final boolean metadata,
            final $TaskOutput$ model) {
        try {
            
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
                        String combinedId;
                        if (id_$name$.contains(":")) {
                            combinedId = id_$name$;
                        } else {
                            combinedId = $parentprocessid$ + ":" + id_$name$;
                        }
                        ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById(combinedId).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                        tracing(pi);
                        io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition transition = new io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition(phase, model.toMap(), io.automatiko.engine.api.auth.IdentityProvider.get());
                        pi.transitionWorkItem(workItemId, transition);
                        
                        io.automatiko.engine.workflow.http.HttpCallbacks.get().post(callbackUrl, getSubModel_$name$(pi, metadata), httpAuth.produce(headers), pi.status());

                        return null;
                    });
      
                });
                   
                ResponseBuilder builder = Response.accepted().entity(Collections.singletonMap("id", id));
                
                return builder.build();
            } else {
            
                identitySupplier.buildIdentityProvider(user, groups);
                return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                    String combinedId;
                    if (id_$name$.contains(":")) {
                        combinedId = id_$name$;
                    } else {
                        combinedId = $parentprocessid$ + ":" + id_$name$;
                    }
                    ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById(combinedId).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                    tracing(pi);
                    io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition transition = new io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition(phase, model.toMap(), io.automatiko.engine.api.auth.IdentityProvider.get());
                    pi.transitionWorkItem(workItemId, transition);
    
                    ResponseBuilder builder = Response.ok().entity(getSubModel_$name$(pi, metadata));
                    
                    return builder.build();
                });
            }
        } catch (WorkItemNotFoundException e) {
            return null;
        } finally {
            IdentityProvider.set(null);
        }
    }
    
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "500",
                description = "In case of processing errors",
                content = @Content(mediaType = "application/json")), 
            @APIResponse(
                responseCode = "404",
                description = "In case of task instance with given id was not found",
                content = @Content(mediaType = "application/json")),              
            @APIResponse(
                responseCode = "200",
                description = "Successfully retrieved instance of $taskName$ task with given id",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $TaskInput$.class))) })
    @Operation(
        summary = "Retrieves $taskName$ task instance with given id")     
    @GET()
    @Path("$prefix$/$name$/{id_$name$}/$taskName$/{workItemId}")
    @Produces(MediaType.APPLICATION_JSON)
    public $TaskInput$ getTask(@PathParam("id") String id, @PathParam("id_$name$") String id_$name$, @PathParam("workItemId") String workItemId, @QueryParam("user") final String user, @QueryParam("group") final List<String> groups) {
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                String combinedId;
                if (id_$name$.contains(":")) {
                    combinedId = id_$name$;
                } else {
                    combinedId = $parentprocessid$ + ":" + id_$name$;
                }
                ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById(combinedId, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
    
                WorkItem workItem = pi.workItem(workItemId, policies(user, groups));
                if (workItem == null) {
                    return null;
                }
                return $TaskInput$.fromMap(workItem.getId(), workItem.getName(), workItem.getParameters());
            });
        } catch (WorkItemNotFoundException e) {
            return null;
        } finally {
            IdentityProvider.set(null);
        }
    }
    
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "500",
                description = "In case of processing errors",
                content = @Content(mediaType = "application/json")), 
            @APIResponse(
                responseCode = "404",
                description = "In case of task instance with given id was not found",
                content = @Content(mediaType = "application/json")),              
            @APIResponse(
                responseCode = "200",
                description = "Successfully aborted instance of $taskName$ task with given id",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Aborts $taskName$ task instance with given id")      
    @DELETE()
    @Path("$prefix$/$name$/{id_$name$}/$taskName$/{workItemId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response abortTask(@Context HttpHeaders httpHeaders, @PathParam("id") String id, @PathParam("id_$name$") String id_$name$, @PathParam("workItemId") final String workItemId, 
            @QueryParam("phase") @DefaultValue("abort") final String phase, @QueryParam("user") final String user, @QueryParam("group") final List<String> groups,
            @QueryParam("metadata") @DefaultValue("false") final boolean metadata) {
        String execMode = httpHeaders.getHeaderString("X-ATK-Mode");
        try {
            if ("async".equalsIgnoreCase(execMode)) {
                String callbackUrl = httpHeaders.getHeaderString("X-ATK-Callback");
                Map<String, String> headers = httpHeaders.getRequestHeaders().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().get(0)));
                
                IdentityProvider identity = identitySupplier.buildIdentityProvider(user, groups);
                IdentityProvider.set(null);
                
                CompletableFuture.runAsync(() -> {
                    IdentityProvider.set(identity);
                    io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                        String combinedId;
                        if (id_$name$.contains(":")) {
                            combinedId = id_$name$;
                        } else {
                            combinedId = $parentprocessid$ + ":" + id_$name$;
                        }
                        ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById(combinedId).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                        tracing(pi);
                        io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition transition = new io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition(phase, null, io.automatiko.engine.api.auth.IdentityProvider.get());
                        pi.transitionWorkItem(workItemId, transition);
                        
                        io.automatiko.engine.workflow.http.HttpCallbacks.get().post(callbackUrl, getSubModel_$name$(pi, metadata), httpAuth.produce(headers), pi.status());

                        return null;
                    });
      
                });
                   
                ResponseBuilder builder = Response.accepted().entity(Collections.singletonMap("id", id));
                
                return builder.build();
            } else {
                identitySupplier.buildIdentityProvider(user, groups);
                return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                    String combinedId;
                    if (id_$name$.contains(":")) {
                        combinedId = id_$name$;
                    } else {
                        combinedId = $parentprocessid$ + ":" + id_$name$;
                    }
                    ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById(combinedId).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                    tracing(pi);
                    io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition transition = new io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition(phase, null, io.automatiko.engine.api.auth.IdentityProvider.get());
                    pi.transitionWorkItem(workItemId, transition);
    
                    ResponseBuilder builder = Response.ok().entity(getSubModel_$name$(pi, metadata));
                    
                    return builder.build();
                });
            }
        } catch (WorkItemNotFoundException e) {
            return null;
        }
    }
}
