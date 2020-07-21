
package io.automatik.engine.workflow.base.instance.event;

import io.automatik.engine.api.workflow.signal.SignalManager;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;

public class DefaultSignalManagerFactory implements SignalManagerFactory {

	public SignalManager createSignalManager(InternalProcessRuntime runtime) {
		return new DefaultSignalManager(runtime);
	}

}
