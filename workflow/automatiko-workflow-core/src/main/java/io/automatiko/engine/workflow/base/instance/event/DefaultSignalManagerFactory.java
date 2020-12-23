
package io.automatiko.engine.workflow.base.instance.event;

import io.automatiko.engine.api.workflow.signal.SignalManager;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;

public class DefaultSignalManagerFactory implements SignalManagerFactory {

	public SignalManager createSignalManager(InternalProcessRuntime runtime) {
		return new DefaultSignalManager(runtime);
	}

}
