
package io.automatiko.engine.api.workflow;

import java.util.Collection;
import java.util.Optional;

import io.automatiko.engine.api.uow.TransactionLog;

public interface ProcessInstances<T> {

    public static final String ID_SORT_KEY = "id";
    public static final String DESC_SORT_KEY = "description";
    public static final String START_DATE_SORT_KEY = "startDate";
    public static final String END_DATE_SORT_KEY = "endDate";
    public static final String BUSINESS_KEY_SORT_KEY = "businessKey";

    TransactionLog transactionLog();

    default Optional<? extends ProcessInstance<T>> findById(String id) {
        return findById(id, ProcessInstanceReadMode.MUTABLE);
    }

    default Optional<? extends ProcessInstance<T>> findById(String id, ProcessInstanceReadMode mode) {
        return findById(id, ProcessInstance.STATE_ACTIVE, mode);
    }

    Optional<? extends ProcessInstance<T>> findById(String id, int status, ProcessInstanceReadMode mode);

    default Collection<? extends ProcessInstance<T>> values(int page, int size) {
        return values(ProcessInstanceReadMode.READ_ONLY, page, size);
    }

    default Collection<? extends ProcessInstance<T>> values(ProcessInstanceReadMode mode, int page, int size) {
        return values(mode, ProcessInstance.STATE_ACTIVE, page, size);
    }

    Collection<? extends ProcessInstance<T>> values(ProcessInstanceReadMode mode, int status, int page, int size);

    default Collection<? extends ProcessInstance<T>> values(ProcessInstanceReadMode mode, int page, int size, String sortBy,
            boolean sortAsc) {
        return values(mode, ProcessInstance.STATE_ACTIVE, page, size, sortBy, sortAsc);
    }

    default Collection<? extends ProcessInstance<T>> values(ProcessInstanceReadMode mode, int status, int page, int size,
            String sortBy, boolean sortAsc) {
        return values(mode, status, page, size);
    }

    default Collection<? extends ProcessInstance<T>> findByIdOrTag(String... values) {
        return findByIdOrTag(ProcessInstanceReadMode.MUTABLE, values);
    }

    default Collection<? extends ProcessInstance<T>> findByIdOrTag(ProcessInstanceReadMode mode, String... values) {
        return findByIdOrTag(mode, ProcessInstance.STATE_ACTIVE, values);
    }

    Collection<? extends ProcessInstance<T>> findByIdOrTag(ProcessInstanceReadMode mode, int status, String... values);

    default Collection<? extends ProcessInstance<T>> findByIdOrTag(String sortBy, boolean sortAsc, String... values) {
        return findByIdOrTag(ProcessInstanceReadMode.MUTABLE, sortBy, sortAsc, values);
    }

    default Collection<? extends ProcessInstance<T>> findByIdOrTag(ProcessInstanceReadMode mode, String sortBy, boolean sortAsc,
            String... values) {
        return findByIdOrTag(mode, ProcessInstance.STATE_ACTIVE, sortBy, sortAsc, values);
    }

    default Collection<? extends ProcessInstance<T>> findByIdOrTag(ProcessInstanceReadMode mode, int status, String sortBy,
            boolean sortAsc, String... values) {
        return findByIdOrTag(mode, status, values);
    }

    default Collection<String> locateByIdOrTag(String... values) {
        return locateByIdOrTag(ProcessInstance.STATE_ACTIVE, values);
    }

    Collection<String> locateByIdOrTag(int status, String... values);

    Long size();

}
