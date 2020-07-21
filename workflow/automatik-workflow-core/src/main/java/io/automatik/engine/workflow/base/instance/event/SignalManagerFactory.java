
package io.automatik.engine.workflow.base.instance.event;

import io.automatik.engine.api.workflow.signal.SignalManager;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;

public interface SignalManagerFactory {

	SignalManager createSignalManager(InternalProcessRuntime runtime);

}
