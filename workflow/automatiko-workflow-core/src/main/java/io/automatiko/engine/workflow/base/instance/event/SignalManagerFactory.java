
package io.automatiko.engine.workflow.base.instance.event;

import io.automatiko.engine.api.workflow.signal.SignalManager;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;

public interface SignalManagerFactory {

	SignalManager createSignalManager(InternalProcessRuntime runtime);

}
