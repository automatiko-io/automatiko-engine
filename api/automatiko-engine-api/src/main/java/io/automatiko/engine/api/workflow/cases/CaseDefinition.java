package io.automatiko.engine.api.workflow.cases;

public interface CaseDefinition<T> extends io.automatiko.engine.api.workflow.Process<T> {

    CaseInstances<T> caseInstances();

}
