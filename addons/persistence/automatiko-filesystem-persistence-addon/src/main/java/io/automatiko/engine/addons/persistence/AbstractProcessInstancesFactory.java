
package io.automatiko.engine.addons.persistence;

import java.nio.file.Paths;
import java.util.Optional;

import io.automatiko.engine.addons.persistence.filesystem.FileSystemProcessInstances;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstancesFactory;

/**
 * This class must always have exact FQCN as
 * <code>io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory</code>
 *
 */
public abstract class AbstractProcessInstancesFactory implements ProcessInstancesFactory {

    protected Optional<Integer> lockTimeout = Optional.empty();

    protected Optional<Integer> lockLimit = Optional.empty();

    protected Optional<Integer> lockWait = Optional.empty();

    public AbstractProcessInstancesFactory() {
    }

    public AbstractProcessInstancesFactory(Optional<Integer> lockTimeout, Optional<Integer> lockLimit,
            Optional<Integer> lockWait) {
        this.lockTimeout = lockTimeout;
        this.lockLimit = lockLimit;
        this.lockWait = lockWait;
    }

    public FileSystemProcessInstances createProcessInstances(Process<?> process) {
        return new FileSystemProcessInstances(process, Paths.get(path()), codec(), transactionLogStore(), auditor(),
                lockTimeout, lockLimit, lockWait);
    }

    public abstract String path();

}
