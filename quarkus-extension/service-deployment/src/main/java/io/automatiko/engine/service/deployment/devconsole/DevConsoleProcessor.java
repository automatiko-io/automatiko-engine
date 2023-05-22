package io.automatiko.engine.service.deployment.devconsole;

import io.automatiko.engine.service.dev.AutomatikoServiceJsonRpcService;
import io.automatiko.engine.service.dev.WorkflowInfoSupplier;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectWorkflowInfo() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("workflowInfos", new WorkflowInfoSupplier());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem create() {
        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        cardPageBuildItem.addPage(Page.externalPageBuilder("Workflow console") // <3> The link label
                .icon("font-awesome-solid:gauge")
                .url("/management/processes/ui")
                .isHtmlContent());

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .icon("font-awesome-solid:diagram-project")
                .componentLink("qwc-automatiko-service-workflows.js")
                .title("Workflows"));

        return cardPageBuildItem;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem("AutomatikoService", AutomatikoServiceJsonRpcService.class);
    }
}
