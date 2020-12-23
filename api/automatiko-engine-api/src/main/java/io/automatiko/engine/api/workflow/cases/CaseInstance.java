package io.automatiko.engine.api.workflow.cases;

import java.util.List;

import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.workitem.Policy;

public interface CaseInstance<T> extends ProcessInstance<T> {

    CaseFile casefile();

    List<String> availableTasks();

    List<String> stages(Policy<?>... policies);

    List<String> milestones(Policy<?>... policies);
}
