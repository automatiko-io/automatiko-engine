package io.automatiko.engine.quarkus.audit;

import javax.enterprise.context.ApplicationScoped;

import io.automatiko.engine.api.audit.AuditEntry;
import io.automatiko.engine.api.audit.AuditEntryFilter;
import io.quarkus.arc.DefaultBean;

@ApplicationScoped
@DefaultBean
public class AcceptAllAuditEntryFilter implements AuditEntryFilter {

    @Override
    public boolean accept(AuditEntry entry) {
        return true;
    }

}
