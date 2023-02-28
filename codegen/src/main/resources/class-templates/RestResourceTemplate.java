package com.myspace.demo;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.WebApplicationException;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.Sig;
import io.automatiko.engine.api.workflow.DefinedProcessErrorException;
import io.automatiko.engine.api.workflow.InstanceMetadata;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatiko.engine.api.workflow.ProcessInstanceNotFoundException;
import io.automatiko.engine.api.workflow.ui.FormProvider;
import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.workflow.Tag;
import io.automatiko.engine.api.workflow.ProcessImageNotFoundException;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.workflow.base.instance.TagInstance;
import io.automatiko.engine.service.auth.HttpAuthSupport;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

@SuppressWarnings({ "rawtypes", "unchecked" })
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name="$processname$", description="$processdocumentation$")
@Path("/$name$")
public class $Type$Resource {

    Process<$Type$> process;
    
    Application application;
    
    IdentitySupplier identitySupplier;
    
    HttpAuthSupport httpAuth = new HttpAuthSupport();
    
    FormProvider formProvider;
    
    @javax.inject.Inject
    public $ResourceType$(Application application, @javax.inject.Named("$id$$version$") Process<$Type$> process, IdentitySupplier identitySupplier, FormProvider formProvider) {
        this.application = application;
        this.process = process;
        this.identitySupplier = identitySupplier;
        this.formProvider = formProvider;
    }

    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "400",
                description = "In case request given does not meet expectations",
                content = @Content(mediaType = "application/json")),
            @APIResponse(
                responseCode = "500",
                description = "In case of processing errors",
                content = @Content(mediaType = "application/json")),
            @APIResponse(
                responseCode = "409",
                description = "In case an instance already exists with given business key",
                content = @Content(mediaType = "application/json")),  
            @APIResponse(
                responseCode = "403",
                description = "In case an instance cannot be created due to access policy by the caller",
                content = @Content(mediaType = "application/json")),            
            @APIResponse(
                responseCode = "200",
                description = "Successfully created instance",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = $Type$Output.class))),            
            @APIResponse(
                    responseCode = "202",
                    description = "Successfully accepted request to create instance (applies only to async execution mode)",
                        content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Creates new instance of $name$")
    @POST()
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)    
    public Response create_$name$(@Context HttpHeaders httpHeaders, @QueryParam("businessKey") @Parameter(description = "Alternative id to be assigned to the instance", required = false) String businessKey, 
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            @Parameter(description = "Indicates if instance metadata should be included", required = false) @QueryParam("metadata") @DefaultValue("false") final boolean metadata,
            $Type$Input resource) {
        if (resource == null) {
            resource = new $Type$Input();
        }
        final $Type$Input value = resource;
        
        String execMode = httpHeaders.getHeaderString("X-ATK-Mode");

        if ("async".equalsIgnoreCase(execMode)) {
            String callbackUrl = httpHeaders.getHeaderString("X-ATK-Callback");
            String startFromNode = httpHeaders.getHeaderString("X-ATK-StartFromNode");
            
            ProcessInstance<$Type$> pi = process.createInstance(businessKey,
                    mapInput(value, new $Type$()));            
            ((AbstractProcessInstance<$Type$>) pi).unlock(true);

            $Type$Output output = mapOutput(new $Type$Output(), pi.variables(), businessKey, pi.metadata());
            
            Map<String, String> headers = httpHeaders.getRequestHeaders().entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().get(0)));
            IdentityProvider identity = identitySupplier.buildIdentityProvider(user, groups);
            IdentityProvider.set(null);
            CompletableFuture.runAsync(() -> {
                IdentityProvider.set(identity);
                io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(),
                        () -> {
                            
                            if (startFromNode != null) {
                                pi.startFrom(startFromNode);
                            } else {
                                pi.start();
                            }
                            tracing(pi);
                            Object result = pi.abortCode() != null ? pi.abortData() : getModel(pi, metadata);

                            io.automatiko.engine.workflow.http.HttpCallbacks.get().post(callbackUrl, result, httpAuth.produce(headers), pi.status());

                            return null;
                        });
            });
               
            ResponseBuilder builder = Response.accepted().entity(output);
            
            return builder.build();
        } else {
        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = process.createInstance(businessKey, mapInput(value, new $Type$()));
                String startFromNode = httpHeaders.getHeaderString("X-ATK-StartFromNode");
                
                if (startFromNode != null) {
                    pi.startFrom(startFromNode);
                } else {
                
                    pi.start();
                }
                tracing(pi);                
                ResponseBuilder builder = Response.ok().entity(getModel(pi, metadata));
                
                return builder.build();
            });
        }
    }
        
    @Operation(hidden = true, 
        summary = "Creates new instance of $name$")
    @GET()
    @Produces(MediaType.TEXT_HTML)        
    public Response create_$name$_form(@Context HttpHeaders httpHeaders,  
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {

        identitySupplier.buildIdentityProvider(user, groups);
        try {
            
            ResponseBuilder builder = Response.ok().entity(formProvider.form(process));
            
            return builder.build();
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
                responseCode = "200",
                description = "Successfully retrieved list of instances",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class, type = SchemaType.ARRAY))) })
    @Operation(
        summary = "Retrieves instances of $name$")
    @GET()
    @Produces(MediaType.APPLICATION_JSON)
    public List<$Type$Output> getAll_$name$(
            @Parameter(description = "Tags to filter when loading instances", required = false) @QueryParam("tags") final List<String> tags,
            @Parameter(description = "Status of the process instance", required = false, schema = @Schema(enumeration = {"active", "completed", "aborted", "error"})) @QueryParam("status") @DefaultValue("active") final String status,
            @Parameter(description = "Pagination - page to start on", required = false) @QueryParam(value = "page") @DefaultValue("1") int page,
            @Parameter(description = "Pagination - number of items to return", required = false) @QueryParam(value = "size") @DefaultValue("10") int size,
            @Parameter(description = "Sorting - name of the field to sort by (description, startDate, endDate, businessKey)", required = false) @QueryParam(value = "sortBy") String sortBy,
            @Parameter(description = "Sorting - direction of sorting ascending or descending", required = false) @QueryParam(value = "sortAsc") @DefaultValue("true") boolean sortAsc,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            @Parameter(description = "Indicates if instance metadata should be included", required = false) @QueryParam("metadata") @DefaultValue("false") final boolean metadata) {
        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            if (tags != null && !tags.isEmpty()) {
                if (sortBy != null && !sortBy.trim().isEmpty()) {
                    return process.instances().findByIdOrTag(io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY, mapStatus(status), sortBy, sortAsc, tags.toArray(String[]::new)).stream()
                            .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), metadata ? pi.metadata() : null))
                            .collect(Collectors.toList());
                } else {
                    return process.instances().findByIdOrTag(io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY, mapStatus(status), tags.toArray(String[]::new)).stream()
                        .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), metadata ? pi.metadata() : null))
                        .collect(Collectors.toList());
                }
            } else {
                if (sortBy != null && !sortBy.trim().isEmpty()) {
                    return process.instances().values(io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY, mapStatus(status), page, size, sortBy, sortAsc).stream()
                            .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), metadata ? pi.metadata() : null))
                            .collect(Collectors.toList());
                } else {
                    return process.instances().values(io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY, mapStatus(status), page, size).stream()
                    .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), metadata ? pi.metadata() : null))
                    .collect(Collectors.toList());
                }
            }
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
                responseCode = "403",
                description = "In case of instance with given id is not accessible to the caller",
                content = @Content(mediaType = "application/json")),            
            @APIResponse(
                responseCode = "200",
                description = "Successfully retrieved instance",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Retrieves $name$ instance with given id")    
    @GET()
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public $Type$Output get_$name$(@PathParam("id") @Parameter(description = "Unique identifier of the instance", required = true) String id,
            @Parameter(description = "Status of the process instance", required = false, schema = @Schema(enumeration = {"active", "completed", "aborted", "error"})) @QueryParam("status") @DefaultValue("active") final String status, 
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            @Parameter(description = "Indicates if instance metadata should be included", required = false) @QueryParam("metadata") @DefaultValue("false") final boolean metadata) {
        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                return process.instances()
                    .findById(id, mapStatus(status), io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY)
                    .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), metadata ? pi.metadata() : null))
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
                responseCode = "403",
                description = "In case of instance with given id is not accessible to the caller",
                content = @Content(mediaType = "application/json")),            
            @APIResponse(
                responseCode = "200",
                description = "Successfully retrieved instance's image",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))) })
    @Operation(hidden = true,
        summary = "Retrieves $name$ instance's image for given id")    
    @GET()
    @Path("/{id}/image")
    @Produces(MediaType.APPLICATION_SVG_XML)
    public Response get_instance_image_$name$(@Context UriInfo uri, @PathParam("id") @Parameter(description = "Unique identifier of the instance", required = true) String id,
            @Parameter(description = "Status of the process instance", required = false, schema = @Schema(enumeration = {"active", "completed", "aborted", "error"})) @QueryParam("status") @DefaultValue("active") final String status,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> instance = process.instances()
                .findById(id, mapStatus(status), io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY)
                .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            String image = instance.image(extractImageBaseUri(uri.getRequestUri().toString()));
            
            if (image == null) {
                throw new ProcessImageNotFoundException(process.id());
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
    @Path("/image")
    @Produces(MediaType.APPLICATION_SVG_XML)
    public Response get_image_$name$() {
        String image = process.image();
        
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
                responseCode = "403",
                description = "In case of instance with given id is not accessible to the caller",
                content = @Content(mediaType = "application/json")),            
            @APIResponse(
                responseCode = "200",
                description = "Successfully deleted instance",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Deletes $name$ instance with given id") 
    @DELETE()
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public $Type$Output delete_$name$(@PathParam("id") @Parameter(description = "Unique identifier of the instance", required = true) final String id,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            @Parameter(description = "Indicates if instance metadata should be included", required = false) @QueryParam("metadata") @DefaultValue("false") final boolean metadata) {
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {  
            ProcessInstance<$Type$> pi = process.instances().findById(id, ProcessInstance.STATE_ACTIVE, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK).orElse(null);
            if (pi == null) {
                pi = process.instances().findById(id, ProcessInstance.STATE_ERROR, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            }
            tracing(pi);
            pi.abort();            
            return getModel(pi, metadata);
            
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
                responseCode = "403",
                description = "In case of instance with given id is not accessible to the caller",
                content = @Content(mediaType = "application/json")),            
            @APIResponse(
                responseCode = "200",
                description = "Successfully updated instance",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Updates data of $name$ instance with given id") 
    @POST()
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateModel_$name$(@Context HttpHeaders httpHeaders, @PathParam("id") @Parameter(description = "Unique identifier of the instance", required = true) String id,
            @Parameter(description = "Status of the process instance", required = false, schema = @Schema(enumeration = {"active", "error"})) @QueryParam("status") @DefaultValue("active") final String status,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            @Parameter(description = "Indicates if instance metadata should be included", required = false) @QueryParam("metadata") @DefaultValue("false") final boolean metadata,
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
                    ProcessInstance<$Type$> pi = process.instances()
                            .findById(id, mapStatus(status), io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK)
                            .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                    tracing(pi);
                    pi.updateVariables(resource);
                    Object result = pi.abortCode() != null ? pi.abortData() : mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), metadata ? pi.metadata() : null);
                    
                    io.automatiko.engine.workflow.http.HttpCallbacks.get().post(callbackUrl, result, httpAuth.produce(headers), pi.status());

                    return null;
                });
  
            });
               
            ResponseBuilder builder = Response.accepted().entity(Collections.singletonMap("id", id));
            
            return builder.build();
        } else {
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = process.instances()
                        .findById(id, mapStatus(status), io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK)
                        .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                tracing(pi);
                pi.updateVariables(resource);
                
                ResponseBuilder builder = Response.ok().entity(pi.abortCode() != null ? pi.abortData() : mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), metadata ? pi.metadata() : null));
                
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
                responseCode = "403",
                description = "In case of instance with given id is not accessible to the caller",
                content = @Content(mediaType = "application/json")),            
            @APIResponse(
                responseCode = "200",
                description = "Successfully retrieved task of the instance",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WorkItem.Descriptor.class, type = SchemaType.ARRAY))) })
    @Operation(
        summary = "Retrieves tasks currently active in $name$ instance with given id") 
    @GET()
    @Path("/{id}/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    public java.util.List<WorkItem.Descriptor> getTasks_$name$(@PathParam("id") @Parameter(description = "Unique identifier of the instance", required = true) String id, 
            @Parameter(description = "User identifier that the tasks should be fetched for", required = false) @QueryParam("user") final String user, 
            @Parameter(description = "Groups that the tasks should be fetched for", required = false) @QueryParam("group") final List<String> groups) {
        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                return process.instances()
                    .findById(id, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY)
                    .map(pi -> pi.workItems(policies(user, groups)))
                    .map(l -> l.stream().map(WorkItem::toMap).collect(Collectors.toList()))
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
                responseCode = "403",
                description = "In case of instance with given id is not accessible to the caller",
                content = @Content(mediaType = "application/json")),            
            @APIResponse(
                responseCode = "200",
                description = "Successfully retrieved tags of the instance",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TagInstance.class, type = SchemaType.ARRAY))) })
    @Operation(
        summary = "Retrieves tags associated with $name$ instance with given id") 
    @GET()
    @Path("/{id}/tags")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<? extends Tag> get_tags_$name$(@PathParam("id") @Parameter(description = "Unique identifier of the instance", required = true) String id,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                return process.instances()
                    .findById(id, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY)
                    .map(pi -> pi.tags().get())
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
                responseCode = "403",
                description = "In case of instance with given id is not accessible to the caller",
                content = @Content(mediaType = "application/json")),
            @APIResponse(
                responseCode = "200",
                description = "Successfully added TagInstance to the instance",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TagInstance.class, type = SchemaType.ARRAY))) })
    @Operation(
        summary = "Adds new TagInstance to $name$ instance with given id")    
    @POST()
    @Path("/{id}/tags")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<? extends Tag> add_tag_$name$(@PathParam("id") @Parameter(description = "Unique identifier of the instance", required = true) String id,             
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            TagInstance resource) {
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances()
                    .findById(id, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK)
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            tracing(pi);
            pi.tags().add(resource.getValue());
            return pi.tags().get();
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
                    responseCode = "403",
                    description = "In case of instance with given id is not accessible to the caller",
                    content = @Content(mediaType = "application/json")),              
            @APIResponse(
                responseCode = "200",
                description = "Successfully removed TagInstance from the instance",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TagInstance.class, type = SchemaType.ARRAY))) })
    @Operation(
            summary = "Removes TagInstance from $name$ instance with given id")     
    @DELETE()
    @Path("/{id}/tags/{tagId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<? extends Tag> get_tag_$name$(@PathParam("id") @Parameter(description = "Unique identifier of the instance", required = true) String id, 
            @Parameter(description = "TagInstance to be removed", required = true) @PathParam("tagId") String tagId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances()
                    .findById(id, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK)
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            tracing(pi);
            pi.tags().remove(tagId);
            return pi.tags().get();
        });
    }
    
    protected $Type$Output getModel(ProcessInstance<$Type$> pi, boolean metadata) {
        if (pi.status() == ProcessInstance.STATE_ERROR && pi.errors().isPresent()) {
            throw new ProcessInstanceExecutionException(pi.id(), pi.errors().get().failedNodeIds(), pi.errors().get().errorMessages());
        }
        if (pi.abortCode() != null) {
            throw new DefinedProcessErrorException(pi.id(), pi.abortCode(), pi.abortData());            
        }
        
        return mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), metadata ? pi.metadata() : null);
    }
    
    protected Policy[] policies(String user, List<String> groups) {         
        return new Policy[] {SecurityPolicy.of(io.automatiko.engine.api.auth.IdentityProvider.get())};
    }
    
    protected $Type$ mapInput($Type$Input input, $Type$ resource) {
        resource.fromMap(input.toMap());
        
        return resource;
    }
    
    protected $Type$Output mapOutput($Type$Output output, $Type$ resource, String businessKey, InstanceMetadata metadata) {
        output.fromMap(businessKey != null ? businessKey: resource.getId(), resource.toMap());
        
        if (metadata != null) {
            output.setMetadata(metadata);
        }
        return output;
    }
    
    protected String extractImageBaseUri(String requestUri) {
        return requestUri.substring(0, requestUri.indexOf("/image"));
    }
    
    protected void tracing(ProcessInstance<?> intance) {
        
    }
    
    protected int mapStatus(String status) {
        int state = 1;
        switch (status.toLowerCase()) {
            case "active":
                state = 1;
                break;
            case "completed":
                state = 2;
                break;
            case "aborted":
                state = 3;
                break;
            case "error":
                state = 5;
                break;                
            default:
                break;
        }
        return state;
    }
}
