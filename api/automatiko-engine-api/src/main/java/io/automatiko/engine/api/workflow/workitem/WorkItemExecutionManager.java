package io.automatiko.engine.api.workflow.workitem;

import java.util.function.Function;

import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemManager;

public interface WorkItemExecutionManager {

    /**
     * Invoked on completion of the service associated with work item
     * 
     * @param processId id of the process associated with the work item
     * @param name name of the output item that will be returned on completion
     * @param workItem work item associated with execution
     * @param manager manager that can be used for synchronous completion handling
     * @param source value returned by the service invocation
     * @param errorMapper maper of error that might happen during execution
     */
    public void complete(String processId, String name, WorkItem workItem, WorkItemManager manager, Object source,
            Function<Throwable, Throwable> errorMapper);
}
