package io.automatiko.addon.usertasks.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.api.auth.TrustedIdentityProvider;
import io.automatiko.engine.api.event.process.ProcessWorkItemTransitionEvent;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.uow.UnitOfWork;
import io.automatiko.engine.api.uow.WorkUnit;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.base.core.event.ProcessWorkItemTransitionEventImpl;
import io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatiko.engine.workflow.process.instance.node.HumanTaskNodeInstance;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "User task index Management", description = "User task index management on top of the service")
@Path("/management/index/tasks")
public class UserTaskIndexManagementResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskIndexManagementResource.class);

    protected Map<String, Process<?>> processData = new LinkedHashMap<String, Process<?>>();

    protected Application application;

    protected IdentitySupplier identitySupplier;

    public UserTaskIndexManagementResource() {

    }

    @Inject
    public UserTaskIndexManagementResource(Application application, Instance<Process<?>> availableProcesses,
            IdentitySupplier identitySupplier) {
        this.processData = availableProcesses == null ? Collections.emptyMap()
                : availableProcesses.stream().collect(Collectors.toMap(p -> p.id(), p -> p));
        this.application = application;
        this.identitySupplier = identitySupplier;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response reindexAll() {
        Map<String, Object> result = new HashMap<>();
        IdentityProvider.set(new TrustedIdentityProvider("System<reindex>"));
        try {

            for (Process<?> process : processData.values()) {
                UnitOfWork uow = application.unitOfWorkManager().newUnitOfWork();
                uow.start();
                int indexedInstances = 0;
                Collection<ProcessInstance<?>> instances = loadProcessInstances(process);

                for (ProcessInstance<?> instance : instances) {
                    try {
                        indexedInstances += indexProcessInstance(uow, process, instance);
                    } catch (Throwable e) {
                        LOGGER.warn("Unable to reindex user tasks for instance {} and process {} due to {}", process.id(),
                                instance.id());
                    }
                }

                uow.end();
                result.put(process.id(), indexedInstances);
            }
        } finally {
            IdentityProvider.set(null);
        }
        result.put("message", "User tasks reindexed");
        return Response.ok().entity(result).build();
    }

    @Path("/{processId}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response reindexProcess(@PathParam("processId") String processId) {

        UnitOfWork uow = application.unitOfWorkManager().newUnitOfWork();
        uow.start();
        Process<?> process = processData.get(processId);
        if (process == null) {
            return Response.status(404).entity(Map.of("message", "Process with id '" + processId + "' was not found")).build();
        }
        int indexedInstances = 0;
        IdentityProvider.set(new TrustedIdentityProvider("System<reindex>"));
        try {

            Collection<ProcessInstance<?>> instances = loadProcessInstances(process);

            for (ProcessInstance<?> instance : instances) {
                try {
                    indexedInstances = indexProcessInstance(uow, process, instance);
                } catch (Throwable e) {
                    LOGGER.warn("Unable to reindex user tasks for instance {} and process {} due to {}", process.id(),
                            instance.id());
                }
            }
        } finally {
            IdentityProvider.set(null);
        }
        uow.end();
        return Response.ok().entity(Map.of("message", "User tasks for process " + processId + " reindexed",
                "count", indexedInstances)).build();
    }

    @Path("/{processId}/{instanceId}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response reindexProcessInstance(@PathParam("processId") String processId,
            @PathParam("instanceId") String instanceId) {
        UnitOfWork uow = application.unitOfWorkManager().newUnitOfWork();
        uow.start();
        Process<?> process = processData.get(processId);

        if (process == null) {
            return Response.status(404).entity(Map.of("message", "Process with id '" + processId + "' was not found")).build();
        }
        int indexedInstances = 0;
        IdentityProvider.set(new TrustedIdentityProvider("System<reindex>"));
        try {
            Optional<ProcessInstance<?>> instance = loadProcessInstance(process, instanceId);

            if (instance.isEmpty()) {
                return Response.status(404)
                        .entity(Map.of("message", "Process instance with id '" + instanceId + "' was not found"))
                        .build();
            }
            indexedInstances = indexProcessInstance(uow, process, instance.get());
        } catch (Throwable e) {
            LOGGER.warn("Unable to reindex user tasks for instance {} and process {} due to {}", process.id(),
                    instanceId);
        } finally {
            IdentityProvider.set(null);
        }
        uow.end();
        return Response.ok().entity(Map.of("message", "User tasks for process instance " + instanceId + " reindexed",
                "count", indexedInstances)).build();
    }

    protected int indexProcessInstance(UnitOfWork uow, Process<?> process, ProcessInstance<?> instance) {
        int count = 0;
        WorkflowProcessInstanceImpl workflowInstance = (WorkflowProcessInstanceImpl) ((AbstractProcessInstance<?>) instance)
                .internalGetProcessInstance();
        Collection<io.automatiko.engine.workflow.process.instance.NodeInstance> nodeInstances = workflowInstance
                .getNodeInstances(true);

        for (NodeInstance nodeInstance : nodeInstances) {

            if (nodeInstance instanceof HumanTaskNodeInstance) {
                ProcessWorkItemTransitionEvent event = new ProcessWorkItemTransitionEventImpl(workflowInstance,
                        ((HumanTaskNodeInstance) nodeInstance).getWorkItem(),
                        new HumanTaskTransition(((HumanTaskNodeInstance) nodeInstance).getWorkItem().getPhaseId()), null,
                        true);
                uow.intercept(WorkUnit.create(event, (e) -> {
                }));
                count++;
            }
        }

        for (ProcessInstance<? extends Model> subInstance : instance.subprocesses()) {

            count += indexProcessInstance(uow, subInstance.process(), subInstance);
        }

        return count;
    }

    @SuppressWarnings("unchecked")
    protected Optional<ProcessInstance<?>> loadProcessInstance(Process<?> process, String instanceId) {
        Optional<ProcessInstance<?>> instance = (Optional<ProcessInstance<?>>) process.instances().findById(instanceId,
                ProcessInstanceReadMode.READ_ONLY);

        if (instance.isEmpty()) {
            instance = (Optional<ProcessInstance<?>>) process.instances().findById(instanceId, ProcessInstance.STATE_ERROR,
                    ProcessInstanceReadMode.READ_ONLY);
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    protected Collection<ProcessInstance<?>> loadProcessInstances(Process<?> process) {
        Collection<ProcessInstance<?>> activeInstances = (Collection<ProcessInstance<?>>) process.instances()
                .values(ProcessInstanceReadMode.READ_ONLY, 1, Integer.MAX_VALUE);

        Collection<ProcessInstance<?>> errorIinstances = (Collection<ProcessInstance<?>>) process.instances()
                .values(ProcessInstanceReadMode.READ_ONLY, ProcessInstance.STATE_ERROR, 1, Integer.MAX_VALUE);

        Collection<ProcessInstance<?>> instances = new ArrayList<>();
        instances.addAll(activeInstances);
        instances.addAll(errorIinstances);

        return instances;
    }
}
