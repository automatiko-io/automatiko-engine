package io.automatiko.engine.service.deployment.devconsole;

import io.automatiko.engine.service.dev.WorkflowInfoSupplier;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectWorkflowInfo() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("workflowInfos", new WorkflowInfoSupplier());
    }
}
