
package io.automatiko.engine.addons.persistence;

import java.util.Optional;

import com.mongodb.client.MongoClient;

import io.automatiko.engine.addons.persistence.mongodb.MongodbProcessInstances;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstancesFactory;

/**
 * This class must always have exact FQCN as
 * <code>io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory</code>
 *
 */
public abstract class AbstractProcessInstancesFactory implements ProcessInstancesFactory {

    protected MongoClient mongoClient;

    protected Optional<String> database;

    public AbstractProcessInstancesFactory() {
    }

    public AbstractProcessInstancesFactory(MongoClient mongoClient, Optional<String> database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    @SuppressWarnings("unchecked")
    public MongodbProcessInstances createProcessInstances(Process<?> process) {
        return new MongodbProcessInstances((Process<? extends Model>) process, mongoClient, codec(),
                transactionLogStore(), auditor(), database);
    }
}
