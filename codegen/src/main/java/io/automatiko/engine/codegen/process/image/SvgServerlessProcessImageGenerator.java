package io.automatiko.engine.codegen.process.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.interfaces.WorkflowDiagram;
import io.serverlessworkflow.diagram.WorkflowDiagramImpl;

public class SvgServerlessProcessImageGenerator implements SvgProcessImageGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SvgServerlessProcessImageGenerator.class);

    private Workflow workflow;

    public SvgServerlessProcessImageGenerator(WorkflowProcess workFlowProcess) {
        this.workflow = (Workflow) workFlowProcess.getMetaData().get("SW-Workflow");
    }

    @Override
    public String generate() {
        if (workflow == null) {
            return null;
        }

        WorkflowDiagram workflowDiagram = new WorkflowDiagramImpl();
        workflowDiagram.setWorkflow(workflow);

        String diagramSVG = null;
        try {
            diagramSVG = workflowDiagram.getSvgDiagram();
        } catch (Exception e) {
            LOGGER.warn("Unable to generate workflow image for {} due to {}", workflow.getName(), e.getMessage());
        }

        return diagramSVG;
    }

}
