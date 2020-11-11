package com.myspace.demo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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


import io.automatik.engine.api.Application;
import io.automatik.engine.api.auth.SecurityPolicy;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatik.engine.api.workflow.WorkItem;
import io.automatik.engine.api.workflow.workitem.Policy;

@Path("/$name$")
public class $Type$ReactiveResource {

    Process<$Type$> process;
    
    Application application;

    @POST()
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)    
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "Create $name$", description = "Number of new instances of $name$")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "Duration of creating $name$", description = "A measure of how long it takes to create new instance of $name$.", unit = MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of instances of $name$", description="Rate of new instances of $name$")
    public CompletionStage<$Type$Output> create_$name$(@Context HttpHeaders httpHeaders, @QueryParam("businessKey") String businessKey, $Type$Input resource) {
        if (resource == null) {
            resource = new $Type$Input();
        }
        final $Type$Input value = resource;
        return CompletableFuture.supplyAsync(() -> {
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
        });
    }

    @GET()
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<List<$Type$Output>> getAll_$name$() {
        return CompletableFuture.supplyAsync(() -> {
            return process.instances().values(1, 10).stream()
                    .map(pi -> mapOutput(new $Type$Output(), pi.variables()))
                 .collect(Collectors.toList());
        });   
    }

    @GET()
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<$Type$Output> get_$name$(@PathParam("id") String id) {
        return CompletableFuture.supplyAsync(() -> {
            return process.instances()
                    .findById(id)
                    .map(pi -> mapOutput(new $Type$Output(), pi.variables()))
                    .orElse(null);
        });
    }
    
    @DELETE()
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "Abort $name$", description = "Number of instances of $name$ aborted")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "Duration of aborting $name$", description = "A measure of how long it takes to abort instance of $name$.", unit = MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of aborted instances of $name$", description="Rate of aborted instances of $name$")       
    public CompletionStage<$Type$Output> delete_$name$(@PathParam("id") final String id) {
        return CompletableFuture.supplyAsync(() -> {
            return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = process.instances()
                        .findById(id)
                        .orElse(null);
                if (pi == null) {
                    return null;
                } else {
                    pi.abort();
                    return getModel(pi);
                }
            });
        });
    }
    
    @POST()
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<$Type$Output> updateModel_$name$(@PathParam("id") String id, $Type$ resource) {
        return CompletableFuture.supplyAsync(() -> {
            return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = process.instances()
                        .findById(id)
                        .orElse(null);
                if (pi == null) {
                    return null;
                } else {
                    pi.updateVariables(resource);
                    return mapOutput(new $Type$Output(), pi.variables());
                }
            });
        });
    }
    
    @GET()
    @Path("/{id}/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<Map<String, String>> getTasks_$name$(@PathParam("id") String id, @QueryParam("user") final String user, @QueryParam("group") final List<String> groups) {
        return CompletableFuture.supplyAsync(() -> {
            return process.instances()
                    .findById(id)
                    .map(pi -> pi.workItems(policies(user, groups)))
                    .map(l -> l.stream().collect(Collectors.toMap(WorkItem::getId, WorkItem::getName)))
                    .orElse(null);
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
        output.fromMap(resource.toMap());
        
        return output;
    }
}
