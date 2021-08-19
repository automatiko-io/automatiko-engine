
package com.myspace.demo;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.Sig;

public class $Type$Resource {


    @Mutation
    @Description("Signals '$signalName$' on instance with given id")
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "Trigger on $name$ with signal '$signalName$'", description = "Number of instances of $name$ triggered with signal '$signalName$'")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "Duration of triggering $name$ instance with signal '$signalName$'", description = "A measure of how long it takes to trigger instance of $name$ with signal '$signalName$'.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of triggering instances of $name$ with signal '$signalName$'", description="Rate of triggering instances of $name$ with signal '$signalName$'")   
    public $Type$Output parentprocessprefix$_signal(@Name("parentId") String id,
            @Name("id") String id_$name$, 
            @Name("user") final String user, 
            @Name("groups") final List<String> groups,
            final $signalType$ data) {

        
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$).orElseThrow(() -> new ProcessInstanceNotFoundException(id_$name$));
            tracing(pi);
            pi.send(Sig.of("$signalName$", data));
            
            return getSubModel_$name$(pi);
        });
        
    }

}
