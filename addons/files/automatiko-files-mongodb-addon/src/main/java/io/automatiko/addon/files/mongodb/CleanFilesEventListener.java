package io.automatiko.addon.files.mongodb;

import java.util.Collection;
import java.util.Map.Entry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.automatiko.engine.api.event.process.DefaultProcessEventListener;
import io.automatiko.engine.api.event.process.ProcessCompletedEvent;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.workflow.file.ByteArrayFile;
import io.quarkus.arc.properties.UnlessBuildProperty;

@ApplicationScoped
@UnlessBuildProperty(name = "quarkus.automatiko.on-instance-end", stringValue = "keep", enableIfMissing = true)
public class CleanFilesEventListener extends DefaultProcessEventListener {

    private GridFSStore store;

    @Inject
    public CleanFilesEventListener(GridFSStore store) {
        this.store = store;
    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        ProcessInstance pi = event.getProcessInstance();

        for (Entry<String, Object> variable : pi.getVariables().entrySet()) {

            if (variable.getValue() instanceof ByteArrayFile) {
                ByteArrayFile file = (ByteArrayFile) variable.getValue();
                store.remove(pi.getProcess().getId(), pi.getProcess().getVersion(), pi.getId(), variable.getKey(), file.name());
            } else if (variable.getValue() instanceof Collection) {
                for (Object potentialFile : (Collection<?>) variable.getValue()) {
                    if (potentialFile instanceof ByteArrayFile) {
                        ByteArrayFile file = (ByteArrayFile) potentialFile;
                        store.remove(pi.getProcess().getId(), pi.getProcess().getVersion(), pi.getId(), variable.getKey(),
                                file.name());
                    }
                }
            }
        }
    }

}
