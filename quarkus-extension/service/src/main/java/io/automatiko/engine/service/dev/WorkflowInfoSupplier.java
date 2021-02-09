package io.automatiko.engine.service.dev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import javax.enterprise.inject.Any;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.TypeLiteral;

import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.workflow.AbstractProcess;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;
import io.quarkus.arc.Arc;

public class WorkflowInfoSupplier implements Supplier<Collection<WorkflowInfo>> {

    @SuppressWarnings("serial")
    @Override
    public Collection<WorkflowInfo> get() {
        Collection<WorkflowInfo> processes = new ArrayList<WorkflowInfo>();

        Arc.container().select(new TypeLiteral<Process<?>>() {
        }, new AnnotationLiteral<Any>() {
        }).forEach(p -> {
            processes.add(new WorkflowInfo(p.id(), p.name(), isPublic(p),
                    (String) ((AbstractProcess<?>) p).process().getMetaData().get("Documentation")));
        });

        return processes;
    }

    private boolean isPublic(Process<?> p) {
        String visibility = ((WorkflowProcess) ((AbstractProcess<?>) p).process()).getVisibility();

        if (WorkflowProcess.PUBLIC_VISIBILITY.equals(visibility) || WorkflowProcess.NONE_VISIBILITY.equals(visibility)) {
            return true;
        }

        return false;
    }

}
