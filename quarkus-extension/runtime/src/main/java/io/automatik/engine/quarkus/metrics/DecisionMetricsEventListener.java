package io.automatik.engine.quarkus.metrics;

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

@IfBuildProperty(name = "quarkus.automatik.metrics.enabled", stringValue = "true")
@ApplicationScoped
public class DecisionMetricsEventListener implements DMNRuntimeEventListener {

	@ConfigProperty(name = "quarkus.application.name")
	String application;

	@ConfigProperty(name = "quarkus.application.version")
	String version;

	@Override
	public void afterEvaluateDecision(AfterEvaluateDecisionEvent event) {
		Metadata evaluateDecisionMetadata = Metadata.builder().withName("automatik.decision.evaluated.count")
				.withDescription("Total count of evaluated decision").withType(MetricType.COUNTER).build();
		Counter counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(evaluateDecisionMetadata,
				new Tag("application", application), new Tag("version", version),
				new Tag("decision", event.getDecision().getName()),
				new Tag("model", event.getDecision().getModelName()),
				new Tag("namespace", event.getDecision().getModelNamespace()),
				new Tag("errors", Boolean.toString(event.getResult().hasErrors())));
		counter.inc();
	}

	@Override
	public void afterEvaluateDecisionTable(AfterEvaluateDecisionTableEvent event) {
		Metadata evaluateDecisionTableMetadata = Metadata.builder().withName("automatik.decision-table.evaluated.count")
				.withDescription("Total count of evaluated decision tables").withType(MetricType.COUNTER).build();
		Counter counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(evaluateDecisionTableMetadata,
				new Tag("application", application), new Tag("version", version),
				new Tag("decisionTable", event.getDecisionTableName()), new Tag("node", event.getNodeName()),
				new Tag("errors", Boolean.toString(event.getResult().hasErrors())));
		counter.inc();
	}

	@Override
	public void afterEvaluateDecisionService(AfterEvaluateDecisionServiceEvent event) {
		Metadata evaluateDecisionMetadata = Metadata.builder().withName("automatik.decision-service.evaluated.count")
				.withDescription("Total count of evaluated decision services").withType(MetricType.COUNTER).build();
		Counter counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(evaluateDecisionMetadata,
				new Tag("application", application), new Tag("version", version),
				new Tag("decision", event.getDecisionService().getName()),
				new Tag("model", event.getDecisionService().getModelName()),
				new Tag("namespace", event.getDecisionService().getModelNamespace()),
				new Tag("errors", Boolean.toString(event.getResult().hasErrors())));
		counter.inc();
	}

}
