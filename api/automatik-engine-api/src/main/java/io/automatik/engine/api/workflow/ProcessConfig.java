
package io.automatik.engine.api.workflow;

import java.util.Collections;
import java.util.Map;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.jobs.JobsService;
import io.automatik.engine.api.uow.UnitOfWorkManager;
import io.automatik.engine.api.workflow.signal.SignalManagerHub;

public interface ProcessConfig {
    WorkItemHandlerConfig workItemHandlers();

    ProcessEventListenerConfig processEventListeners();

    SignalManagerHub signalManagerHub();

    UnitOfWorkManager unitOfWorkManager();

    JobsService jobsService();

    VariableInitializer variableInitializer();

    // TODO refactor this
    default Map<String, Process> processes() {
        return Collections.emptyMap();
    }

    default ProcessInstancesFactory processInstancesFactory() {
        return null;
    }

}
