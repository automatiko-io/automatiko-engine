package io.automatiko.engine.workflow.base.instance.impl.end;

import io.automatiko.engine.api.workflow.ArchiveBuilder;
import io.automatiko.engine.api.workflow.ArchiveStore;
import io.automatiko.engine.api.workflow.ArchivedProcessInstance;
import io.automatiko.engine.api.workflow.EndOfInstanceStrategy;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.json.JsonArchiveBuilder;

public class ArchiveEndOfInstanceStrategy implements EndOfInstanceStrategy {

    private ArchiveStore storage;

    public ArchiveEndOfInstanceStrategy(ArchiveStore storage) {
        this.storage = storage;
    }

    @Override
    public boolean shouldInstanceBeRemoved() {
        return true;
    }

    @Override
    public boolean shouldInstanceBeUpdated() {
        return false;
    }

    @Override
    public void perform(ProcessInstance<?> instance) {
        ArchiveBuilder builder = new JsonArchiveBuilder();
        ArchivedProcessInstance archived = instance.archive(builder);

        storage.store(archived);
    }

}
