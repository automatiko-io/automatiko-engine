package io.automatiko.engine.api.audit;

import java.util.function.Supplier;

public interface Auditor {

    void publish(Supplier<AuditEntry> entry);
}
