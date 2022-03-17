package io.automatiko.engine.addons.persistence.common.tlog;

import java.util.Optional;
import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.uow.TransactionLog;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.services.uow.UnitOfWorkExecutor;
import io.automatiko.engine.workflow.AbstractProcess;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class TransactionLogRecovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionLogRecovery.class);

    @Inject
    private Application application;

    @Inject
    private Instance<Process<?>> processes;

    public void recoverOnStart(@Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) StartupEvent event) {
        recovery();
    }

    public void recovery() {

        for (Process<?> process : processes) {

            if (!WorkflowProcess.PUBLIC_VISIBILITY
                    .equals(((WorkflowProcess) ((AbstractProcess<?>) process).process()).getVisibility())) {
                continue;
            }

            recoverByProcess(process);
        }
    }

    @SuppressWarnings("unchecked")
    protected void recoverByProcess(Process<?> process) {
        TransactionLog transactionLog = process.instances().transactionLog();
        if (transactionLog != null && transactionLog.requiresRecovery()) {
            LOGGER.info("Transaction recovery required for process '{}'", process.id());
            Set<String> recoverableInstances = transactionLog.recoverable(process.id());
            LOGGER.info("Checking process '{}' for recoverable instances, found {}", process.id(), recoverableInstances);
            if (recoverableInstances != null) {
                for (String instanceInfo : recoverableInstances) {
                    String[] elements = instanceInfo.split("\\|");
                    String instanceId = elements[1];
                    try {

                        boolean recovered = UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {

                            Optional<ProcessInstance<?>> found = (Optional<ProcessInstance<?>>) process.instances()
                                    .findById(instanceId, ProcessInstance.STATE_RECOVERING, ProcessInstanceReadMode.MUTABLE);
                            if (found.isPresent()) {
                                ProcessInstance<?> pi = found.get();
                                if (pi.errors().isPresent()) {
                                    LOGGER.debug("Recovering instance '{}' from process '{}'", instanceId, process.id());
                                    pi.errors().get().retrigger();
                                    LOGGER.info("Successfully recovered instance '{}' from process '{}'", instanceId,
                                            process.id());

                                    return true;
                                } else {
                                    LOGGER.warn(
                                            "Recovering instance '{}' from process '{}' cannot be completed due to missing node information",
                                            instanceId, process.id());
                                }
                            } else {
                                LOGGER.warn("Recovering instance '{}' from process '{}' failed at finding process instance",
                                        instanceId, process.id());
                            }

                            return false;
                        });

                        if (recovered) {
                            transactionLog.complete(elements[0], process.id(), instanceId);
                        }

                        if (process.subprocesses() != null) {

                            for (Process<?> sProcess : process.subprocesses()) {
                                recoverByProcess(sProcess);
                            }
                        }
                    } catch (Throwable e) {
                        LOGGER.warn("Recovery of instance '{}' resulted in exception '{}'", instanceId, e.getMessage());
                    }
                }
            }
        }
    }
}
