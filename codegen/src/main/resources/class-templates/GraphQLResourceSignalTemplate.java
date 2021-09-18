
package com.myspace.demo;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.Sig;

public class $Type$Resource {

    @Mutation
    @Description("Signals '$signalName$' on instance with given id")
    public $Type$Output signal(@Name("id") final String id, @Name("model") final $signalType$ data) {
        
        identitySupplier.buildIdentityProvider(null, null);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.instances().findById(id).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            tracing(pi);
            pi.send(Sig.of("$signalName$", data));

            return getModel(pi);
        });
        
    }

}
