package io.automatiko.engine.addons.persistence;

import java.util.Optional;

import com.datastax.oss.driver.api.core.CqlSession;

import io.automatiko.engine.addons.persistence.cassandra.CassandraProcessInstances;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstancesFactory;

/**
 * This class must always have exact FQCN as
 * <code>io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory</code>
 *
 */
public abstract class AbstractProcessInstancesFactory implements ProcessInstancesFactory {

    protected CqlSession cqlSession;

    protected Optional<Boolean> createKeyspace;

    protected Optional<Boolean> createTables;

    protected Optional<String> keyspace;

    public AbstractProcessInstancesFactory() {
    }

    public AbstractProcessInstancesFactory(CqlSession cqlSession, Optional<Boolean> createKeyspace,
            Optional<Boolean> createTables, Optional<String> keyspace) {
        this.cqlSession = cqlSession;
        this.createKeyspace = createKeyspace;
        this.createTables = createTables;
        this.keyspace = keyspace;
    }

    @SuppressWarnings("unchecked")
    public CassandraProcessInstances createProcessInstances(Process<?> process) {
        return new CassandraProcessInstances((Process<? extends Model>) process, cqlSession, codec(),
                transactionLogStore(), auditor(), createKeyspace, createTables, keyspace);
    }
}
