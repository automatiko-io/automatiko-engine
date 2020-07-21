
package io.automatik.engine.addons.persistence;

import java.nio.file.Paths;

import io.automatik.engine.addons.persistence.filesystem.FileSystemProcessInstances;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstancesFactory;

/**
 * This class must always have exact FQCN as
 * <code>io.automatik.engine.addons.persistence.AbstractProcessInstancesFactory</code>
 *
 */
public abstract class AbstractProcessInstancesFactory implements ProcessInstancesFactory {

	public FileSystemProcessInstances createProcessInstances(Process<?> process) {
		return new FileSystemProcessInstances(process, Paths.get(path()));
	}

	public abstract String path();

}
