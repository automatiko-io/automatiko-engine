package io.automatiko.engine.workflow.bpmn2;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.Processes;

public class BpmnProcesses implements Processes {

    private Map<String, BpmnProcess> processes = new HashMap<>();

    public BpmnProcesses(List<BpmnProcess> processList) {
        if (processList != null) {
            for (BpmnProcess process : processList) {
                this.processes.put(process.id(), process);
            }
        }
    }

    @Override
    public Process<? extends Model> processById(String id) {
        return processes.get(id);
    }

    @Override
    public Collection<String> processIds() {
        return processes.keySet();
    }

    public Collection<BpmnProcess> processes() {
        return processes.values();
    }

    @Override
    public void activate() {
        processes.values().forEach(p -> p.activate());
    }

    @Override
    public void deactivate() {
        processes.values().forEach(p -> p.deactivate());
    }

}
