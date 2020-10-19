package io.automatik.engine.api.workflow.cases;

public interface CaseDefinition<T> extends io.automatik.engine.api.workflow.Process<T> {

    CaseInstances<T> caseInstances();

}
