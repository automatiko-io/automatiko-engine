package io.automatiko.engine.workflow.json;

import io.automatiko.engine.api.workflow.ArchiveBuilder;
import io.automatiko.engine.api.workflow.ArchivedProcessInstance;
import io.automatiko.engine.api.workflow.ArchivedVariable;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.files.File;
import io.automatiko.engine.workflow.file.FileArchivedVariable;

public class JsonArchiveBuilder implements ArchiveBuilder {

    @Override
    public ArchivedProcessInstance instance(String id, String processId, ExportedProcessInstance<?> exported) {
        return new ArchivedProcessInstance(id, processId, exported);
    }

    @Override
    public ArchivedVariable variable(String name, Object value) {

        if (value instanceof File) {

            return new FileArchivedVariable(name, value);
        } else {

            return new JsonArchiveVariable(name, value);
        }
    }

}
