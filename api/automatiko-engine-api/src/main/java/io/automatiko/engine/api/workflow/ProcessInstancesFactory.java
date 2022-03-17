
package io.automatiko.engine.api.workflow;

import io.automatiko.engine.api.uow.TransactionLogStore;
import io.automatiko.engine.api.workflow.encrypt.StoredDataCodec;

public interface ProcessInstancesFactory {

    MutableProcessInstances<?> createProcessInstances(Process<?> process);

    default StoredDataCodec codec() {
        return StoredDataCodec.NO_OP_CODEC;
    }

    default TransactionLogStore transactionLogStore() {
        return null;
    }
}
