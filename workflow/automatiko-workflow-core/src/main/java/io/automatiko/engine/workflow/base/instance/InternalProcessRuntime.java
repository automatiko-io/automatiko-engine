
package io.automatiko.engine.workflow.base.instance;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.event.process.ProcessEventManager;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;
import io.automatiko.engine.api.uow.UnitOfWorkManager;
import io.automatiko.engine.api.workflow.VariableInitializer;
import io.automatiko.engine.api.workflow.signal.SignalManager;
import io.automatiko.engine.services.correlation.CorrelationAwareProcessRuntime;
import io.automatiko.engine.workflow.base.core.event.ProcessEventSupport;

public interface InternalProcessRuntime extends ProcessRuntime, ProcessEventManager, CorrelationAwareProcessRuntime {

    ProcessInstanceManager getProcessInstanceManager();

    SignalManager getSignalManager();

    ProcessEventSupport getProcessEventSupport();

    UnitOfWorkManager getUnitOfWorkManager();

    void dispose();

    void setProcessEventSupport(ProcessEventSupport processEventSupport);

    void clearProcessInstances();

    void clearProcessInstancesState();

    Process getProcess(String id);

    VariableInitializer getVariableInitializer();
}
