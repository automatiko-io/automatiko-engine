package io.automatiko.engine.service.tracing;

import java.util.Collection;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.Tag;
import io.opentracing.Tracer;

@ApplicationScoped
public class TracingAdds {

    @Inject
    Instance<Tracer> tracer;

    public void addTags(ProcessInstance<?> instance) {
        if (tracer.isResolvable()) {
            Tracer tracerInstance = tracer.get();
            tracerInstance.activeSpan().setTag("workflow.instance.id", instance.id());
            tracerInstance.activeSpan().setTag("workflow.root.instance.id",
                    instance.rootProcessInstanceId() == null ? instance.id() : instance.rootProcessInstanceId());
            if (instance.businessKey() != null) {
                tracerInstance.activeSpan().setTag("workflow.business.key", instance.businessKey());
            }

            Collection<String> tags = instance.tags().values();
            if (!tags.isEmpty()) {
                tracerInstance.activeSpan().setTag("workflow.instance.tags",
                        tags.stream().collect(Collectors.joining(",")));
            }
        }
    }

    public void addTags(io.automatiko.engine.api.runtime.process.ProcessInstance instance) {
        if (tracer.isResolvable()) {
            Tracer tracerInstance = tracer.get();
            tracerInstance.activeSpan().setTag("workflow.instance.id", instance.getId());
            tracerInstance.activeSpan().setTag("workflow.root.instance.id",
                    instance.getRootProcessInstanceId() == null ? instance.getId() : instance.getRootProcessInstanceId());
            if (instance.getCorrelationKey() != null) {
                tracerInstance.activeSpan().setTag("workflow.business.key", instance.getCorrelationKey());
            }
            Collection<Tag> tags = ((WorkflowProcessInstance) instance).getTags();
            if (!tags.isEmpty()) {
                tracerInstance.activeSpan().setTag("workflow.instance.tags",
                        tags.stream().map(t -> t.getValue())
                                .collect(Collectors.joining(",")));
            }
        }
    }
}
