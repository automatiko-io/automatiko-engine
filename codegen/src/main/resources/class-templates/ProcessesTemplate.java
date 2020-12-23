package $Package$;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.Processes;

public class ApplicationProcesses implements Processes {

    Object processes;
        
    private Map<String, Process<?>> mappedProcesses = new HashMap<>();

    @javax.annotation.PostConstruct
    public void setup() {
        for (Process<?> process : processes) {
            mappedProcesses.put(process.id(), process);
        }
    }
    
    public Process<? extends Model> processById(String processId) {
        return (Process<? extends Model>)mappedProcesses.get(processId);
    }
    
    public Collection<String> processIds() {
        return mappedProcesses.keySet();
    }
}
