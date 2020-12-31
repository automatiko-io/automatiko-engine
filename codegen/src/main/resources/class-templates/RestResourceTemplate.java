package com.myspace.demo;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
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
import io.automatiko.engine.workflow.Sig;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatiko.engine.api.workflow.ProcessInstanceNotFoundException;
import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.workflow.Tag;
import io.automatiko.engine.api.workflow.ProcessImageNotFoundException;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.workflow.base.instance.TagInstance;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

@org.eclipse.microprofile.openapi.annotations.tags.Tag(name="$processname$", description="$processdocumentation$")
@Path("/$name$")
public class $Type$Resource {

    Process<$Type$> process;
    
    Application application;
    
    IdentitySupplier identitySupplier;

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
                    schema = @Schema(implementation = $Type$Output.class))) })
    @Operation(
        summary = "Creates new instance of $name$")
    @POST()
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "create $name$", description = "Number of new instances of $name$")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "duration of creating $name$", description = "A measure of how long it takes to create new instance of $name$.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of instances of $name$", description="Rate of new instances of $name$")
    public $Type$Output create_$name$(@Context HttpHeaders httpHeaders, @QueryParam("businessKey") @Parameter(description = "Alternative id to be assigned to the instance", required = false) String businessKey, 
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            @Parameter(description = "The input model for $name$ instance") $Type$Input resource) {
        if (resource == null) {
            resource = new $Type$Input();
        }
        final $Type$Input value = resource;
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.createInstance(businessKey, mapInput(value, new $Type$()));
            String startFromNode = httpHeaders.getHeaderString("X-AUTOMATIK-StartFromNode");
            
            if (startFromNode != null) {
                pi.startFrom(startFromNode);
            } else {
            
                pi.start();
            }
            tracing(pi);
            return getModel(pi);
        });
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
            @Parameter(description = "Pagination - page to start on", required = false) @QueryParam(value = "page") @DefaultValue("1") int page,
            @Parameter(description = "Pagination - number of items to return", required = false) @QueryParam(value = "size") @DefaultValue("10") int size,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            return process.instances().values(page, size).stream()
                    .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey()))
                    .collect(Collectors.toList());
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
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            
            return process.instances()
                .findById(id)
                .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey()))
                .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
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
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            
            ProcessInstance<$Type$> instance = process.instances()
                .findById(id)
                .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            String image = instance.image(extractImageBaseUri(uri.getRequestUri().toString()));
            
            if (image == null) {
                throw new ProcessImageNotFoundException(process.id());
            }
            StreamingOutput entity = new ImageStreamingOutput(image);     
            ResponseBuilder builder = Response.ok().entity(entity);
            
            return builder
                    .header("Content-Type", "image/svg+xml")
                    .build();
        } finally {
            IdentityProvider.set(null);
        }
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
        StreamingOutput entity = new ImageStreamingOutput(image);    
        ResponseBuilder builder = Response.ok().entity(entity);
        
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
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "delete $name$", description = "Number of instances of $name$ deleted/aborted")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "duration of deleting $name$", description = "A measure of how long it takes to delete instance of $name$.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of deleted instances of $name$", description="Rate of deleted instances of $name$")    
    public $Type$Output delete_$name$(@PathParam("id") @Parameter(description = "Unique identifier of the instance", required = true) final String id,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances()
                    .findById(id)
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));            
            tracing(pi);
            pi.abort();            
            return getModel(pi);
            
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
    public $Type$Output updateModel_$name$(@PathParam("id") @Parameter(description = "Unique identifier of the instance", required = true) String id, 
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            @Parameter(description = "Updates to the data model for $name$ instance", required = true) $Type$ resource) {
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances()
                    .findById(id)
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            tracing(pi);
            pi.updateVariables(resource);
            return mapOutput(new $Type$Output(), pi.variables(), pi.businessKey());
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
            @Parameter(description = "TagInstance content that should be associated with the $name$ instance", required = true) TagInstance resource) {
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances()
                    .findById(id)
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
    public Collection<? extends Tag> get_tags_$name$(@PathParam("id") @Parameter(description = "Unique identifier of the instance", required = true) String id, 
            @Parameter(description = "TagInstance to be removed", required = true) @PathParam("tagId") String tagId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user, 
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances()
                    .findById(id)
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            tracing(pi);
            pi.tags().remove(tagId);
            return pi.tags().get();
        });
    }
    
    protected $Type$Output getModel(ProcessInstance<$Type$> pi) {
        if (pi.status() == ProcessInstance.STATE_ERROR && pi.error().isPresent()) {
            throw new ProcessInstanceExecutionException(pi.id(), pi.error().get().failedNodeId(), pi.error().get().errorMessage());
        }
        
        return mapOutput(new $Type$Output(), pi.variables(), pi.businessKey());
    }
    
    protected Policy[] policies(String user, List<String> groups) {         
        return new Policy[] {SecurityPolicy.of(io.automatiko.engine.api.auth.IdentityProvider.get())};
    }
    
    protected $Type$ mapInput($Type$Input input, $Type$ resource) {
        resource.fromMap(input.toMap());
        
        return resource;
    }
    
    protected $Type$Output mapOutput($Type$Output output, $Type$ resource, String businessKey) {
        output.fromMap(businessKey != null ? businessKey: resource.getId(), resource.toMap());
        
        return output;
    }
    
    protected String extractImageBaseUri(String requestUri) {
        return requestUri.substring(0, requestUri.indexOf("/image"));
    }
    
    protected class ImageStreamingOutput implements StreamingOutput {
        
        private String image;
        
        public ImageStreamingOutput(String image) {
            this.image = image;
        }
        
        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {
            
            output.write(image.getBytes(StandardCharsets.UTF_8));                
        }
    }
    
    protected void tracing(ProcessInstance<?> intance) {
        
    }
}
