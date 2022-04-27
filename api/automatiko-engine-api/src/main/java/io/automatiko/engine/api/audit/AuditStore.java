package io.automatiko.engine.api.audit;

public interface AuditStore {

    /**
     * Stores the audit entry in preferred way
     * 
     * @param entry entry to be stored
     * @param format requested format for storing the entry
     */
    void store(AuditEntry entry, String format);
}
