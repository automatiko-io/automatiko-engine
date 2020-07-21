
package io.automatik.engine.workflow.base.instance;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.event.process.ProcessEventManager;
import io.automatik.engine.api.runtime.process.ProcessRuntime;
import io.automatik.engine.api.uow.UnitOfWorkManager;
import io.automatik.engine.api.workflow.signal.SignalManager;
import io.automatik.engine.services.correlation.CorrelationAwareProcessRuntime;
import io.automatik.engine.workflow.base.core.event.ProcessEventSupport;

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
}
