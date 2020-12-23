package io.automatiko.engine.quarkus.metrics;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.kie.dmn.api.core.event.AfterEvaluateDecisionEvent;
import org.kie.dmn.api.core.event.AfterEvaluateDecisionServiceEvent;
import org.kie.dmn.api.core.event.AfterEvaluateDecisionTableEvent;
import org.kie.dmn.api.core.event.DMNRuntimeEventListener;

import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.metrics.MetricRegistries;

@IfBuildProperty(name = "quarkus.automatiko.metrics.enabled", stringValue = "true")
@ApplicationScoped
public class DecisionMetricsEventListener implements DMNRuntimeEventListener {

    @ConfigProperty(name = "quarkus.application.name")
    Optional<String> application;

    @ConfigProperty(name = "quarkus.application.version")
    Optional<String> version;

    @Override
    public void afterEvaluateDecision(AfterEvaluateDecisionEvent event) {
        Metadata evaluateDecisionMetadata = Metadata.builder().withName("automatiko.decision.evaluated.count")
                .withDescription("Total count of evaluated decision").withType(MetricType.COUNTER).build();
        Counter counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(evaluateDecisionMetadata,
                new Tag("application", application.orElse("")), new Tag("version", version.orElse("")),
                new Tag("decision", event.getDecision().getName()),
                new Tag("model", event.getDecision().getModelName()),
                new Tag("namespace", event.getDecision().getModelNamespace()),
                new Tag("errors", Boolean.toString(event.getResult().hasErrors())));
        counter.inc();
    }

    @Override
    public void afterEvaluateDecisionTable(AfterEvaluateDecisionTableEvent event) {
        Metadata evaluateDecisionTableMetadata = Metadata.builder().withName("automatiko.decision-table.evaluated.count")
                .withDescription("Total count of evaluated decision tables").withType(MetricType.COUNTER).build();
        Counter counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(evaluateDecisionTableMetadata,
                new Tag("application", application.orElse("")), new Tag("version", version.orElse("")),
                new Tag("decisionTable", event.getDecisionTableName()), new Tag("node", event.getNodeName()),
                new Tag("errors", Boolean.toString(event.getResult().hasErrors())));
        counter.inc();
    }

    @Override
    public void afterEvaluateDecisionService(AfterEvaluateDecisionServiceEvent event) {
        Metadata evaluateDecisionMetadata = Metadata.builder().withName("automatiko.decision-service.evaluated.count")
                .withDescription("Total count of evaluated decision services").withType(MetricType.COUNTER).build();
        Counter counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(evaluateDecisionMetadata,
                new Tag("application", application.orElse("")), new Tag("version", version.orElse("")),
                new Tag("decision", event.getDecisionService().getName()),
                new Tag("model", event.getDecisionService().getModelName()),
                new Tag("namespace", event.getDecisionService().getModelNamespace()),
                new Tag("errors", Boolean.toString(event.getResult().hasErrors())));
        counter.inc();
    }

}
