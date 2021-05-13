package io.automatiko.engine.addons.process.management.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.automatiko.engine.addons.process.management.model.JsonExportedProcessInstance;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.StringExportedProcessInstance;

public class ProcessInstanceExporter {

    private final Map<String, Process<?>> processData;

    public ProcessInstanceExporter(Map<String, Process<?>> processData) {
        this.processData = processData;
    }

    @SuppressWarnings("unchecked")
    public JsonExportedProcessInstance exportInstance(String id,
            ProcessInstance<?> processInstance) {

        Collection<ProcessInstance<? extends Model>> subInstances = processInstance.subprocesses();
        List<JsonExportedProcessInstance> subinstances = new ArrayList<JsonExportedProcessInstance>();
        if (!subInstances.isEmpty()) {

            for (ProcessInstance<? extends Model> si : subInstances) {

                JsonExportedProcessInstance subExported = exportInstance(id + ":" + si.id(), si);
                subinstances.add(subExported);
            }
        }

        ExportedProcessInstance<String> exported = processInstance.process().exportInstance(id, false);

        JsonExportedProcessInstance jsonExported = JsonExportedProcessInstance.of(exported);
        jsonExported.setSubInstances(subinstances);

        return jsonExported;
    }

    public ProcessInstance<?> importInstance(JsonExportedProcessInstance instance) {

        if (!instance.getSubInstances().isEmpty()) {

            for (JsonExportedProcessInstance subinstance : instance.getSubInstances()) {
                importInstance(subinstance);
            }
        }

        String processId = instance.getInstance().get("processId").asText();
        String processVersion = instance.getInstance().has("processVersion")
                ? instance.getInstance().get("processVersion").asText()
                : null;

        if (processVersion != null) {
            processId += "_" + processVersion.replaceAll("\\.", "_");
        }

        Process<?> process = processData.get(processId);

        return process.importInstance(
                StringExportedProcessInstance.of(instance.getHeader().toString(), instance.getInstance().toString(),
                        instance.getTimers().toString(), s -> {
                            return instance.convertTimers();
                        }));
    }

}
