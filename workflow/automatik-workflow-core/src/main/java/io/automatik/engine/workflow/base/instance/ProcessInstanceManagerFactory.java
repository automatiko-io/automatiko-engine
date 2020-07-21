
package io.automatik.engine.workflow.base.instance;

public interface ProcessInstanceManagerFactory {

	ProcessInstanceManager createProcessInstanceManager(InternalProcessRuntime runtime);

}
