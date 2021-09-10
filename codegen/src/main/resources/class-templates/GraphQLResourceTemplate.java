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

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Name;

import io.smallrye.mutiny.Multi;
import io.smallrye.graphql.api.Subscription;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.workflow.AbstractProcessInstance;
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
import io.automatiko.engine.service.auth.HttpAuthSupport;


@Description("$processdocumentation$")
@GraphQLApi
public class $Type$GraphQLResource {
    
    BroadcastProcessor<$Type$Output> createdProcessor = BroadcastProcessor.create();
    
    BroadcastProcessor<$Type$Output> completedProcessor = BroadcastProcessor.create();
    
    BroadcastProcessor<$Type$Output> abortedProcessor = BroadcastProcessor.create();
    
    BroadcastProcessor<$Type$Output> inErrorProcessor = BroadcastProcessor.create();

    Process<$Type$> process;
    
    Application application;
    
    IdentitySupplier identitySupplier;

    @Mutation("create_$name$$prefix$")
    @Description("Creates new instance of $name$ $prefix$")
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "create $name$", description = "Number of new instances of $name$")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "duration of creating $name$", description = "A measure of how long it takes to create new instance of $name$.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of instances of $name$", description="Rate of new instances of $name$")
    public $Type$Output create_$name$(@Name("key") @DefaultValue("") final String businessKey, @Name("data") $Type$Input resource, 
            @Name("user") final String user, 
            @Name("groups") final List<String> groups) {
        if (resource == null) {
            resource = new $Type$Input();
        }
        final $Type$Input value = resource;
                
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = process.createInstance(businessKey.isEmpty() ? null : businessKey, mapInput(value, new $Type$()));
                pi.start();
                
                $Type$Output model = getModel(pi);
                
                createdProcessor.onNext(model);
                
                tracing(pi);                
 
                return model;
            });
       
    }

    @Query("get_all_$name$$prefix$")
    @Description("Retrieves instances of $name$ $prefix$")
    public List<$Type$Output> getAll_$name$(@Name("tags") final List<String> tags, @Name("status") @DefaultValue("active") final String status, @Name("page") @DefaultValue("1") int page, @Name("size") @DefaultValue("10") int size, 
            @Name("user") final String user, 
            @Name("groups") final List<String> groups) {
        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            if (tags != null && !tags.isEmpty()) {
                return process.instances().findByIdOrTag(io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY, mapStatus(status), tags.toArray(String[]::new)).stream()
                        .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), pi))
                        .collect(Collectors.toList());
            } else {
                return process.instances().values(io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY, mapStatus(status), page, size).stream()
                    .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), pi))
                    .collect(Collectors.toList());
            }
        });
    }

       
    @Query("get_$name$$prefix$")
    @Description("Retrieves $name$ $prefix$ instance with given id")
    public $Type$Output get_$name$(@Name("id") String id, @Name("status") @DefaultValue("active") final String status, 
            @Name("user") final String user, 
            @Name("groups") final List<String> groups) {
        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                return process.instances()
                    .findById(id, mapStatus(status), io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY)
                    .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), pi))
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            });
    }
    
    @Mutation("delete_$name$$prefix$")
    @Description("Deletes $name$ $prefix$ instance with given id")
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "delete $name$", description = "Number of instances of $name$ deleted/aborted")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "duration of deleting $name$", description = "A measure of how long it takes to delete instance of $name$.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of deleted instances of $name$", description="Rate of deleted instances of $name$")    
    public $Type$Output delete_$name$(@Name("id") final String id, 
            @Name("user") final String user, 
            @Name("groups") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
             
            ProcessInstance<$Type$> pi = process.instances().findById(id, ProcessInstance.STATE_ACTIVE, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE).orElse(null);
            if (pi == null) {
                pi = process.instances().findById(id, ProcessInstance.STATE_ERROR, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            }
            tracing(pi);
            pi.abort();            
            return getModel(pi);
            
        });
    }
     
//    @Mutation("update_model_$name$")
//    @Description("Updates data of $name$ instance with given id")
//    public $Type$Output update_model_$name$(@Name("data") $Type$ resource) {
//        
//       
//        identitySupplier.buildIdentityProvider(null, null);
//        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
//            ProcessInstance<$Type$> pi = process.instances()
//                    .findById(resource.getId())
//                    .orElseThrow(() -> new ProcessInstanceNotFoundException(resource.getId()));
//            tracing(pi);
//            pi.updateVariables(resource);
//  
//            return mapOutput(new $Type$Output(), pi.variables(), pi.businessKey());
//        });
//        
//    }
    
    @Query("get_$name$$prefix$_tasks")
    @Description("Retrieves tasks currently active in $name$ $prefix$ instance with given id")
    public java.util.List<WorkItem.Descriptor> getTasks_$name$(@Name("id")  String id, 
            @Name("user") final String user, 
            @Name("groups") final List<String> groups) {
        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                return process.instances()
                    .findById(id, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY)
                    .map(pi -> pi.workItems(policies(user, groups)))
                    .map(l -> l.stream().map(WorkItem::toMap).collect(Collectors.toList()))
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            });
    }
    
    @Subscription
    public Multi<$Type$Output> $name$$prefix$_created(){
        return createdProcessor; 
    }
    
    @Subscription
    public Multi<$Type$Output> $name$$prefix$_completed(){
        return completedProcessor; 
    }
    
    @Subscription
    public Multi<$Type$Output> $name$$prefix$_aborted(){
        return abortedProcessor; 
    }
    
    @Subscription
    public Multi<$Type$Output> $name$$prefix$_in_error(){
        return inErrorProcessor; 
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected $Type$Output getModel(ProcessInstance<$Type$> pi) {
        $Type$Output model = mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), pi);
        if (pi.status() == ProcessInstance.STATE_ERROR && pi.errors().isPresent()) {
            
            inErrorProcessor.onNext(model);
            
            throw new ProcessInstanceExecutionException(pi.id(), pi.errors().get().failedNodeIds(), pi.errors().get().errorMessages());
        }        
        
        if (pi.status() == ProcessInstance.STATE_COMPLETED) {
            completedProcessor.onNext(model);
        } else if (pi.status() == ProcessInstance.STATE_ABORTED) {
            abortedProcessor.onNext(model);
        }
        
        return model;
    }
    
    protected Policy[] policies(String user, List<String> groups) {         
        return new Policy[] {SecurityPolicy.of(io.automatiko.engine.api.auth.IdentityProvider.get())};
    }
    
    protected $Type$ mapInput($Type$Input input, $Type$ resource) {
        resource.fromMap(input.toMap());
        
        return resource;
    }
    
    protected $Type$Output mapOutput($Type$Output output, $Type$ resource, String businessKey, ProcessInstance<$Type$> pi) {
        output.fromMap(businessKey != null ? businessKey: resource.getId(), resource.toMap());
        
        return output;
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
