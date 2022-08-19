package io.automatiko.engine.workflow.builder;

import io.automatiko.engine.workflow.base.core.Work;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.impl.ParameterDefinitionImpl;
import io.automatiko.engine.workflow.base.core.impl.WorkImpl;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;

/**
 * Builder responsible for building an service node
 */
public class RestServiceNodeBuilder extends AbstractNodeBuilder {

    private static final String SERVICE_TASK_TYPE = "Service Task";

    private WorkItemNode node;

    public RestServiceNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new WorkItemNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));
        this.node.setMetaData("Type", SERVICE_TASK_TYPE);
        this.node.setMetaData("Implementation", "##WebService");

        Work work = new WorkImpl();
        this.node.setWork(work);
        work.setName(SERVICE_TASK_TYPE);

        workflowBuilder.get().addNode(node);

        contect();
    }

    /**
     * Configures the OpenAPI definition url, can be either local or remote (https)
     * 
     * @param apiDefinitionUrl open api definition url
     * @return the builder
     */
    public RestServiceNodeBuilder openApi(String apiDefinitionUrl) {
        this.node.getWork().setParameter("Interface", apiDefinitionUrl);

        this.node.getWork().setParameter("interfaceImplementationRef", apiDefinitionUrl);
        this.node.getWork().setParameter("implementation", "##WebService");

        return this;
    }

    /**
     * Configures the operation to be called as part of the service node
     * 
     * @param operationId identifier of the operation as defined in OpenAPI
     * @return the builder
     */
    public RestServiceNodeBuilder operation(String operationId) {
        this.node.getWork().setParameter("Operation", operationId);

        return this;
    }

    /**
     * Selects the data object to be used as input of the service method call
     * 
     * @param name name of the data object
     * @return the builder
     */
    public RestServiceNodeBuilder fromDataObject(String name) {
        Variable var = this.workflowBuilder.get().getVariableScope().findVariable(name);

        if (var == null) {
            throw new IllegalArgumentException("Cannot find data object with '" + name + "' name");
        }

        DataAssociation in = new DataAssociation(name, name, null, null);
        node.addInAssociation(in);

        node.getWork()
                .addParameterDefinition(new ParameterDefinitionImpl(name, var.getType()));

        return this;
    }

    /**
     * Configures where the value of service method call should be assigned to - what data object should get
     * the value
     * 
     * @param name name of the data object
     * @param value value to be assigned
     * @return the builder
     */
    public RestServiceNodeBuilder toDataObject(String name, Object value) {

        DataAssociation out = new DataAssociation("Result", name, null, null);
        node.addOutAssociation(out);

        return this;
    }

    /**
     * Adds error handling node that allows to move to different path in case of an error
     * 
     * @param errorCodes list of codes to listen on
     * @return the builder
     */
    public ErrorNodeBuilder onError(String... errorCodes) {
        workflowBuilder.putOnContext(getNode());
        workflowBuilder.putBuilderOnContext(null);
        return new ErrorNodeBuilder("error on " + node.getName(), (String) this.node.getMetaData("UniqueId"), workflowBuilder)
                .errorCodes(errorCodes);
    }

    @Override
    protected Node getNode() {
        return this.node;
    }
}
