package org.acme.audit;

import javax.enterprise.context.ApplicationScoped;

import io.automatiko.engine.api.audit.AuditEntry;
import io.automatiko.engine.api.audit.AuditEntryFilter;

@ApplicationScoped
public class ScriptOnlyAuditEntryFilter implements AuditEntryFilter {

    @Override
    public boolean accept(AuditEntry entry) {
        if ("scripts".equalsIgnoreCase((String) entry.items().get("workflowDefinitionId"))) {
            return true;
        }
        return false;
    }

}
