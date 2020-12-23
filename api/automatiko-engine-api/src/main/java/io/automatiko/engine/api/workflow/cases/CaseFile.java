package io.automatiko.engine.api.workflow.cases;

import java.util.List;
import java.util.function.Predicate;

public interface CaseFile {

    <T> CaseFileItem<T> create(String name, T value) throws CaseFileItemDuplicatedException;

    <T> CaseFileItem<T> update(String name, T value) throws CaseFileItemNotFoundException;

    <T> CaseFileItem<T> delete(String name) throws CaseFileItemNotFoundException;

    CaseFileItem<?> fileItem(String name) throws CaseFileItemNotFoundException;

    List<CaseFileItem<?>> fileItems();

    List<CaseFileItem<?>> fileItems(Predicate<CaseFileItem<?>> filter);
}
