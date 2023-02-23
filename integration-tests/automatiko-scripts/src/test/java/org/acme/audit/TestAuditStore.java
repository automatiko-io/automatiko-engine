package org.acme.audit;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.automatiko.engine.api.audit.AuditEntry;
import io.automatiko.engine.quarkus.audit.LoggerAuditStore;

@ApplicationScoped
public class TestAuditStore extends LoggerAuditStore {

    private List<AuditEntry> entries = new ArrayList<>();

    @Override
    public void store(AuditEntry entry, String format) {
        super.store(entry, format);

        this.entries.add(entry);
    }

    public List<AuditEntry> entries() {
        return entries;
    }

    public void clear() {
        this.entries.clear();
    }
}
