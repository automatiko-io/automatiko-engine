
package io.automatik.engine.api.workflow;

public interface ProcessInstancesFactory {

	MutableProcessInstances<?> createProcessInstances(Process<?> process);
}
