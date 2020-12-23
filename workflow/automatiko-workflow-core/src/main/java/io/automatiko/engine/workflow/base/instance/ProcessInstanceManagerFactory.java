
package io.automatiko.engine.workflow.base.instance;

public interface ProcessInstanceManagerFactory {

	ProcessInstanceManager createProcessInstanceManager(InternalProcessRuntime runtime);

}
