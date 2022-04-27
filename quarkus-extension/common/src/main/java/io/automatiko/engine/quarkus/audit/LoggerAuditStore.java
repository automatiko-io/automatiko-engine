package io.automatiko.engine.quarkus.audit;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.audit.AuditEntry;
import io.automatiko.engine.api.audit.AuditStore;
import io.quarkus.arc.DefaultBean;

@ApplicationScoped
@DefaultBean
public class LoggerAuditStore implements AuditStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutomatikoAudit");

    @Override
    public void store(AuditEntry entry, String format) {
        if ("json".equalsIgnoreCase(format)) {
            LOGGER.info(entry.toJsonString());
        } else {
            LOGGER.info(entry.toRawString());
        }

    }

}
