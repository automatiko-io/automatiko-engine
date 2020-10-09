package io.automatik.engine.addons.persistence;

import io.automatik.engine.addons.persistence.db.DatabaseProcessInstances;
import io.automatik.engine.addons.persistence.db.model.ProcessInstanceEntity;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstancesFactory;

/**
 * This class must always have exact FQCN as
 * <code>io.automatik.engine.addons.persistence.AbstractProcessInstancesFactory</code>
 *
 */
public abstract class AbstractProcessInstancesFactory implements ProcessInstancesFactory {

    public DatabaseProcessInstances createProcessInstances(Process<?> process) {
        return new DatabaseProcessInstances((Process<? extends ProcessInstanceEntity>) process);
    }
}
