package io.automatiko.engine.addons.process.management.archive;

import io.automatiko.engine.api.workflow.ArchiveBuilder;
import io.automatiko.engine.api.workflow.ArchivedProcessInstance;
import io.automatiko.engine.api.workflow.ArchivedVariable;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;

public class JsonArchiveBuilder implements ArchiveBuilder {

    @Override
    public ArchivedProcessInstance instance(String id, ExportedProcessInstance<?> exported) {
        return new ArchivedProcessInstance(id, exported);
    }

    @Override
    public ArchivedVariable variable(String name, Object value) {
        return new JsonArchiveVariable(name, value);
    }

}
