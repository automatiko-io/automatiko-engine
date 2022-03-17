package io.automatiko.engine.api.uow;

import java.util.Set;

public interface TransactionLogStore {

    /**
     * Persists given content that is associated with transaction id, process id and instance id
     * 
     * @param transactionId identifier of the transaction
     * @param processId identifier of the process definition
     * @param instanceId identifier of the process instance
     * @param content content of the process instance
     */
    void store(String transactionId, String processId, String instanceId, byte[] content);

    /**
     * Reads the content of the process instance associated with given process id and process instance id
     * 
     * @param processId identifier of the process definition
     * @param instanceId identifier of the process instance
     * @return content content of the process instance
     */
    byte[] load(String processId, String instanceId);

    /**
     * List all transaction log entries for given process id regardless of the transaction it was written with
     * 
     * @param processId identifier of the process definition
     * @return not null set of instances that should be recovered
     */
    Set<String> list(String processId);

    /**
     * List all transactions that require recovery
     * 
     * @return set of transactions ids to be reovered or empty set
     */
    Set<String> list();

    /**
     * Deletes all entries of the transaction log for given transaction identifier
     * 
     * @param transactionId identifier of the transaction
     */
    void delete(String transactionId);

    void delete(String transactionId, String processId, String instanceId);

    boolean contains(String processId, String instanceId);
}
