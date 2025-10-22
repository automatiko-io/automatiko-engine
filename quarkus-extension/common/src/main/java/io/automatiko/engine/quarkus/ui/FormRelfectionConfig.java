package io.automatiko.engine.quarkus.ui;

import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.workflow.BaseWorkItem;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = { WorkItem.class, BaseWorkItem.class })
public class FormRelfectionConfig {

}
