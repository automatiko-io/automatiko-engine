package io.automatiko.engine.service.metrics;

import java.util.Arrays;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.api.definition.process.Process;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

@ApplicationScoped
public class ProcessMessagingMetrics {

    @ConfigProperty(name = "quarkus.application.name", defaultValue = "")
    Optional<String> application;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "")
    Optional<String> version;

    @ConfigProperty(name = "quarkus.automatiko.metrics.enabled")
    Optional<Boolean> enabled;

    @Inject
    Instance<MeterRegistry> registry;

    public void messageReceived(String connector, String message, Process process) {
        if (!enabled.orElse(false)) {
            return;
        }

        //"Displays total count of received messages on given process"
        Counter counter = registry.get().counter("automatiko.process.messages.received.count",
                Arrays.asList(Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                        Tag.of("processId", process.getId()),
                        Tag.of("processName", nonNull(process.getName())),
                        Tag.of("processVersion", nonNull(process.getVersion())),
                        Tag.of("message", message),
                        Tag.of("connector", connector)));
        counter.increment();
    }

    public void messageConsumed(String connector, String message, Process process) {
        if (!enabled.orElse(false)) {
            return;
        }

        //"Displays total count of consumed messages on given process"
        Counter counter = registry.get().counter("automatiko.process.messages.consumed.count",
                Arrays.asList(Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                        Tag.of("processId", process.getId()),
                        Tag.of("processName", nonNull(process.getName())),
                        Tag.of("processVersion", nonNull(process.getVersion())),
                        Tag.of("message", message),
                        Tag.of("connector", connector)));
        counter.increment();
    }

    public void messageMissed(String connector, String message, Process process) {
        if (!enabled.orElse(false)) {
            return;
        }

        // "Displays total count of missed (received but not consumed) messages on given process"
        Counter counter = registry.get().counter("automatiko.process.messages.missed.count",
                Arrays.asList(Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                        Tag.of("processId", process.getId()),
                        Tag.of("processName", nonNull(process.getName())),
                        Tag.of("processVersion", nonNull(process.getVersion())),
                        Tag.of("message", message),
                        Tag.of("connector", connector)));
        counter.increment();
    }

    public void messageRejected(String connector, String message, Process process) {
        if (!enabled.orElse(false)) {
            return;
        }

        // "Displays total count of rejected (received but rejected by filter expression) messages on given process"
        Counter counter = registry.get().counter("automatiko.process.messages.rejected.count",
                Arrays.asList(Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                        Tag.of("processId", process.getId()),
                        Tag.of("processName", nonNull(process.getName())),
                        Tag.of("processVersion", nonNull(process.getVersion())),
                        Tag.of("message", message),
                        Tag.of("connector", connector)));
        counter.increment();
    }

    public void messageFailed(String connector, String message, Process process) {
        if (!enabled.orElse(false)) {
            return;
        }

        //"Displays total count of failed (received but failed at consuming) messages on given process"
        Counter counter = registry.get().counter("automatiko.process.messages.failed.count",
                Arrays.asList(Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                        Tag.of("processId", process.getId()),
                        Tag.of("processName", nonNull(process.getName())),
                        Tag.of("processVersion", nonNull(process.getVersion())),
                        Tag.of("message", message),
                        Tag.of("connector", connector)));
        counter.increment();
    }

    public void messageProduced(String connector, String message, Process process) {
        if (!enabled.orElse(false)) {
            return;
        }

        //"Displays total count of produced messages from given process"
        Counter counter = registry.get().counter("automatiko.process.messages.produced.count",
                Arrays.asList(Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                        Tag.of("processId", process.getId()),
                        Tag.of("processName", nonNull(process.getName())),
                        Tag.of("processVersion", nonNull(process.getVersion())),
                        Tag.of("message", message),
                        Tag.of("connector", connector)));
        counter.increment();
    }

    public void messageProducedFailure(String connector, String message, Process process) {
        if (!enabled.orElse(false)) {
            return;
        }

        //"Displays total count of produced messages from given process"
        Counter counter = registry.get().counter("automatiko.process.messages.produced.failed.count",
                Arrays.asList(Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                        Tag.of("processId", process.getId()),
                        Tag.of("processName", nonNull(process.getName())),
                        Tag.of("processVersion", nonNull(process.getVersion())),
                        Tag.of("message", message),
                        Tag.of("connector", connector)));
        counter.increment();
    }

    private String nonNull(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }
}
