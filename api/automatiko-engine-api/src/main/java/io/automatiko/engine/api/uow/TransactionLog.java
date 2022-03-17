package io.automatiko.engine.api.uow;

import java.util.Set;

import io.automatiko.engine.api.runtime.process.NodeInstance;

public interface TransactionLog {

    /**
     * Determines if there are any transactions left that require recovery
     * 
     * @return true when there are unfinished transactions otherwise false
     */
    boolean requiresRecovery();

    /**
     * Returns content of the process instance with given identifier that belongs to process definition identified by
     * <code>processId</code>
     * 
     * @param processId id of the process definition
     * @param instanceId id of the process instance
     * @return returns read bytes representing the process instance
     */
    byte[] readContent(String processId, String instanceId);

    /**
     * Returns set of identifiers of process instances that were not successfully processed and requires recovery
     * 
     * @param processId identifier of the process that the recoverable instances should be located
     * @return set of instance ids that are to be recovered
     */
    Set<String> recoverable(String processId);

    /**
     * Records transaction operation that should be stored in transaction log before it
     * gets flushed into the storage
     * 
     * @param transactionId - unique identifier of the transaction
     * @param processId id of the process definition
     * @param instanceId - id of the instance the transaction belong to
     * @param currentNodeInstance - node instance that is being triggered
     */
    void record(String transactionId, String processId, String instanceId, NodeInstance currentNodeInstance);

    /**
     * Cleans up any transaction log entries as complete method means transaction was completed successfully
     * so any log entries can be safely dropped
     * 
     * @param transactionId - unique identifier of the transaction
     */
    void complete(String transactionId);

    void complete(String transactionId, String id, String instanceId);

    boolean contains(String id, String instanceId);
}
