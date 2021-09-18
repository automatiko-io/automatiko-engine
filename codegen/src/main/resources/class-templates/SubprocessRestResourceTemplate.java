package com.myspace.demo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessImageNotFoundException;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.workflow.Sig;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class $Type$Resource {

    Process<$Type$> subprocess_$name$;

    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "500",
                description = "In case of processing errors",
                content = @Content(mediaType = "application/json")),              
            @APIResponse(
                responseCode = "200",
                description = "Successfully retrieved list of instances",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class, type = SchemaType.ARRAY))) })
    @Operation(
        summary = "Retrieves instances of $name$")
    @GET()
    @Path("$prefix$/$name$")
    @Produces(MediaType.APPLICATION_JSON)
    public List<$Type$Output> getAll_$name$(@PathParam("id") String id,
            @Parameter(description = "Status of the process instance", required = false, schema = @Schema(enumeration = {"active", "completed", "aborted", "error"})) @QueryParam("status") @DefaultValue("active") final String status,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
                
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            	ProcessInstance parent = $parentprocess$.instances()
                        .findById($parentparentprocessid$$parentprocessid$, mapStatus(status), io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY)
                        .orElse(null);
                if (parent != null) {
            	
                	return (List<$Type$Output>) parent.subprocesses().stream()
                        .map(pi -> mapOutput(new $Type$Output(), ($Type$) ((ProcessInstance)pi).variables(), ((ProcessInstance)pi).businessKey()))
                        .collect(java.util.stream.Collectors.toList());
                } else {
                	return java.util.Collections.emptyList();
                }
            });
    }
    
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "404",
                description = "In case of image does not exist for $name$",
                content = @Content(mediaType = "application/json")),           
            @APIResponse(
                responseCode = "200",
                description = "Successfully retrieved instance's image",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))) })
    @Operation(hidden = true,
        summary = "Retrieves image for $name$")    
    @GET()
    @Path("$prefix$/$name$/image")
    @Produces(MediaType.APPLICATION_SVG_XML)
    public Response get_image_$name$() {
        String image = subprocess_$name$.image();
        
        if (image == null) {
            throw new ProcessImageNotFoundException(process.id());
        } 
        ResponseBuilder builder = Response.ok().entity(image);
        
        return builder
                .header("Content-Type", "image/svg+xml")
                .build();
    }

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
                description = "Successfully retrieved instance",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Retrieves $name$ instance with given id")      
    @GET()
    @Path("$prefix$/$name$/{id_$name$}")
    @Produces(MediaType.APPLICATION_JSON)
    public $Type$Output get_$name$(@PathParam("id") String id, @PathParam("id_$name$") String id_$name$,
            @Parameter(description = "Status of the process instance", required = false, schema = @Schema(enumeration = {"active", "completed", "aborted", "error"})) @QueryParam("status") @DefaultValue("active") final String status,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            return subprocess_$name$.instances()
                .findById($parentprocessid$ + ":" + id_$name$, mapStatus(status), io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY)
                .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey()))
                .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
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
                description = "In case of instance with given id was not found",
                content = @Content(mediaType = "application/json")),              
            @APIResponse(
                responseCode = "200",
                description = "Successfully retrieved instance",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))) })
    @Operation(hidden = true,
        summary = "Retrieves $name$ instance's image for given id")      
    @GET()
    @Path("$prefix$/$name$/{id_$name$}/image")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get_instance_image_$name$(@Context UriInfo uri, @PathParam("id") String id, @PathParam("id_$name$") String id_$name$,
            @Parameter(description = "Status of the process instance", required = false, schema = @Schema(enumeration = {"active", "completed", "aborted", "error"})) @QueryParam("status") @DefaultValue("active") final String status,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        
        
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> instance =  subprocess_$name$.instances()
                .findById($parentprocessid$ + ":" + id_$name$, mapStatus(status), io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY)
                .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            
            String image = instance.image(extractImageBaseUri(uri.getRequestUri().toString()));
            
            if (image == null) {
                throw new ProcessImageNotFoundException(subprocess_$name$.id());
            }  
            ResponseBuilder builder = Response.ok().entity(image);
            
            return builder
                    .header("Content-Type", "image/svg+xml")
                    .build();
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
                description = "In case of instance with given id was not found",
                content = @Content(mediaType = "application/json")),              
            @APIResponse(
                responseCode = "200",
                description = "Successfully deleted instance",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Deletes $name$ instance with given id")     
    @DELETE()
    @Path("$prefix$/$name$/{id_$name$}")
    @Produces(MediaType.APPLICATION_JSON)
    public $Type$Output delete_$name$(@PathParam("id") String id, @PathParam("id_$name$") final String id_$name$,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {       
            ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$, ProcessInstance.STATE_ACTIVE, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE).orElse(null);
            if (pi == null) {
                pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$, ProcessInstance.STATE_ERROR, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            }
            tracing(pi);
            pi.abort();
            return getSubModel_$name$(pi);
            
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
                description = "In case of instance with given id was not found",
                content = @Content(mediaType = "application/json")),              
            @APIResponse(
                responseCode = "200",
                description = "Successfully updated instance",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Updates data of $name$ instance with given id")     
    @POST()
    @Path("$prefix$/$name$/{id_$name$}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateModel_$name$(@Context HttpHeaders httpHeaders, @PathParam("id") String id, @PathParam("id_$name$") String id_$name$, 
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            $Type$ resource) {
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
                    ProcessInstance<$Type$> pi = subprocess_$name$.instances()
                            .findById($parentprocessid$ + ":" + id_$name$)
                            .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                    tracing(pi);
                    pi.updateVariables(resource);
                    $Type$Output result = mapOutput(new $Type$Output(), pi.variables(), pi.businessKey());
                    
                    io.automatiko.engine.workflow.http.HttpCallbacks.get().post(callbackUrl, result, httpAuth.produce(headers), pi.status());

                    return null;
                });
  
            });
               
            ResponseBuilder builder = Response.accepted().entity(Collections.singletonMap("id", id));
            
            return builder.build();
        } else {
        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = subprocess_$name$.instances()
                        .findById($parentprocessid$ + ":" + id_$name$)
                        .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                tracing(pi);
                pi.updateVariables(resource);
                
                ResponseBuilder builder = Response.ok().entity(mapOutput(new $Type$Output(), pi.variables(), pi.businessKey()));
                
                return builder.build();
    
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
                description = "In case of instance with given id was not found",
                content = @Content(mediaType = "application/json")),              
            @APIResponse(
                responseCode = "200",
                description = "Successfully retrieved task of the instance",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WorkItem.Descriptor.class, type = SchemaType.ARRAY))) })
    @Operation(
        summary = "Retrieves tasks currently active in $name$ instance with given id")     
    @GET()
    @Path("$prefix$/$name$/{id_$name$}/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    public java.util.List<WorkItem.Descriptor> getTasks_$name$(@PathParam("id") String id, @PathParam("id_$name$") String id_$name$, @QueryParam("user") final String user, @QueryParam("group") final List<String> groups) {
        
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            return subprocess_$name$.instances()
                .findById($parentprocessid$ + ":" + id_$name$, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY)
                .map(pi -> pi.workItems(policies(user, groups)))
                .map(l -> l.stream().map(WorkItem::toMap).collect(Collectors.toList()))
                .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
        });
    }
    
    protected $Type$Output getSubModel_$name$(ProcessInstance<$Type$> pi) {
        if (pi.status() == ProcessInstance.STATE_ERROR && pi.errors().isPresent()) {
            throw new ProcessInstanceExecutionException(pi.id(), pi.errors().get().failedNodeIds(), pi.errors().get().errorMessages());
        }
        
        return mapOutput(new $Type$Output(), pi.variables(), pi.businessKey());
    }

    
    protected $Type$ mapInput($Type$Input input, $Type$ resource) {
        resource.fromMap(input.toMap());
        
        return resource;
    }
    
    protected $Type$Output mapOutput($Type$Output output, $Type$ resource, String businessKey) {
        output.fromMap(businessKey != null ? businessKey: resource.getId(), resource.toMap());
        
        return output;
    }
}
