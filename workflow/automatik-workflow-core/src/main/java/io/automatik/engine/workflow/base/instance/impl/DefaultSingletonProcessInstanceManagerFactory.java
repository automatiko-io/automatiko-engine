
package io.automatik.engine.workflow.base.instance.impl;

import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.ProcessInstanceManager;
import io.automatik.engine.workflow.base.instance.ProcessInstanceManagerFactory;

public class DefaultSingletonProcessInstanceManagerFactory implements ProcessInstanceManagerFactory {

	private static ProcessInstanceManager instance = new DefaultProcessInstanceManager();

	public ProcessInstanceManager createProcessInstanceManager(InternalProcessRuntime runtime) {
		return instance;
	}

}
