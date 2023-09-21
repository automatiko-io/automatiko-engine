package io.automatiko.engine.service.tracing;

import java.util.Collection;
import java.util.stream.Collectors;

import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.Tag;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class TracingAdds {

    @Inject
    Instance<Tracer> tracer;

    public void addTags(ProcessInstance<?> instance) {
        if (tracer.isResolvable()) {
            Span.current().setAttribute("workflow.instance.id", instance.id());
            Span.current().setAttribute("workflow.root.instance.id",
                    instance.rootProcessInstanceId() == null ? instance.id() : instance.rootProcessInstanceId());
            if (instance.businessKey() != null) {
                Span.current().setAttribute("workflow.business.key", instance.businessKey());
            }

            Collection<String> tags = instance.tags().values();
            if (!tags.isEmpty()) {
                Span.current().setAttribute("workflow.instance.tags",
                        tags.stream().collect(Collectors.joining(",")));
            }
        }
    }

    public void addTags(io.automatiko.engine.api.runtime.process.ProcessInstance instance) {
        if (tracer.isResolvable()) {
            Span.current().setAttribute("workflow.instance.id", instance.getId());
            Span.current().setAttribute("workflow.root.instance.id",
                    instance.getRootProcessInstanceId() == null ? instance.getId() : instance.getRootProcessInstanceId());
            if (instance.getCorrelationKey() != null) {
                Span.current().setAttribute("workflow.business.key", instance.getCorrelationKey());
            }
            Collection<Tag> tags = ((WorkflowProcessInstance) instance).getTags();
            if (!tags.isEmpty()) {
                Span.current().setAttribute("workflow.instance.tags",
                        tags.stream().map(t -> t.getValue())
                                .collect(Collectors.joining(",")));
            }
        }
    }
}
