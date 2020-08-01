package io.automatik.engine.quarkus.metrics;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;

import io.automatik.engine.api.event.process.DefaultProcessEventListener;
import io.automatik.engine.api.event.process.ProcessCompletedEvent;
import io.automatik.engine.api.event.process.ProcessNodeInstanceFailedEvent;
import io.automatik.engine.api.event.process.ProcessStartedEvent;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.metrics.MetricRegistries;

@IfBuildProperty(name = "quarkus.automatik.metrics.enabled", stringValue = "true")
@ApplicationScoped
public class ProcessMetricsProcessEventListener extends DefaultProcessEventListener {

	@ConfigProperty(name = "quarkus.application.name")
	String application;

	@ConfigProperty(name = "quarkus.application.version")
	String version;

	@Override
	public void afterProcessStarted(ProcessStartedEvent event) {
		final WorkflowProcessInstanceImpl processInstance = (WorkflowProcessInstanceImpl) event.getProcessInstance();
		Metadata startedPIMetadata = Metadata.builder().withName("automatik.process.started.count")
				.withDescription("Total count of started process instances").withType(MetricType.COUNTER).build();
		Counter counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(startedPIMetadata,
				new Tag("processId", processInstance.getProcessId()));
		counter.inc();

		ConcurrentGauge currentlyActive = MetricRegistries.get(MetricRegistry.Type.VENDOR).concurrentGauge(
				"automatik.process.current.active.count", new Tag("application", application),
				new Tag("version", version), new Tag("processId", processInstance.getProcessId()));
		currentlyActive.inc();
	}

	@Override
	public void afterProcessCompleted(ProcessCompletedEvent event) {

		final WorkflowProcessInstanceImpl processInstance = (WorkflowProcessInstanceImpl) event.getProcessInstance();

		Counter counter;
		if (processInstance.getState() == ProcessInstance.STATE_COMPLETED) {
			Metadata completedPIMetadata = Metadata.builder().withName("automatik.process.completed.count")
					.withDescription("Displays total count of completed process instances").withType(MetricType.COUNTER)
					.build();
			counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(completedPIMetadata,
					new Tag("application", application), new Tag("version", version),
					new Tag("processId", processInstance.getProcessId()));
		} else {
			Metadata abortedPIMetadata = Metadata.builder().withName("automatik.process.aborted.count")
					.withDescription("Displays total count of aborted process instances").withType(MetricType.COUNTER)
					.build();
			counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(abortedPIMetadata,
					new Tag("application", application), new Tag("version", version),
					new Tag("processId", processInstance.getProcessId()));
		}

		counter.inc();

		if (processInstance.getStartDate() != null) {
			Metadata processInstanceDurationMetadata = Metadata.builder()
					.withName("automatik.process.instances.duration")
					.withDescription("Displays duration of process instances - from start to completion")
					.withType(MetricType.SIMPLE_TIMER).withUnit(TimeUnit.MILLISECONDS.name()).build();

			SimpleTimer instanceDuration = MetricRegistries.get(MetricRegistry.Type.VENDOR).simpleTimer(
					processInstanceDurationMetadata, new Tag("application", application), new Tag("version", version),
					new Tag("processId", processInstance.getProcessId()));

			final long duration = millisToSeconds(
					processInstance.getEndDate().getTime() - processInstance.getStartDate().getTime());
			instanceDuration.update(Duration.ofMillis(duration));

		}

		Metadata currentlyActivePIMetadata = Metadata.builder().withName("automatik.process.current.active.count")
				.withDescription("Currently Active Process Instances").withType(MetricType.CONCURRENT_GAUGE).build();

		ConcurrentGauge currentlyActive = MetricRegistries.get(MetricRegistry.Type.VENDOR).concurrentGauge(
				currentlyActivePIMetadata, new Tag("application", application), new Tag("version", version),
				new Tag("processId", processInstance.getProcessId()));
		currentlyActive.dec();
	}

	@Override
	public void afterNodeInstanceFailed(ProcessNodeInstanceFailedEvent event) {

		final WorkflowProcessInstanceImpl processInstance = (WorkflowProcessInstanceImpl) event.getProcessInstance();

		Metadata erroredtMetadata = Metadata.builder().withName("automatik.process.errored.count").withDescription(
				"Displays total count of process instances that failed during execution - failure of given node")
				.withType(MetricType.COUNTER).build();

		Counter counter = MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(erroredtMetadata,
				new Tag("application", application), new Tag("version", version),
				new Tag("processId", processInstance.getProcessId()),
				new Tag("nodeName", event.getNodeInstance().getNodeName()));
		counter.inc();
	}

	protected static long millisToSeconds(long millis) {
		return millis / 1000;
	}
}
