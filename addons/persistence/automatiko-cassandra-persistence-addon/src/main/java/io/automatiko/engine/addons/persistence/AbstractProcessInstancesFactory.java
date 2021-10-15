package io.automatiko.engine.addons.persistence;

import com.datastax.oss.driver.api.core.CqlSession;

import io.automatiko.engine.addons.persistence.cassandra.CassandraProcessInstances;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.config.CassandraPersistenceConfig;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstancesFactory;

/**
 * This class must always have exact FQCN as
 * <code>io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory</code>
 *
 */
public abstract class AbstractProcessInstancesFactory implements ProcessInstancesFactory {

    protected CqlSession cqlSession;
    protected CassandraPersistenceConfig config;

    public AbstractProcessInstancesFactory() {
    }

    public AbstractProcessInstancesFactory(CqlSession cqlSession, CassandraPersistenceConfig config) {
        this.cqlSession = cqlSession;
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    public CassandraProcessInstances createProcessInstances(Process<?> process) {
        return new CassandraProcessInstances((Process<? extends Model>) process, cqlSession, config, codec());
    }
}
