package io.automatik.engine.api.workflow.cases;

import java.util.Collection;
import java.util.Optional;

import io.automatik.engine.api.workflow.ProcessInstanceReadMode;
import io.automatik.engine.api.workflow.ProcessInstances;

public interface CaseInstances<T> extends ProcessInstances<T> {

    Optional<CaseInstance<T>> findById(String i);

    Collection<CaseInstance<T>> values(int page, int size);

    Optional<CaseInstance<T>> findById(String i, ProcessInstanceReadMode mode);

    Collection<CaseInstance<T>> values(ProcessInstanceReadMode mode, int page, int size);
}
