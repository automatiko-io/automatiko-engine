
package io.automatiko.engine.api.workflow;

public interface ProcessInstancesFactory {

	MutableProcessInstances<?> createProcessInstances(Process<?> process);
}
