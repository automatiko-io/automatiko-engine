package io.automatiko.addons.management.error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.event.process.DefaultProcessEventListener;
import io.automatiko.engine.api.event.process.ProcessNodeInstanceFailedEvent;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatiko.engine.api.workflow.ServiceExecutionError;

@ApplicationScoped
public class InitiateErrorManagementProcessEventListener extends DefaultProcessEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitiateErrorManagementProcessEventListener.class);

    private Process<? extends Model> handler;

    private Optional<String> configuredDelay;

    private Optional<Integer> maxIncrementAttepts;

    private List<String> excludedPackages = new ArrayList<>();

    private List<String> ignoredErrorCodes = new ArrayList<>();

    @Inject
    public InitiateErrorManagementProcessEventListener(@Named("atkErrorRecovery") Process<? extends Model> handler,
            @ConfigProperty(name = "quarkus.automatiko.error-recovery.delay") Optional<String> configuredDelay,
            @ConfigProperty(name = "quarkus.automatiko.error-recovery.excluded") Optional<String> excluded,
            @ConfigProperty(name = "quarkus.automatiko.error-recovery.max-increment-attempts") Optional<Integer> maxIncrementAttepts,
            @ConfigProperty(name = "quarkus.automatiko.error-recovery.ignored-error-codes") Optional<String> ignoredErrorCode) {
        this.handler = handler;
        this.configuredDelay = configuredDelay;
        if (excluded.isPresent()) {
            this.excludedPackages = List.of(excluded.get().split(","));
        }
        this.maxIncrementAttepts = maxIncrementAttepts;
        if (ignoredErrorCode.isPresent()) {
            this.ignoredErrorCodes = List.of(ignoredErrorCode.get().split(","));
            ;
        }
    }

    @Override
    public void afterNodeInstanceFailed(ProcessNodeInstanceFailedEvent e) {

        if (e.getProcessInstance().getProcessId().equals("atkErrorRecovery")
                || isExcluded(e.getProcessInstance().getProcess().getPackageName())) {
            LOGGER.debug("Failure happened in internal process instance or it was excluded by package");
            return;
        }

        if (ignoreErrorCode(e.getException())) {
            LOGGER.debug("Error code ({}) has been configured to be excluded",
                    ((ServiceExecutionError) e.getException()).getErrorCode());
            return;
        }

        String instanceId = e.getProcessInstance().getId();
        if (e.getProcessInstance().getParentProcessInstanceId() != null
                && !e.getProcessInstance().getParentProcessInstanceId().isEmpty()) {
            instanceId = e.getProcessInstance().getParentProcessInstanceId() + ":" + instanceId;
        }
        LOGGER.warn("Execution error for instance {} of process {} for node {}, startign automatic recovery",
                e.getProcessInstance().getId(),
                e.getProcessInstance().getProcessId(), e.getNodeInstance().getNodeName());
        Map<String, Object> params = new HashMap<>();
        params.put("processId",
                e.getProcessInstance().getProcessId() + version(e.getProcessInstance().getProcess().getVersion()));
        params.put("instanceId", instanceId);
        params.put("nodeId", e.getNodeInstance().getNodeDefinitionId());
        params.put("delay", configuredDelay.orElse("PT30S"));
        params.put("maxAttempts", maxIncrementAttepts.orElse(10));

        Model model = handler.createModel();
        model.fromMap(params);

        String businessKey = e.getProcessInstance().getId() + ":" + e.getNodeInstance().getNodeDefinitionId();
        try {
            ProcessInstance<?> instance = handler.createInstance(businessKey, model);

            instance.start();
        } catch (ProcessInstanceDuplicatedException ex) {
            LOGGER.debug("There is already running recovery instance, skipping");
        }
    }

    private boolean isExcluded(String packageName) {
        if (excludedPackages.isEmpty() || packageName == null) {
            return false;
        }

        if (excludedPackages.contains(packageName)) {
            return true;
        }
        return false;
    }

    private boolean ignoreErrorCode(Exception ex) {
        if (ignoredErrorCodes.isEmpty() || !(ex instanceof ServiceExecutionError)) {
            return false;
        }

        if (excludedPackages.contains(((ServiceExecutionError) ex).getErrorCode())) {
            return true;
        }
        return false;
    }

    private String version(String version) {
        if (version != null && !version.trim().isEmpty()) {
            return "_" + version.replaceAll("\\.", "_");
        }
        return "";
    }
}
