package com.myspace.demo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import io.automatik.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatik.engine.api.Application;
import io.automatik.engine.api.auth.SecurityPolicy;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatik.engine.api.workflow.ProcessInstanceNotFoundException;
import io.automatik.engine.api.workflow.Tag;
import io.automatik.engine.api.workflow.WorkItem;
import io.automatik.engine.api.workflow.workitem.Policy;
import io.automatik.engine.workflow.Sig;
import io.automatik.engine.workflow.base.instance.TagInstance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/$name$")
public class $Type$Resource {

    Process<$Type$> process;
    
    Application application;

    @POST()
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "create $name$", description = "Number of new instances of $name$")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "duration of creating $name$", description = "A measure of how long it takes to create new instance of $name$.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of instances of $name$", description="Rate of new instances of $name$")
    public $Type$Output create_$name$(@Context HttpHeaders httpHeaders, @QueryParam("businessKey") String businessKey, $Type$Input resource) {
        if (resource == null) {
            resource = new $Type$Input();
        }
        final $Type$Input value = resource;

        return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.createInstance(businessKey, mapInput(value, new $Type$()));
            String startFromNode = httpHeaders.getHeaderString("X-AUTOMATIK-StartFromNode");
            
            if (startFromNode != null) {
                pi.startFrom(startFromNode);
            } else {
            
                pi.start();
            }
            return getModel(pi);
        });
    }

    @GET()
    @Produces(MediaType.APPLICATION_JSON)
    public List<$Type$Output> getAll_$name$() {
        return process.instances().values().stream()
                .map(pi -> mapOutput(new $Type$Output(), pi.variables()))
                .collect(Collectors.toList());
    }

    @GET()
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public $Type$Output get_$name$(@PathParam("id") String id) {
        return process.instances()
                .findById(id)
                .map(pi -> mapOutput(new $Type$Output(), pi.variables()))
                .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
    }

    @DELETE()
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "delete $name$", description = "Number of instances of $name$ deleted/aborted")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "duration of deleting $name$", description = "A measure of how long it takes to delete instance of $name$.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of deleted instances of $name$", description="Rate of deleted instances of $name$")    
    public $Type$Output delete_$name$(@PathParam("id") final String id) {
        return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances()
                    .findById(id)
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));            
            pi.abort();
            return getModel(pi);
            
        });
    }
    
    @POST()
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public $Type$Output updateModel_$name$(@PathParam("id") String id, $Type$ resource) {
        return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances()
                    .findById(id)
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
        
            pi.updateVariables(resource);
            return mapOutput(new $Type$Output(), pi.variables());
        });
    }
    
    @GET()
    @Path("/{id}/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    public java.util.List<WorkItem.Descriptor> getTasks_$name$(@PathParam("id") String id, @QueryParam("user") final String user, @QueryParam("group") final List<String> groups) {
        
        return process.instances()
                .findById(id)
                .map(pi -> pi.workItems(policies(user, groups)))
                .map(l -> l.stream().map(WorkItem::toMap).collect(Collectors.toList()))
                .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
    }
    
    @GET()
    @Path("/{id}/tags")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Tag> get_tags_$name$(@PathParam("id") String id) {
        return process.instances()
                .findById(id)
                .map(pi -> pi.tags().get())
                .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
    }
    
    @POST()
    @Path("/{id}/tags")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Tag> add_tag_$name$(@PathParam("id") String id, TagInstance resource) {
        return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances()
                    .findById(id)
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
        
            pi.tags().add(resource.getValue());
            return pi.tags().get();
        });
    }
    
    @DELETE()
    @Path("/{id}/tags/{tagId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Tag> get_tags_$name$(@PathParam("id") String id, @PathParam("tagId") String tagId) {
        return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances()
                    .findById(id)
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
        
            pi.tags().remove(tagId);
            return pi.tags().get();
        });
    }
    
    protected $Type$Output getModel(ProcessInstance<$Type$> pi) {
        if (pi.status() == ProcessInstance.STATE_ERROR && pi.error().isPresent()) {
            throw new ProcessInstanceExecutionException(pi.id(), pi.error().get().failedNodeId(), pi.error().get().errorMessage());
        }
        
        return mapOutput(new $Type$Output(), pi.variables());
    }
    
    protected Policy[] policies(String user, List<String> groups) {
        if (user == null) {
            return new Policy[0];
        } 
        io.automatik.engine.api.auth.IdentityProvider identity = null;
        if (user != null) {
            identity = new io.automatik.engine.services.identity.StaticIdentityProvider(user, groups);
        }
        return new Policy[] {SecurityPolicy.of(identity)};
    }
    
    protected $Type$ mapInput($Type$Input input, $Type$ resource) {
        resource.fromMap(input.toMap());
        
        return resource;
    }
    
    protected $Type$Output mapOutput($Type$Output output, $Type$ resource) {
        output.fromMap(resource.getId(), resource.toMap());
        
        return output;
    }
}
