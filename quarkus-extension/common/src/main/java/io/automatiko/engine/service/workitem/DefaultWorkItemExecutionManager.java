package io.automatiko.engine.service.workitem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.TrustedIdentityProvider;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemManager;
import io.automatiko.engine.api.uow.WorkUnit;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionManager;
import io.automatiko.engine.services.uow.UnitOfWorkExecutor;
import io.automatiko.engine.services.utils.StringUtils;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class DefaultWorkItemExecutionManager implements WorkItemExecutionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWorkItemExecutionManager.class);

    private Map<String, Process<?>> processData = new LinkedHashMap<String, Process<?>>();

    private Application application;

    @Inject
    public DefaultWorkItemExecutionManager(Application application, Instance<Process<?>> availableProcesses) {
        this.processData = availableProcesses == null ? Collections.emptyMap()
                : availableProcesses.stream().collect(Collectors.toMap(p -> p.id(), p -> p));
        this.application = application;

    }

    @Override
    public void complete(String processId, String name, WorkItem workItem, WorkItemManager manager, Object source,
            Function<Throwable, Throwable> errorMapper) {

        if (source instanceof CompletionStage) {

            application.unitOfWorkManager().currentUnitOfWork().intercept(
                    create(Uni.createFrom().completionStage((CompletionStage<?>) source), processId, name, workItem, manager,
                            errorMapper));
        } else if (source instanceof Uni) {
            application.unitOfWorkManager().currentUnitOfWork()
                    .intercept(create((Uni<?>) source, processId, name, workItem, manager, errorMapper));

        } else {
            manager.completeWorkItem(workItem.getId(), name == null ? null : Collections.singletonMap(name, source));
        }

    }

    @SuppressWarnings("unchecked")
    protected void success(String processId, String name, WorkItem workItem, WorkItemManager manager, Object value) {
        LOGGER.debug("Executing success callback after work item execution");
        Process<?> process = processData.get(processId);

        if (process == null) {
            LOGGER.error("Unable to find process with id {}, completion of service invocation aborted", processId);
            return;
        }

        IdentityProvider.set(new TrustedIdentityProvider("System<async>"));
        UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            String id = StringUtils.isEmpty(workItem.getParentProcessInstanceId()) ? workItem.getProcessInstanceId()
                    : workItem.getParentProcessInstanceId() + ":" + workItem.getProcessInstanceId();
            Optional<ProcessInstance<?>> instance = (Optional<ProcessInstance<?>>) process.instances()
                    .findById(id);

            if (instance.isPresent()) {
                instance.get().completeWorkItem(workItem.getId(), name == null ? null : Collections.singletonMap(name, value));
            }

            return null;
        });

    }

    @SuppressWarnings("unchecked")
    protected void error(String processId, WorkItem workItem, WorkItemManager manager, Throwable error) {
        LOGGER.debug("Executing error callback after work item execution");
        Process<?> process = processData.get(processId);

        if (process == null) {
            LOGGER.error("Unable to find process with id {}, completion of service invocation aborted", processId);
            return;
        }
        try {
            IdentityProvider.set(new TrustedIdentityProvider("System<async>"));
            UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {

                String id = StringUtils.isEmpty(workItem.getParentProcessInstanceId()) ? workItem.getProcessInstanceId()
                        : workItem.getParentProcessInstanceId() + ":" + workItem.getProcessInstanceId();
                Optional<ProcessInstance<?>> instance = (Optional<ProcessInstance<?>>) process.instances()
                        .findById(id);

                if (instance.isPresent()) {
                    instance.get().failWorkItem(workItem.getId(), error);
                }

                return null;
            });
        } catch (Throwable e) {

        }

    }

    private WorkUnit<Uni<?>> create(Uni<?> data, String processId, String name, WorkItem workItem, WorkItemManager manager,
            Function<Throwable, Throwable> errorMapper) {
        return new WorkUnit<Uni<?>>() {

            @Override
            public Uni<?> data() {
                return data;
            }

            @Override
            public void perform() {
                data().subscribe().with(
                        v -> success(processId, name, workItem, manager, v),
                        err -> error(processId, workItem, manager, errorMapper.apply(err)));
            }

            @Override
            public Integer priority() {
                return 50000;
            }
        };
    }

}
