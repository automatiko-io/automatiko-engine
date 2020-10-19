package io.automatik.engine.api.workflow.cases;

import java.util.Collection;

import io.automatik.engine.api.Model;

public interface CaseDefinitions {

    CaseDefinition<? extends Model> caseDefinitionById(String id);

    Collection<String> caseDefinitionIds();
}
