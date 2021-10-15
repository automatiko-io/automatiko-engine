package io.automatiko.engine.addons.persistence;

import io.automatiko.engine.addons.persistence.dynamodb.DynamoDBProcessInstances;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.config.DynamoDBPersistenceConfig;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstancesFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * This class must always have exact FQCN as
 * <code>io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory</code>
 *
 */
public abstract class AbstractProcessInstancesFactory implements ProcessInstancesFactory {

    protected DynamoDbClient dynamodb;
    protected DynamoDBPersistenceConfig config;

    public AbstractProcessInstancesFactory() {
    }

    public AbstractProcessInstancesFactory(DynamoDbClient dynamodb, DynamoDBPersistenceConfig config) {
        this.dynamodb = dynamodb;
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    public DynamoDBProcessInstances createProcessInstances(Process<?> process) {
        return new DynamoDBProcessInstances((Process<? extends Model>) process, dynamodb, config, codec());
    }
}
