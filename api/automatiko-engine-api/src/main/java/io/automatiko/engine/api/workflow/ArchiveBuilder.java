package io.automatiko.engine.api.workflow;

public interface ArchiveBuilder {

    ArchivedProcessInstance instance(String id, String processId, ExportedProcessInstance<?> exported);

    ArchivedVariable variable(String name, Object value);
}
