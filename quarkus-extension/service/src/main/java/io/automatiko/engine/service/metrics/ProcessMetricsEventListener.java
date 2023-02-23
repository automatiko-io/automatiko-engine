package io.automatiko.engine.service.metrics;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.api.event.process.DefaultProcessEventListener;
import io.automatiko.engine.api.event.process.ProcessCompletedEvent;
import io.automatiko.engine.api.event.process.ProcessNodeInstanceFailedEvent;
import io.automatiko.engine.api.event.process.ProcessSignaledEvent;
import io.automatiko.engine.api.event.process.ProcessStartedEvent;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.quarkus.arc.properties.IfBuildProperty;

@IfBuildProperty(name = "quarkus.automatiko.metrics.enabled", stringValue = "true")
@ApplicationScoped
public class ProcessMetricsEventListener extends DefaultProcessEventListener {

    @ConfigProperty(name = "quarkus.application.name", defaultValue = "")
    Optional<String> application;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "")
    Optional<String> version;

    @Inject
    MeterRegistry registry;

    @Override
    public void afterProcessStarted(ProcessStartedEvent event) {
        final WorkflowProcessInstanceImpl processInstance = (WorkflowProcessInstanceImpl) event.getProcessInstance();
        //"Total count of started process instances"
        Counter counter = registry.counter("automatiko.process.started.count",
                Arrays.asList(Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                        Tag.of("processId", processInstance.getProcessId()),
                        Tag.of("processVersion",
                                processInstance.getProcess().getVersion() == null ? "unknown"
                                        : processInstance.getProcess().getVersion())));
        counter.increment();
    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {

        final WorkflowProcessInstanceImpl processInstance = (WorkflowProcessInstanceImpl) event.getProcessInstance();

        Counter counter;
        if (processInstance.getState() == ProcessInstance.STATE_COMPLETED) {
            //Displays total count of completed process instances
            counter = registry.counter("automatiko.process.completed.count", Arrays.asList(
                    Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                    Tag.of("processId", processInstance.getProcessId()),
                    Tag.of("processVersion",
                            processInstance.getProcess().getVersion() == null ? "unknown"
                                    : processInstance.getProcess().getVersion())));
        } else {
            //Displays total count of aborted process instances
            counter = registry.counter("automatiko.process.aborted.count", Arrays.asList(
                    Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                    Tag.of("processId", processInstance.getProcessId()),
                    Tag.of("processVersion",
                            processInstance.getProcess().getVersion() == null ? "unknown"
                                    : processInstance.getProcess().getVersion())));
        }

        counter.increment();

        if (processInstance.getStartDate() != null) {
            //Displays duration of process instances - from start to completion
            Timer instanceDuration = registry.timer(
                    "automatiko.process.instances.duration", Arrays.asList(Tag.of("application", application.orElse("")),
                            Tag.of("version", version.orElse("")),
                            Tag.of("processId", processInstance.getProcessId()),
                            Tag.of("processVersion",
                                    processInstance.getProcess().getVersion() == null ? "unknown"
                                            : processInstance.getProcess().getVersion())));

            final long duration = millisToSeconds(
                    processInstance.getEndDate().getTime() - processInstance.getStartDate().getTime());
            instanceDuration.record(Duration.ofSeconds(duration));

        }

    }

    @Override
    public void afterNodeInstanceFailed(ProcessNodeInstanceFailedEvent event) {

        final WorkflowProcessInstanceImpl processInstance = (WorkflowProcessInstanceImpl) event.getProcessInstance();

        //Displays total count of process instances that failed during execution - failure of given node
        Counter counter = registry.counter("automatiko.process.errored.count", Arrays.asList(
                Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                Tag.of("processId", processInstance.getProcessId()),
                Tag.of("processVersion",
                        processInstance.getProcess().getVersion() == null ? "unknown"
                                : processInstance.getProcess().getVersion()),
                Tag.of("nodeName", event.getNodeInstance().getNodeName())));
        counter.increment();
    }

    @Override
    public void afterProcessSignaled(ProcessSignaledEvent event) {
        final WorkflowProcessInstanceImpl processInstance = (WorkflowProcessInstanceImpl) event.getProcessInstance();
        //Displays total count of process instance's signals
        Counter counter = registry.counter("automatiko.process.signals.count", Arrays.asList(
                Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                Tag.of("processId", processInstance.getProcessId()),
                Tag.of("processVersion",
                        processInstance.getProcess().getVersion() == null ? "unknown"
                                : processInstance.getProcess().getVersion()),
                Tag.of("signal", event.getSignal())));
        counter.increment();
    }

    protected static long millisToSeconds(long millis) {
        return millis / 1000;
    }
}
