package io.automatik.engine.api.workflow.cases;

import java.util.List;

import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.workitem.Policy;

public interface CaseInstance<T> extends ProcessInstance<T> {

    CaseFile casefile();

    List<String> availableTasks();

    List<String> stages(Policy<?>... policies);

    List<String> milestones(Policy<?>... policies);
}
