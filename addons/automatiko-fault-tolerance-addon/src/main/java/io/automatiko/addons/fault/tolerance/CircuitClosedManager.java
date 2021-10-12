package io.automatiko.addons.fault.tolerance;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.addons.fault.tolerance.internal.AutomatikoStrategyCache;
import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.services.uow.UnitOfWorkExecutor;
import io.smallrye.faulttolerance.internal.StrategyCache;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class CircuitClosedManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CircuitClosedManager.class);

    private Map<String, Process<?>> processData = new LinkedHashMap<String, Process<?>>();

    private Application application;

    private StrategyCache cache;

    @Inject
    public CircuitClosedManager(Application application, Instance<Process<?>> availableProcesses,
            StrategyCache cache) {
        this.processData = availableProcesses == null ? Collections.emptyMap()
                : availableProcesses.stream().collect(Collectors.toMap(p -> p.id(), p -> p));
        this.application = application;
        this.cache = cache;
    }

    public void onCircuitClosed(@Observes CircuitClosedEvent event) {
        retriggerErroredInstances(event.getName());
    }

    public Set<CircuitBrakerDTO> info() {
        Set<CircuitBrakerDTO> info = new HashSet<>();
        for (String name : ((AutomatikoStrategyCache) cache).circuitBreakerNames()) {
            int counter = 0;
            for (Process<?> process : processData.values()) {

                Collection<String> matching = process.instances().locateByIdOrTag(ProcessInstance.STATE_ERROR, name);
                counter += matching.size();
            }
            info.add(new CircuitBrakerDTO(name, counter));
        }

        return info;
    }

    @SuppressWarnings("unchecked")
    @Asynchronous
    public Uni<Void> retriggerErroredInstances(String errorName) {
        LOGGER.info("Circuit '{}' has been closed, locating instances affected and retriggering...", errorName);
        for (Process<?> process : processData.values()) {

            Collection<String> matching = process.instances().locateByIdOrTag(ProcessInstance.STATE_ERROR, errorName);
            for (String instanceId : matching) {
                try {
                    LOGGER.debug("Retriggering {} instance that failed due to {}", instanceId, errorName);
                    UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                        Optional<ProcessInstance<?>> loaded = (Optional<ProcessInstance<?>>) process.instances()
                                .findById(instanceId, ProcessInstance.STATE_ERROR, ProcessInstanceReadMode.MUTABLE);

                        if (loaded.isPresent()) {
                            loaded.get().errors().get().errors().stream().filter(e -> e.errorDetails().equals(errorName))
                                    .forEach(error -> error.retrigger());
                            LOGGER.debug("{} instance retrigged and ended in status {}", instanceId, loaded.get().status());
                        }

                        return null;
                    });
                } catch (CircuitBreakerOpenException e) {
                    if (e.getMessage().contains(errorName)) {
                        LOGGER.warn(
                                "Retrigger of instance {} resulted in another circuit breaker excetion of the same type {}, sopping retriggering",
                                instanceId, errorName);
                        return Uni.createFrom().nullItem();
                    }
                } catch (Throwable e) {
                    LOGGER.warn("Retrigger of instance {} resulted in exception {}", instanceId, e.getMessage());
                }
            }
        }

        return Uni.createFrom().nullItem();
    }
}
