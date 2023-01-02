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

import javax.json.JsonValue.ValueType;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Name;

import io.smallrye.mutiny.Multi;
import io.smallrye.graphql.api.Context;
import io.smallrye.graphql.api.Subscription;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.Sig;
import io.automatiko.engine.api.workflow.DefinedProcessErrorException;
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
import io.automatiko.addons.graphql.GraphQLProcessSubscriptionEventPublisher;

@javax.enterprise.context.ApplicationScoped
@SuppressWarnings({ "rawtypes", "unchecked" })
@Description("$processdocumentation$")
@GraphQLApi
public class $Type$GraphQLResource {
    
    
    GraphQLProcessSubscriptionEventPublisher subscriptionPublisher;

    Process<$Type$> process;
    
    Application application;
    
    IdentitySupplier identitySupplier;
    
    Context context;
    
    @javax.inject.Inject
    public $ResourceType$(Context context, Application application, @javax.inject.Named("$id$$version$") Process<$Type$> process, IdentitySupplier identitySupplier, GraphQLProcessSubscriptionEventPublisher subscriptionPublisher) {
        this.context = context;
        this.application = application;
        this.process = process;
        this.identitySupplier = identitySupplier;
        this.subscriptionPublisher = subscriptionPublisher;
        this.subscriptionPublisher.configure(process.id(), pi -> mapOutput(new $Type$Output(), ((ProcessInstance<$Type$>) pi).variables(), ((ProcessInstance<$Type$>) pi).businessKey(), ((ProcessInstance<$Type$>) pi)));
    }

    
    @Mutation("create_$name$$prefix$")
    @Description("Creates new instance of $name$ $prefix$")
    public $Type$Output create_$name$(@Name("key") @DefaultValue("") final String businessKey, @Name("data") $Type$Input resource, 
            @Name("user") final String user, 
            @Name("groups") final List<String> groups) throws org.eclipse.microprofile.graphql.GraphQLException {
        if (resource == null) {
            resource = new $Type$Input();
        }
        final $Type$Input value = resource;
        try {        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = process.createInstance(businessKey.isEmpty() ? null : businessKey, mapInput(value, new $Type$()));
                pi.start();
                
                $Type$Output model = getModel(pi);
                
                subscriptionPublisher.created($Type$Output.class).onNext(model, ((AbstractProcessInstance<?>) pi).visibleTo());
                
                tracing(pi);                
 
                return model;
            });
         } catch(DefinedProcessErrorException e) {
             throw new org.eclipse.microprofile.graphql.GraphQLException(e.getMessage(), e.getError());
         }
    }

    
    @Query("get_all_$name$$prefix$")
    @Description("Retrieves instances of $name$ $prefix$")
    public List<$Type$Output> getAll_$name$(@Name("tags") final List<String> tags, @Name("status") @DefaultValue("active") final String status, @Name("page") @DefaultValue("1") int page, @Name("size") @DefaultValue("10") int size, 
            @Name("sortBy") final String sortBy, @Name("sortAsc") @DefaultValue("true") final Boolean sortAsc, @Name("user") final String user, 
            @Name("groups") final List<String> groups) {
        
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            if (tags != null && !tags.isEmpty()) {
                if (sortBy != null && !sortBy.trim().isEmpty()) {
                    return process.instances().findByIdOrTag(io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY, mapStatus(status), sortBy, sortAsc, tags.toArray(String[]::new)).stream()
                            .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), pi))
                            .collect(Collectors.toList());
                } else {
                    return process.instances().findByIdOrTag(io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY, mapStatus(status), tags.toArray(String[]::new)).stream()
                        .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), pi))
                        .collect(Collectors.toList());
                }
            } else {
                if (sortBy != null && !sortBy.trim().isEmpty()) {
                    return process.instances().values(io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY, mapStatus(status), page, size, sortBy, sortAsc).stream()
                            .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), pi))
                            .collect(Collectors.toList());
                } else {
                    return process.instances().values(io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY, mapStatus(status), page, size).stream()
                    .map(pi -> mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), pi))
                    .collect(Collectors.toList());
                }
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
    public $Type$Output delete_$name$(@Name("id") final String id, @Name("status") @DefaultValue("active") final String status,
            @Name("user") final String user, 
            @Name("groups") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
             
            ProcessInstance<$Type$> pi = process.instances().findById(id, mapStatus(status), io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK).orElseThrow(() -> new ProcessInstanceNotFoundException(id));            
            tracing(pi);
            pi.abort();            
            return getModel(pi);
            
        });
    }
     
    @Mutation("update_model_$name$$prefix$")
    @Description("Updates data of $name$ instance with given id")
    public $Type$Output update_model_$name$(@Name("id")  String id, @Name("status") @DefaultValue("active") final String status, 
            @Name("user") final String user, 
            @Name("groups") final List<String> groups, @Name("data") $Type$ resource) {
        
       
        identitySupplier.buildIdentityProvider(null, null);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances()
                    .findById(id, mapStatus(status), io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK)
                    .orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            tracing(pi);
            pi.updateVariables(resource);
  
            return mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), pi);
        });
        
    }
    
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
    @Description("Emits on every new $name$ $prefix$ instance being created")
    public Multi<$Type$Output> $name$$prefix$_created(@Name("user") @DefaultValue("") final String user, 
            @Name("groups")  @DefaultValue("[]") final List<String> groups) {
        
        identitySupplier.buildIdentityProvider(user, groups);
        return subscriptionPublisher.created($Type$Output.class).onSubscribe().invoke(() -> IdentityProvider.set(null));
        
    }
    
    
    @Subscription
    @Description("Emits on every new $name$ $prefix$ instance being completed")
    public Multi<$Type$Output> $name$$prefix$_completed(@Name("user") @DefaultValue("") final String user, 
            @Name("groups") final List<String> groups) {
        
        identitySupplier.buildIdentityProvider(user, groups);
        return subscriptionPublisher.completed($Type$Output.class).onSubscribe().invoke(() -> IdentityProvider.set(null)); 
    }
    
    
    @Subscription
    @Description("Emits on every new $name$ $prefix$ instance being aborted")
    public Multi<$Type$Output> $name$$prefix$_aborted(@Name("user") @DefaultValue("") final String user, 
            @Name("groups") final List<String> groups) {
        
        identitySupplier.buildIdentityProvider(user, groups);
        return subscriptionPublisher.aborted($Type$Output.class).onSubscribe().invoke(() -> IdentityProvider.set(null)); 
    }
    
    
    @Subscription
    @Description("Emits on every new $name$ $prefix$ instance failing on node execution")
    public Multi<$Type$Output> $name$$prefix$_in_error(@Name("user") @DefaultValue("") final String user, 
            @Name("groups") final List<String> groups) {
        
        identitySupplier.buildIdentityProvider(user, groups);
        return subscriptionPublisher.inError($Type$Output.class).onSubscribe().invoke(() -> IdentityProvider.set(null)); 
    }
    
    
    @Subscription
    @Description("Emits on every new $name$ $prefix$ instance being changed (resumed by signal, user task completion etc)")
    public Multi<$Type$Output> $name$$prefix$_changed(@Name("user") @DefaultValue("") final String user, 
            @Name("groups") final List<String> groups) {
        
        identitySupplier.buildIdentityProvider(user, groups);
        return subscriptionPublisher.changed($Type$Output.class).onSubscribe().invoke(() -> IdentityProvider.set(null)); 
    }
    
    protected $Type$Output getModel(ProcessInstance<$Type$> pi) {
        $Type$Output model = mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), pi);
        if (pi.status() == ProcessInstance.STATE_ERROR && pi.errors().isPresent()) {           
            throw new ProcessInstanceExecutionException(pi.id(), pi.errors().get().failedNodeIds(), pi.errors().get().errorMessages());
        }
        if (pi.abortCode() != null) {
            throw new DefinedProcessErrorException(pi.id(), pi.abortCode(), model);            
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
        if (metadataRequested()) {
            output.setMetadata(pi.metadata());
        }
        
        return output;
    }
    
    protected boolean metadataRequested() {
        try {
            return context.getSelectedFields().stream().anyMatch(field -> {
                if (field.getValueType() == ValueType.OBJECT && field.asJsonObject().containsKey("metadata")) {
                    return true;
                }
    
                return false;
            });
        } catch(Exception e) {
            // in case selected fields cannot be fetched allow to return metadata
            return true;
        }
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
