package io.automatiko.addon.files.filesystem;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.automatiko.engine.api.event.process.DefaultProcessEventListener;
import io.automatiko.engine.api.event.process.ProcessCompletedEvent;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.quarkus.arc.properties.UnlessBuildProperty;

@ApplicationScoped
@UnlessBuildProperty(name = "quarkus.automatiko.on-instance-end", stringValue = "keep", enableIfMissing = true)
public class CleanFilesEventListener extends DefaultProcessEventListener {

    private FileStore store;

    @Inject
    public CleanFilesEventListener(FileStore store) {
        this.store = store;
    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        ProcessInstance pi = event.getProcessInstance();
        store.remove(pi.getProcess().getId(), pi.getProcess().getVersion(), pi.getId());
    }

}
