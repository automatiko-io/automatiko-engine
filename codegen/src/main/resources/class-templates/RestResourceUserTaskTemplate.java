package com.myspace.demo;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import io.automatiko.engine.api.auth.IdentityProvider;
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
                responseCode = "403",
                description = "In case of instance with given id is not accessible to the caller",
                content = @Content(mediaType = "application/json")),            
            @APIResponse(
                responseCode = "200",
                description = "Successfully created new instance of $taskName$ task",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Adds new $taskName$ task instance")  
    @POST
    @Path("/{id}/$taskName$")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "Creating new $taskName$ task", description = "Number of $taskName$ tasks created")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "Duration of creatingnew $taskName$ task", description = "A measure of how long it takes to create $taskName$ tasks.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of creating $taskName$ tasks", description="Rate of creating $taskName$ tasks")   
    public javax.ws.rs.core.Response signal(@PathParam("id") @Parameter(description = "Unique identifier of the owning instance", required = true) final String id,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances().findById(id).orElseThrow(() -> new ProcessInstanceNotFoundException(id));

            pi.send(Sig.of("$taskNodeName$", java.util.Collections.emptyMap()));
            java.util.Optional<WorkItem> task = pi.workItems().stream().filter(wi -> wi.getName().equals("$taskName$")).findFirst();
            if(task.isPresent()) {
                return javax.ws.rs.core.Response.ok(getModel(pi))
                        .header("Link", "</" + id + "/$taskName$/" + task.get().getId() + ">; rel='instance'")
                        .build();
            }
            return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
        });
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
                    responseCode = "403",
                    description = "In case of instance with given id is not accessible to the caller",
                    content = @Content(mediaType = "application/json")),                
                @APIResponse(
                    responseCode = "200",
                    description = "Successfully completed instance of $taskName$ task with given id",
                        content = @Content(mediaType = "application/json",
                                schema = @Schema(implementation = $Type$Output.class))) })
        @Operation(
            summary = "Completes $taskName$ task instance with given id")  
    @POST()
    @Path("/{id}/$taskName$/{workItemId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "Completed $taskName$ tasks", description = "Number of $taskName$ tasks completed")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "Duration of completing $taskName$ task", description = "A measure of how long it takes to complete $taskName$ task.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of completing $taskName$ tasks", description="Rate of completing $taskName$ tasks")       
    public $Type$Output completeTask(@PathParam("id") @Parameter(description = "Unique identifier of the owning instance", required = true) final String id, 
            @Parameter(description = "Unique identifier of the task instance", required = true) @PathParam("workItemId") final String workItemId, 
            @Parameter(description = "Optional phase to be used when aborting", required = false) @QueryParam("phase") @DefaultValue("complete") final String phase, 
            @Parameter(description = "User identifier that the tasks should be completed with", required = false) @QueryParam("user") final String user, 
            @Parameter(description = "Groups that the tasks should be completed with", required = false) @QueryParam("group") final List<String> groups, final $TaskOutput$ model) {
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = process.instances().findById(id).orElseThrow(() -> new ProcessInstanceNotFoundException(id));

                
                io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition transition = new io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition(phase, model.toMap(), io.automatiko.engine.api.auth.IdentityProvider.get());
                pi.transitionWorkItem(workItemId, transition);

                return getModel(pi);
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
                    responseCode = "403",
                    description = "In case of instance with given id is not accessible to the caller",
                    content = @Content(mediaType = "application/json")),                
                @APIResponse(
                    responseCode = "200",
                    description = "Successfully retrieved instance of $taskName$ task with given id",
                        content = @Content(mediaType = "application/json",
                                schema = @Schema(implementation = $TaskInput$.class))) })
        @Operation(
            summary = "Retrieves $taskName$ task instance with given id")  
    @GET()
    @Path("/{id}/$taskName$/{workItemId}")
    @Produces(MediaType.APPLICATION_JSON)
    public $TaskInput$ getTask(@PathParam("id") @Parameter(description = "Unique identifier of the owning instance", required = true) String id, 
            @Parameter(description = "Unique identifier of the task instance", required = true) @PathParam("workItemId") String workItemId, 
            @Parameter(description = "User identifier that the tasks should be retrieved for", required = false) @QueryParam("user") final String user, 
            @Parameter(description = "Groups that the tasks should be retrieved for", required = false) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        try {
            ProcessInstance<$Type$> pi = process.instances().findById(id, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            WorkItem workItem = pi.workItem(workItemId, policies(user, groups));
            if (workItem == null) {
                return null;
            }
            return $TaskInput$.fromMap(workItem.getId(), workItem.getName(), workItem.getParameters());
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
                responseCode = "403",
                description = "In case of instance with given id is not accessible to the caller",
                content = @Content(mediaType = "application/json")),            
            @APIResponse(
                responseCode = "200",
                description = "Successfully aborted instance of $taskName$ task with given id",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Aborts $taskName$ task instance with given id")  
    @DELETE()
    @Path("/{id}/$taskName$/{workItemId}")
    @Produces(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "Aborted $taskName$ tasks", description = "Number of $taskName$ tasks aborted")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "Duration of aborting $taskName$ task", description = "A measure of how long it takes to abort $taskName$ task.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of aborting $taskName$ tasks", description="Rate of aborting $taskName$ tasks")           
    public $Type$Output abortTask(@PathParam("id") @Parameter(description = "Unique identifier of the owning instance", required = true) final String id, 
            @Parameter(description = "Unique identifier of the task instance", required = true) @PathParam("workItemId") final String workItemId, 
            @Parameter(description = "Optional phase to be used when aborting", required = false) @QueryParam("phase") @DefaultValue("abort") final String phase, 
            @Parameter(description = "User identifier that the tasks should be aborted with", required = false) @QueryParam("user") final String user, 
            @Parameter(description = "Groups that the tasks should be aborted with", required = false) @QueryParam("group") final List<String> groups) {
        
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = process.instances().findById(id).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                                
                io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition transition = new io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition(phase, null, io.automatiko.engine.api.auth.IdentityProvider.get());
                pi.transitionWorkItem(workItemId, transition);

                return getModel(pi);
            });
        } catch (WorkItemNotFoundException e) {
            return null;
        }
    }
}
