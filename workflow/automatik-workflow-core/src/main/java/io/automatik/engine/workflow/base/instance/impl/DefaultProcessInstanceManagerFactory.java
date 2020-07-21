
package io.automatik.engine.workflow.base.instance.impl;

import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.ProcessInstanceManager;
import io.automatik.engine.workflow.base.instance.ProcessInstanceManagerFactory;

public class DefaultProcessInstanceManagerFactory implements ProcessInstanceManagerFactory {

	public ProcessInstanceManager createProcessInstanceManager(InternalProcessRuntime runtime) {
		return new DefaultProcessInstanceManager();
	}

}
