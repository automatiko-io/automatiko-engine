package io.automatiko.engine.service.metrics;

import java.util.Arrays;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kie.dmn.api.core.event.AfterEvaluateDecisionEvent;
import org.kie.dmn.api.core.event.AfterEvaluateDecisionServiceEvent;
import org.kie.dmn.api.core.event.AfterEvaluateDecisionTableEvent;
import org.kie.dmn.api.core.event.DMNRuntimeEventListener;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.quarkus.arc.properties.IfBuildProperty;

@IfBuildProperty(name = "quarkus.automatiko.metrics.enabled", stringValue = "true")
@ApplicationScoped
public class DecisionMetricsEventListener implements DMNRuntimeEventListener {

    @ConfigProperty(name = "quarkus.application.name")
    Optional<String> application;

    @ConfigProperty(name = "quarkus.application.version")
    Optional<String> version;

    MeterRegistry registry;

    @Override
    public void afterEvaluateDecision(AfterEvaluateDecisionEvent event) {
        //"Total count of evaluated decision"
        Counter counter = registry.counter("automatiko.decision.evaluated.count",
                Arrays.asList(Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                        Tag.of("decision", event.getDecision().getName()),
                        Tag.of("model", event.getDecision().getModelName()),
                        Tag.of("namespace", event.getDecision().getModelNamespace()),
                        Tag.of("errors", Boolean.toString(event.getResult().hasErrors()))));
        counter.increment();
    }

    @Override
    public void afterEvaluateDecisionTable(AfterEvaluateDecisionTableEvent event) {

        // "Total count of evaluated decision tables"
        Counter counter = registry.counter("automatiko.decision-table.evaluated.count",
                Arrays.asList(Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                        Tag.of("decisionTable", event.getDecisionTableName()), Tag.of("node", event.getNodeName()),
                        Tag.of("errors", Boolean.toString(event.getResult().hasErrors()))));
        counter.increment();
    }

    @Override
    public void afterEvaluateDecisionService(AfterEvaluateDecisionServiceEvent event) {
        //"Total count of evaluated decision services"
        Counter counter = registry.counter("automatiko.decision-service.evaluated.count",
                Arrays.asList(Tag.of("application", application.orElse("")), Tag.of("version", version.orElse("")),
                        Tag.of("decision", event.getDecisionService().getName()),
                        Tag.of("model", event.getDecisionService().getModelName()),
                        Tag.of("namespace", event.getDecisionService().getModelNamespace()),
                        Tag.of("errors", Boolean.toString(event.getResult().hasErrors()))));
        counter.increment();
    }

}
