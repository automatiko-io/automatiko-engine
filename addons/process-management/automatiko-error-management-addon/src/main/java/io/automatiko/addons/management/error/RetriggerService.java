package io.automatiko.addons.management.error;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.ServiceExecutionError;

@ApplicationScoped
public class RetriggerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetriggerService.class);

    @Inject
    private Instance<Process<?>> processes;

    @SuppressWarnings("unchecked")
    public void retriggerFailedInstance(String processId, String instanceId, String nodeId) {

        Optional<Process<?>> process = processes.stream().filter(p -> p.id().equalsIgnoreCase(processId)).findFirst();

        if (process.isPresent()) {

            Optional<ProcessInstance<?>> instance = (Optional<ProcessInstance<?>>) process.get().instances()
                    .findById(instanceId, ProcessInstance.STATE_ERROR, ProcessInstanceReadMode.MUTABLE);

            if (instance.isPresent() && instance.get().status() == ProcessInstance.STATE_ERROR) {
                instance.get().errors().get().retrigger(nodeId);

                if (instance.get().status() == ProcessInstance.STATE_ERROR
                        && instance.get().errors().get().failedNodeIds().contains(nodeId)) {
                    LOGGER.debug("Failed at recovering process instance {} from an error state on node {}", instanceId, nodeId);
                    throw new ServiceExecutionError("500");
                }
                LOGGER.debug("Successfully recovered process instance {} from an error state on node {}", instanceId, nodeId);
            } else {
                LOGGER.debug("Unable to find process instance for {} (process {}), no retrigger attempt was made", processId,
                        instanceId);
            }
        } else {
            LOGGER.debug("Unable to find process for {}, no retrigger attempt was made", processId);
        }
    }

}
