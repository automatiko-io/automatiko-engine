package com.myspace.demo;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.workflow.DefinedProcessErrorException;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.workflow.Sig;


public class $Type$Resource {


    @Mutation
    @Description("Completes $taskName$ task instance with given id")
    public $Type$Output $parentprocessprefix$_completeTask(@Name("parentId") String id,
            @Name("id") String id_$name$, 
            @Name("workItemId") final String workItemId, 
            @Name("phase") @DefaultValue("complete") final String phase, 
            @Name("user") final String user, 
            @Name("group") final List<String> groups, 
            @Name("data") final $TaskOutput$ model) throws org.eclipse.microprofile.graphql.GraphQLException {
        try {
           
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                String combinedId;
                if (id_$name$.contains(":")) {
                    combinedId = id_$name$;
                } else {
                    combinedId = $parentprocessid$ + ":" + id_$name$;
                }
                ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById(combinedId).orElseThrow(() -> new ProcessInstanceNotFoundException(id_$name$));
                tracing(pi);
                io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition transition = new io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition(phase, model.toMap(), io.automatiko.engine.api.auth.IdentityProvider.get());
                pi.transitionWorkItem(workItemId, transition);

                return getSubModel_$name$(pi);
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
    public $TaskInput$ $parentprocessprefix$_getTask(@Name("parentId") String id,
            @Name("id") String id_$name$, 
            @Name("workItemId") final String workItemId, 
            @Name("user") final String user, 
            @Name("group") final List<String> groups) {
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                String combinedId;
                if (id_$name$.contains(":")) {
                    combinedId = id_$name$;
                } else {
                    combinedId = $parentprocessid$ + ":" + id_$name$;
                }
                ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById(combinedId, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.READ_ONLY).orElseThrow(() -> new ProcessInstanceNotFoundException(id_$name$));
    
                WorkItem workItem = pi.workItem(workItemId, policies(user, groups));
                if (workItem == null) {
                    return null;
                }
                return $TaskInput$.fromMap(workItem.getId(), workItem.getName(), workItem.getParameters());
            });
        } catch (WorkItemNotFoundException e) {
            return null;
        } finally {
            IdentityProvider.set(null);
        }
    }

    @Mutation
    @Description("Aborts $taskName$ task instance with given id")
    public $Type$Output $parentprocessprefix$_abortTask(@Name("parentId") String id,
            @Name("id") String id_$name$, 
            @Name("workItemId") final String workItemId, 
            @Name("phase") @DefaultValue("abort") final String phase, 
            @Name("user") final String user, 
            @Name("group") final List<String> groups) {
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                String combinedId;
                if (id_$name$.contains(":")) {
                    combinedId = id_$name$;
                } else {
                    combinedId = $parentprocessid$ + ":" + id_$name$;
                }
                ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById(combinedId).orElseThrow(() -> new ProcessInstanceNotFoundException(id_$name$));
                tracing(pi);
                io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition transition = new io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition(phase, null, io.automatiko.engine.api.auth.IdentityProvider.get());
                pi.transitionWorkItem(workItemId, transition);
          
                return getSubModel_$name$(pi);
            });
            
        } catch (WorkItemNotFoundException e) {
            return null;
        }
    }
}
