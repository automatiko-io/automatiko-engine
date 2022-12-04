package io.automatiko.engine.addons.persistence;

import java.util.Optional;

import io.automatiko.engine.addons.persistence.dynamodb.DynamoDBProcessInstances;
import io.automatiko.engine.api.Model;
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

    protected Optional<Boolean> createTables;

    protected Optional<Long> readCapacity;

    protected Optional<Long> writeCapacity;

    public AbstractProcessInstancesFactory() {
    }

    public AbstractProcessInstancesFactory(DynamoDbClient dynamodb, Optional<Boolean> createTables,
            Optional<Long> readCapacity, Optional<Long> writeCapacity) {
        this.dynamodb = dynamodb;
        this.createTables = createTables;
        this.readCapacity = readCapacity;
        this.writeCapacity = writeCapacity;
    }

    @SuppressWarnings("unchecked")
    public DynamoDBProcessInstances createProcessInstances(Process<?> process) {
        return new DynamoDBProcessInstances((Process<? extends Model>) process, dynamodb, codec(),
                transactionLogStore(), auditor(), createTables, readCapacity, writeCapacity);
    }
}
