package io.automatiko.engine.service.metrics;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import io.automatiko.engine.api.definition.process.Process;
import io.smallrye.metrics.MetricRegistries;

@ApplicationScoped
public class ProcessMessagingMetrics {

    @ConfigProperty(name = "quarkus.application.name", defaultValue = "")
    Optional<String> application;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "")
    Optional<String> version;

    @ConfigProperty(name = "quarkus.automatiko.metrics.enabled")
    Optional<Boolean> enabled;

    public void messageReceived(String connector, String message, Process process) {
        if (!enabled.orElse(false)) {
            return;
        }

        Metadata messageMetadata = Metadata.builder().withName("automatiko.process.messages.received.count").withDescription(
                "Displays total count of received messages on given process")
                .withType(MetricType.COUNTER).build();

        Counter counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(messageMetadata,
                new Tag("application", application.orElse("")), new Tag("version", version.orElse("")),
                new Tag("processId", process.getId()),
                new Tag("processName", nonNull(process.getName())),
                new Tag("processVersion", nonNull(process.getVersion())),
                new Tag("message", message),
                new Tag("connector", connector));
        counter.inc();
    }

    public void messageConsumed(String connector, String message, Process process) {
        if (!enabled.orElse(false)) {
            return;
        }
        Metadata messageMetadata = Metadata.builder().withName("automatiko.process.messages.consumed.count").withDescription(
                "Displays total count of consumed messages on given process")
                .withType(MetricType.COUNTER).build();

        Counter counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(messageMetadata,
                new Tag("application", application.orElse("")), new Tag("version", version.orElse("")),
                new Tag("processId", process.getId()),
                new Tag("processName", nonNull(process.getName())),
                new Tag("processVersion", nonNull(process.getVersion())),
                new Tag("message", message),
                new Tag("connector", connector));
        counter.inc();
    }

    public void messageMissed(String connector, String message, Process process) {
        if (!enabled.orElse(false)) {
            return;
        }
        Metadata messageMetadata = Metadata.builder().withName("automatiko.process.messages.missed.count").withDescription(
                "Displays total count of missed (received but not consumed) messages on given process")
                .withType(MetricType.COUNTER).build();

        Counter counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(messageMetadata,
                new Tag("application", application.orElse("")), new Tag("version", version.orElse("")),
                new Tag("processId", process.getId()),
                new Tag("processName", nonNull(process.getName())),
                new Tag("processVersion", nonNull(process.getVersion())),
                new Tag("message", message),
                new Tag("connector", connector));
        counter.inc();
    }

    public void messageFailed(String connector, String message, Process process) {
        if (!enabled.orElse(false)) {
            return;
        }
        Metadata messageMetadata = Metadata.builder().withName("automatiko.process.messages.failed.count").withDescription(
                "Displays total count of failed (received but failed at consuming) messages on given process")
                .withType(MetricType.COUNTER).build();

        Counter counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(messageMetadata,
                new Tag("application", application.orElse("")), new Tag("version", version.orElse("")),
                new Tag("processId", process.getId()),
                new Tag("processName", nonNull(process.getName())),
                new Tag("processVersion", nonNull(process.getVersion())),
                new Tag("message", message),
                new Tag("connector", connector));
        counter.inc();
    }

    public void messageProduced(String connector, String message, Process process, String processInstance, String businessKey) {
        if (!enabled.orElse(false)) {
            return;
        }
        Metadata messageMetadata = Metadata.builder().withName("automatiko.process.messages.produced.count").withDescription(
                "Displays total count of produced messages from given process")
                .withType(MetricType.COUNTER).build();

        Counter counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(messageMetadata,
                new Tag("application", application.orElse("")), new Tag("version", version.orElse("")),
                new Tag("processId", process.getId()),
                new Tag("processName", nonNull(process.getName())),
                new Tag("processVersion", nonNull(process.getVersion())),
                new Tag("processInstance", processInstance),
                new Tag("businessKey", businessKey),
                new Tag("message", message),
                new Tag("connector", connector));
        counter.inc();
    }

    private String nonNull(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }
}
