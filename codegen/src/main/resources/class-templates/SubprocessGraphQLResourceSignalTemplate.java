
package com.myspace.demo;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.workflow.DefinedProcessErrorException;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.Sig;

public class $Type$Resource {


    @Mutation
    @Description("Signals '$signalName$' on instance with given id and parent id")
    public $Type$Output $parentprocessprefix$_signal(@Name("parentId") String id,
            @Name("id") String id_$name$, 
            @Name("user") final String user, 
            @Name("groups") final List<String> groups,
            @Name("model") final $signalType$ data) throws org.eclipse.microprofile.graphql.GraphQLException {

        try {
            identitySupplier.buildIdentityProvider(user, groups);
            return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK).orElseThrow(() -> new ProcessInstanceNotFoundException(id_$name$));
                tracing(pi);
                pi.send(Sig.of("$signalName$", data));
                
                return getSubModel_$name$(pi);
            });
        } catch(DefinedProcessErrorException e) {
            throw new org.eclipse.microprofile.graphql.GraphQLException(e.getMessage(), e.getError());
        }
    }

}
