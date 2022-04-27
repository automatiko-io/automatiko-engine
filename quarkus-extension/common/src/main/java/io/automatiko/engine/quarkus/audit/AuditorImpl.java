package io.automatiko.engine.quarkus.audit;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.audit.AuditEntry;
import io.automatiko.engine.api.audit.AuditEntryFilter;
import io.automatiko.engine.api.audit.AuditStore;
import io.automatiko.engine.api.audit.Auditor;
import io.automatiko.engine.api.config.AuditConfig;

@ApplicationScoped
public class AuditorImpl implements Auditor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditorImpl.class);

    private AuditStore store;
    private AuditConfig config;
    private Application application;
    private AuditEntryFilter filter;

    private Set<String> includedTypes = new HashSet<>();
    private Set<String> excludedTypes = new HashSet<>();

    @Inject
    public AuditorImpl(AuditStore store, AuditConfig config, Application application, AuditEntryFilter filter) {
        this.store = store;
        this.config = config;
        this.application = application;
        this.filter = filter;

        if (config.included().isPresent()) {
            for (String included : config.included().get().split(",")) {
                this.includedTypes.add(included.trim().toLowerCase());
            }
        }

        if (config.excluded().isPresent()) {
            for (String excluded : config.excluded().get().split(",")) {
                this.excludedTypes.add(excluded.trim().toLowerCase());
            }
        }
    }

    @Override
    public void publish(Supplier<AuditEntry> entry) {
        if (!config.enabled()) {
            return;
        }
        AuditEntry auditEntry = entry.get();
        if (excludedTypes.contains(auditEntry.type().name().toLowerCase())) {
            return;
        }

        if (includedTypes.isEmpty() || includedTypes.contains(auditEntry.type().name().toLowerCase())) {
            try {

                if (!filter.accept(auditEntry)) {
                    LOGGER.debug("Audit entry {} has been rejected by configured filter", auditEntry);
                    return;
                }

                String uowIdentifier = application.unitOfWorkManager().currentUnitOfWork().identifier();
                auditEntry.add("transactionId", uowIdentifier);

                store.store(auditEntry, config.format().orElse("plain"));

            } catch (Exception e) {
                LOGGER.warn("Unable to store audit entry due to unexpected error", e);
            }
        }
    }
}
