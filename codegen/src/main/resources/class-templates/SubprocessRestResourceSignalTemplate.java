
package com.myspace.demo;

import java.util.List;

import io.automatik.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.workflow.Sig;

public class $Type$Resource {


    @POST
    @Path("$prefix$/$name$/{id_$name$}/$signalPath$")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "Trigger on $name$", description = "Number of instances of $name$ triggered")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "Duration of triggering $name$ instance", description = "A measure of how long it takes to trigger instance of $name$.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of triggering instances of $name$", description="Rate of triggering instances of $name$")   
    public $Type$Output signal(@PathParam("id") String id, @PathParam("id_$name$") String id_$name$, final $signalType$ data) {
        return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            pi.send(Sig.of("$signalName$", data));
            return getModel(pi);
        });
    }

}
