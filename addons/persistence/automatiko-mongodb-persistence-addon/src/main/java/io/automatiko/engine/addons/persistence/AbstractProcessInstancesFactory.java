
package io.automatiko.engine.addons.persistence;

import com.mongodb.client.MongoClient;

import io.automatiko.engine.addons.persistence.mongodb.MongodbProcessInstances;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.config.MongodbPersistenceConfig;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstancesFactory;

/**
 * This class must always have exact FQCN as
 * <code>io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory</code>
 *
 */
public abstract class AbstractProcessInstancesFactory implements ProcessInstancesFactory {

    protected MongoClient mongoClient;
    protected MongodbPersistenceConfig config;

    public AbstractProcessInstancesFactory() {
    }

    public AbstractProcessInstancesFactory(MongoClient mongoClient, MongodbPersistenceConfig config) {
        this.mongoClient = mongoClient;
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    public MongodbProcessInstances createProcessInstances(Process<?> process) {
        return new MongodbProcessInstances((Process<? extends Model>) process, mongoClient, config, codec());
    }
}
