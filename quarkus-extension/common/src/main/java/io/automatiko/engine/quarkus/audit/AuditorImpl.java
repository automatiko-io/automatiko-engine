package io.automatiko.engine.quarkus.audit;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
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
    private Application application;
    private AuditEntryFilter filter;

    private boolean enabled;
    private String format;
    private Set<String> includedTypes = new HashSet<>();
    private Set<String> excludedTypes = new HashSet<>();

    @Inject
    public AuditorImpl(AuditStore store, Application application, AuditEntryFilter filter,
            @ConfigProperty(name = AuditConfig.ENABLED_KEY) Optional<Boolean> enabled,
            @ConfigProperty(name = AuditConfig.FORMAT_KEY) Optional<String> format,
            @ConfigProperty(name = AuditConfig.INCLUDED_KEY) Optional<String> includes,
            @ConfigProperty(name = AuditConfig.EXCLUDED_KEY) Optional<String> excludes) {
        this.store = store;
        this.enabled = enabled.orElse(false);
        this.application = application;
        this.filter = filter;
        this.format = format.orElse("plain");

        if (includes.isPresent()) {
            for (String included : includes.get().split(",")) {
                this.includedTypes.add(included.trim().toLowerCase());
            }
        }

        if (excludes.isPresent()) {
            for (String excluded : excludes.get().split(",")) {
                this.excludedTypes.add(excluded.trim().toLowerCase());
            }
        }
    }

    @Override
    public void publish(Supplier<AuditEntry> entry) {
        if (!enabled) {
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

                store.store(auditEntry, format);

            } catch (Exception e) {
                LOGGER.warn("Unable to store audit entry due to unexpected error", e);
            }
        }
    }
}
