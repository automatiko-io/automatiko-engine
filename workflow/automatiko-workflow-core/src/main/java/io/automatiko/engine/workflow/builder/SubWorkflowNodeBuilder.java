package io.automatiko.engine.workflow.builder;

import java.util.HashMap;
import java.util.Map;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.executable.core.Metadata;

/**
 * Builder responsible for building an user (tasks assigned to human actors) node
 */
public class SubWorkflowNodeBuilder extends AbstractNodeBuilder {

    private SubProcessNode node;

    public SubWorkflowNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new SubProcessNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));

        this.node.setIndependent(false);
        this.node.setWaitForCompletion(true);

        Map<String, String> inputs = new HashMap<>();
        this.node.setMetaData(Metadata.DATA_INPUTS, inputs);

        Map<String, String> outputs = new HashMap<>();
        this.node.setMetaData(Metadata.DATA_OUTPUTS, outputs);

        this.node.setMetaData("BPMN.InputTypes", inputs);
        this.node.setMetaData("BPMN.OutputTypes", outputs);

        workflowBuilder.get().addNode(node);

        contect();
    }

    public SubWorkflowNodeBuilder dataObjectAsInput(String name) {

        return dataObjectAsInput(name, name);
    }

    public SubWorkflowNodeBuilder dataObjectAsInput(String name, String inputName) {
        Variable var = this.workflowBuilder.get().getVariableScope().findVariable(name);

        if (var == null) {
            throw new IllegalArgumentException("Cannot find data object with '" + name + "' name");
        }
        this.node.addInAssociation(new DataAssociation(name, inputName, null, null));
        addToDataInputs(inputName, var.getType().getStringType());
        return this;
    }

    public SubWorkflowNodeBuilder outputToDataObject(String name, String dataObjectName) {
        Variable var = this.workflowBuilder.get().getVariableScope().findVariable(dataObjectName);

        if (var == null) {
            throw new IllegalArgumentException("Cannot find data object with '" + dataObjectName + "' name");
        }

        this.node.addOutAssociation(new DataAssociation(name, dataObjectName, null, null));
        addToDataOutputs(name, var.getType().getStringType());
        return this;
    }

    /**
     * Specifies the id of the workflow to be called
     * 
     * @param id unique id of the sub workflow
     * @return the builder
     */
    public SubWorkflowNodeBuilder id(String id) {
        this.node.setProcessId(id);
        return this;
    }

    /**
     * Specifies the version of the workflow to be called
     * 
     * @param version version of the sub workflow
     * @return the builder
     */
    public SubWorkflowNodeBuilder version(String version) {
        this.node.setProcessVersion(version);
        return this;
    }

    /**
     * Specifies if the main workflow should wait for completion of the sub workflow
     * or consider it to be "fire and forget"
     * 
     * @param value indicator to wait or not for completion of the sub workflow
     * @return the builder
     */
    public SubWorkflowNodeBuilder waitForCompletion(boolean value) {
        this.node.setWaitForCompletion(value);
        return this;
    }

    /**
     * Specifies if the sub workflow is independent from the main one. It refers to the life cycle
     * of the workflows - if set to true completion of he main workflow does not affect the sub workflow
     * 
     * @param value indicator of independence of sub workflow from the main workflow
     * @return the builder
     */
    public SubWorkflowNodeBuilder indepenent(boolean value) {
        this.node.setIndependent(value);
        return this;
    }

    @Override
    protected Node getNode() {
        return this.node;
    }

    @SuppressWarnings("unchecked")
    private void addToDataInputs(String name, String type) {
        ((Map<String, String>) this.node.getMetaData(Metadata.DATA_INPUTS)).put(name, type);
    }

    @SuppressWarnings("unchecked")
    private void addToDataOutputs(String name, String type) {
        ((Map<String, String>) this.node.getMetaData(Metadata.DATA_OUTPUTS)).put(name, type);
    }

}
