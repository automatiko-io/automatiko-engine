package com.myspace.demo;

import java.util.List;

import io.automatik.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.WorkItem;
import io.automatik.engine.workflow.Sig;


public class $Type$Resource {

    @POST
    @Path("$prefix$/$name$/{id_$name$}/$taskName$")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "Creating new $taskName$ task", description = "Number of $taskName$ tasks created")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "Duration of creatingnew $taskName$ task", description = "A measure of how long it takes to create $taskName$ tasks.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of creating $taskName$ tasks", description="Rate of creating $taskName$ tasks")   
    public javax.ws.rs.core.Response signal(@PathParam("id") String id, @PathParam("id_$name$") String id_$name$) {
        return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$).orElseThrow(() -> new ProcessInstanceNotFoundException(id));

            pi.send(Sig.of("$taskNodeName$", java.util.Collections.emptyMap()));
            java.util.Optional<WorkItem> task = pi.workItems().stream().filter(wi -> wi.getName().equals("$taskName$")).findFirst();
            if(task.isPresent()) {
                return javax.ws.rs.core.Response.ok(getSubModel_$name$(pi))
                        .header("Link", "</" + id + "/$taskName$/" + task.get().getId() + ">; rel='instance'")
                        .build();
            }
            return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
        });
    }

    @POST()
    @Path("$prefix$/$name$/{id_$name$}/$taskName$/{workItemId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "Completed $taskName$ tasks", description = "Number of $taskName$ tasks completed")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "Duration of completing $taskName$ task", description = "A measure of how long it takes to complete $taskName$ task.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of completing $taskName$ tasks", description="Rate of completing $taskName$ tasks")       
    public $Type$Output completeTask(@PathParam("id") String id, @PathParam("id_$name$") String id_$name$, @PathParam("workItemId") final String workItemId, @QueryParam("phase") @DefaultValue("complete") final String phase, @QueryParam("user") final String user, @QueryParam("group") final List<String> groups, final $TaskOutput$ model) {
        try {
            return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$).orElseThrow(() -> new ProcessInstanceNotFoundException(id));

                io.automatik.engine.api.auth.IdentityProvider identity = null;
                if (user != null) {
                    identity = new io.automatik.engine.services.identity.StaticIdentityProvider(user, groups);
                }
                io.automatik.engine.workflow.base.instance.impl.humantask.HumanTaskTransition transition = new io.automatik.engine.workflow.base.instance.impl.humantask.HumanTaskTransition(phase, model.toMap(), identity);
                pi.transitionWorkItem(workItemId, transition);

                return getSubModel_$name$(pi);
            });
        } catch (WorkItemNotFoundException e) {
            return null;
        }
    }
    
    
    @GET()
    @Path("$prefix$/$name$/{id_$name$}/$taskName$/{workItemId}")
    @Produces(MediaType.APPLICATION_JSON)
    public $TaskInput$ getTask(@PathParam("id") String id, @PathParam("id_$name$") String id_$name$, @PathParam("workItemId") String workItemId, @QueryParam("user") final String user, @QueryParam("group") final List<String> groups) {
        try {
            ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$).orElseThrow(() -> new ProcessInstanceNotFoundException(id));

            WorkItem workItem = pi.workItem(workItemId, policies(user, groups));
            if (workItem == null) {
                return null;
            }
            return $TaskInput$.fromMap(workItem.getId(), workItem.getName(), workItem.getParameters());
        } catch (WorkItemNotFoundException e) {
            return null;
        }
    }
    
    @DELETE()
    @Path("$prefix$/$name$/{id_$name$}/$taskName$/{workItemId}")
    @Produces(MediaType.APPLICATION_JSON)
    @org.eclipse.microprofile.metrics.annotation.Counted(name = "Aborted $taskName$ tasks", description = "Number of $taskName$ tasks aborted")
    @org.eclipse.microprofile.metrics.annotation.Timed(name = "Duration of aborting $taskName$ task", description = "A measure of how long it takes to abort $taskName$ task.", unit = org.eclipse.microprofile.metrics.MetricUnits.MILLISECONDS)
    @org.eclipse.microprofile.metrics.annotation.Metered(name="Rate of aborting $taskName$ tasks", description="Rate of aborting $taskName$ tasks")           
    public $Type$Output abortTask(@PathParam("id") String id, @PathParam("id_$name$") String id_$name$, @PathParam("workItemId") final String workItemId, @QueryParam("phase") @DefaultValue("abort") final String phase, @QueryParam("user") final String user, @QueryParam("group") final List<String> groups) {
        
        try {
            
            return io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$).orElseThrow(() -> new ProcessInstanceNotFoundException(id));

                io.automatik.engine.api.auth.IdentityProvider identity = null;
                if (user != null) {
                    identity = new io.automatik.engine.services.identity.StaticIdentityProvider(user, groups);
                }
                io.automatik.engine.workflow.base.instance.impl.humantask.HumanTaskTransition transition = new io.automatik.engine.workflow.base.instance.impl.humantask.HumanTaskTransition(phase, null, identity);
                pi.transitionWorkItem(workItemId, transition);

                return getSubModel_$name$(pi);
            });
        } catch (WorkItemNotFoundException e) {
            return null;
        }
    }
}
