package io.automatiko.engine.api.audit;

public interface AuditEntryFilter {

    /**
     * Determines if given entry should be accepted by auditor implementation
     * 
     * @param entry entry to be inspected
     * @return true if entry should be stored otherwise false
     */
    boolean accept(AuditEntry entry);
}
