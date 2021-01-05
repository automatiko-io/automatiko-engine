package io.automatiko.engine.service.tracing;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

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
            tracerInstance.activeSpan().setTag("atk.instance.id", instance.id());
            if (instance.businessKey() != null) {
                tracerInstance.activeSpan().setTag("atk.business.key", instance.businessKey());
            }

            Collection<String> tags = instance.tags().values();
            if (!tags.isEmpty()) {
                tracerInstance.activeSpan().setTag("atk.instance.tags",
                        tags.stream().collect(Collectors.joining(",")));
            }
        }
    }

    public void addTags(io.automatiko.engine.api.runtime.process.ProcessInstance instance) {
        if (tracer.isResolvable()) {
            Tracer tracerInstance = tracer.get();
            tracerInstance.activeSpan().setTag("atk.instance.id", instance.getId());
            if (instance.getCorrelationKey() != null) {
                tracerInstance.activeSpan().setTag("atk.business.key", instance.getCorrelationKey());
            }
            Collection<Tag> tags = ((WorkflowProcessInstance) instance).getTags();
            if (!tags.isEmpty()) {
                tracerInstance.activeSpan().setTag("atk.instance.tags",
                        tags.stream().map(t -> t.getValue())
                                .collect(Collectors.joining(",")));
            }
        }
    }
}
