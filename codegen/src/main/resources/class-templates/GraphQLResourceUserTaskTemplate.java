package com.myspace.demo;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.workflow.DefinedProcessErrorException;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.workflow.Sig;


public class $Type$Resource {

  
    @Mutation
    @Description("Adds new $taskName$ task instance")
    public $Type$Output signal(@Name("id") final String id,
            @Name("user") final String user, 
            @Name("groups") final List<String> groups) {
        
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances().findById(id).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            tracing(pi);
            pi.send(Sig.of("$taskNodeName$", java.util.Collections.emptyMap()));
       
            return getModel(pi);
        });
        
    }
 
    @Mutation
    @Description("Completes $taskName$ task instance with given id")
    public $Type$Output completeTask(@Name("id") final String id, @Name("workItemId") final String workItemId, @Name("phase") @DefaultValue("complete") final String phase, @Name("data") final $TaskOutput$ model,
            @Name("user") final String user, 
            @Name("groups") final List<String> groups) throws org.eclipse.microprofile.graphql.GraphQLException {
        
        
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = process.instances().findById(id).orElseThrow(() -> new ProcessInstanceNotFoundException(id));

                tracing(pi);
                io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition transition = new io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition(phase, model.toMap(), io.automatiko.engine.api.auth.IdentityProvider.get());
                pi.transitionWorkItem(workItemId, transition);

                return getModel(pi);
            });
        } catch(DefinedProcessErrorException e) {
            throw new org.eclipse.microprofile.graphql.GraphQLException(e.getMessage(), e.getError());
        } catch (WorkItemNotFoundException e) {
            return null;
        } finally {
            IdentityProvider.set(null);
        }
        
    }
    
      
    @Query
    @Description("Retrieves $taskName$ task instance with given id")
    public $TaskInput$ getTask(@Name("id") String id, @Name("workItemId") String workItemId,
            @Name("user") final String user, 
            @Name("groups") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        try {
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = process.instances().findById(id, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                WorkItem workItem = pi.workItem(workItemId, policies(user, groups));
                if (workItem == null) {
                    return null;
                }
                return $TaskInput$.fromMap(workItem.getId(), workItem.getName(), workItem.getParameters());
            });
        } catch (WorkItemNotFoundException e) {
            return null;
        }
    }
     
    @Mutation
    @Description("Aborts $taskName$ task instance with given id")
    public $Type$Output abortTask(@Name("id") final String id, @Name("workItemId") final String workItemId, @Name("phase") @DefaultValue("abort") final String phase,
            @Name("user") final String user, 
            @Name("groups") final List<String> groups) {
        
            try {
                identitySupplier.buildIdentityProvider(user, groups);
                return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                    ProcessInstance<$Type$> pi = process.instances().findById(id).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
                    tracing(pi);       
                    io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition transition = new io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition(phase, null, io.automatiko.engine.api.auth.IdentityProvider.get());
                    pi.transitionWorkItem(workItemId, transition);
    
                    return getModel(pi);
                });
            } catch (WorkItemNotFoundException e) {
                return null;
            }
        
    }
}
