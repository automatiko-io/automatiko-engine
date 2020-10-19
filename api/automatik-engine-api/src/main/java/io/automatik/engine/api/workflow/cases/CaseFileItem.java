package io.automatik.engine.api.workflow.cases;

import java.time.ZonedDateTime;

public interface CaseFileItem<T> {

    public enum Status {
        Available,
        Discarded
    }

    String name();

    Class<?> type();

    Status status();

    T value();

    ZonedDateTime lastModificatied();

    String lastModifiedBy();
}
