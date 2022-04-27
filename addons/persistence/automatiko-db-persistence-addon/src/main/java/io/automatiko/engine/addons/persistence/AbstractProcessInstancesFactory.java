package io.automatiko.engine.addons.persistence;

import io.automatiko.engine.addons.persistence.db.DatabaseProcessInstances;
import io.automatiko.engine.addons.persistence.db.model.ProcessInstanceEntity;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstancesFactory;

/**
 * This class must always have exact FQCN as
 * <code>io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory</code>
 *
 */
public abstract class AbstractProcessInstancesFactory implements ProcessInstancesFactory {

    @SuppressWarnings("unchecked")
    public DatabaseProcessInstances createProcessInstances(Process<?> process) {
        return new DatabaseProcessInstances((Process<? extends ProcessInstanceEntity>) process, codec(), transactionLogStore(),
                auditor());
    }
}
