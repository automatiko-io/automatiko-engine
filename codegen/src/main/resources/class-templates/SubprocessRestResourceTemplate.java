package com.myspace.demo;

import java.util.Collections;
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
import io.automatik.engine.api.workflow.WorkItem;
import io.automatik.engine.api.workflow.workitem.Policy;
import io.automatik.engine.workflow.Sig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class $Type$Resource {

    Process<$Type$> subprocess_$name$;

    @GET()
    @Path("$prefix$/$name$")
    @Produces(MediaType.APPLICATION_JSON)
    public List<$Type$Output> getAll_$name$(@PathParam("id") String id) {
        
    	ProcessInstance parent = $parentprocess$.instances()
                .findById($parentprocessid$)
                .orElse(null);
        if (parent != null) {
    	
        	return (List<$Type$Output>) parent.subprocesses().stream()
                .map(pi -> mapOutput(new $Type$Output(), ($Type$) ((ProcessInstance)pi).variables()))
                .collect(java.util.stream.Collectors.toList());
        } else {
        	return java.util.Collections.emptyList();
        }
    }

    @GET()
    @Path("$prefix$/$name$/{id_$name$}")
    @Produces(MediaType.APPLICATION_JSON)
    public $Type$Output get_$name$(@PathParam("id") String id, @PathParam("id_$name$") String id_$name$) {
        return subprocess_$name$.instances()
                .findById(id_$name$)
                .map(pi -> mapOutput(new $Type$Output(), pi.variables()))
                .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
    }

    @DELETE()
    @Path("$prefix$/$name$/{id_$name$}")
    @Produces(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "delete $name$", description = "Number of instances of $name$ deleted/aborted")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "duration of deleting $name$", description = "A measure of how long it takes to delete instance of $name$.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of deleted instances of $name$", description="Rate of deleted instances of $name$")    
    public $Type$Output delete_$name$(@PathParam("id") String id, @PathParam("id_$name$") final String id_$name$) {
        return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = subprocess_$name$.instances()
                    .findById(id_$name$)
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            pi.abort();
            return getSubModel_$name$(pi);
            
        });
    }
    
    @POST()
    @Path("$prefix$/$name$/{id_$name$}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public $Type$Output updateModel_$name$(@PathParam("id") String id, @PathParam("id_$name$") String id_$name$, $Type$ resource) {
        return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = subprocess_$name$.instances()
                    .findById(id_$name$)
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));

            pi.updateVariables(resource);
            return mapOutput(new $Type$Output(), pi.variables());

        });
    }
    
    @GET()
    @Path("$prefix$/$name$/{id_$name$}/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    public java.util.List<WorkItem.Descriptor> getTasks_$name$(@PathParam("id") String id, @PathParam("id_$name$") String id_$name$, @QueryParam("user") final String user, @QueryParam("group") final List<String> groups) {
        
        return subprocess_$name$.instances()
                .findById(id_$name$)
                .map(pi -> pi.workItems(policies(user, groups)))
                .map(l -> l.stream().map(WorkItem::toMap).collect(Collectors.toList()))
                .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
    }
    
    protected $Type$Output getSubModel_$name$(ProcessInstance<$Type$> pi) {
        if (pi.status() == ProcessInstance.STATE_ERROR && pi.error().isPresent()) {
            throw new ProcessInstanceExecutionException(pi.id(), pi.error().get().failedNodeId(), pi.error().get().errorMessage());
        }
        
        return mapOutput(new $Type$Output(), pi.variables());
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
