package io.automatiko.engine.workflow.builder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.ForEachNode;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.executable.core.Metadata;

/**
 * Builder responsible for building an user (tasks assigned to human actors) node
 */
public class SubWorkflowNodeBuilder extends AbstractNodeBuilder {

    private SubProcessNode node;

    private ForEachNode forEachNode;

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

        workflowBuilder.container().addNode(node);

        connect();
    }

    public SubWorkflowNodeBuilder dataObjectAsInput(String name) {

        return dataObjectAsInput(name, name);
    }

    public SubWorkflowNodeBuilder dataObjectAsInput(String name, String inputName) {
        Variable var;
        if (forEachNode != null) {
            var = ((VariableScope) forEachNode.getCompositeNode().getDefaultContext(VariableScope.VARIABLE_SCOPE))
                    .findVariable(name);
        } else {
            var = this.workflowBuilder.get().getVariableScope().findVariable(name);
        }

        if (var == null) {
            throw new IllegalArgumentException("Cannot find data object with '" + name + "' name");
        }
        this.node.addInAssociation(new DataAssociation(name, inputName, null, null));
        addToDataInputs(inputName, var.getType().getStringType());
        return this;
    }

    /**
     * Creates data input for the service call based on given literal value
     * 
     * @param <T> type of data
     * @param inputName name of the data input to be created
     * @param value actual literal value
     * @return the builder
     */
    public <T> SubWorkflowNodeBuilder literalAsInput(String inputName, T value) {

        ObjectDataType type = new ObjectDataType(value.getClass());
        String source;

        if (value instanceof String) {
            source = value.toString();
        } else {
            source = "#{"
                    + value.toString().replace("\"", "\\\"") + "}";
        }
        this.node.addInAssociation(new DataAssociation(source, inputName, null, null));
        addToDataInputs(inputName, type.getStringType());

        return this;
    }

    /**
     * Creates data input for the service call based on given expression that will be evaluated at the service call
     * 
     * @param <T> type of data
     * @param inputName name of the data input to be created
     * @param clazz type of data that is going to be returned from the expression
     * @param expression expression to be evaluated
     * @return the builder
     */
    public <T> SubWorkflowNodeBuilder expressionAsInput(String inputName, Class<T> clazz, Supplier<T> expression) {

        ObjectDataType dataType = new ObjectDataType(clazz);
        String source = "#{"
                + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"") + "}";
        this.node.addInAssociation(new DataAssociation(source, inputName, null, null));
        addToDataInputs(inputName, dataType.getStringType());
        return this;
    }

    /**
     * Creates mapping between data object of the sub workflow and the parent workflow. <code>name</code> is the data object in
     * sub workflow, while <code>dataObjectName</code> is the data object in the parent workflow.
     * 
     * @param subworkflowDataObjectName name of the data object in sub workflow to be used as source
     * @param parentWorkflowDataObjectName name of the data object in parent workflow to be used as target
     * @return the builder
     */
    public SubWorkflowNodeBuilder outputToDataObject(String subworkflowDataObjectName, String parentWorkflowDataObjectName) {
        Variable var;
        if (forEachNode != null) {
            var = ((VariableScope) forEachNode.getCompositeNode().getDefaultContext(VariableScope.VARIABLE_SCOPE))
                    .findVariable(parentWorkflowDataObjectName);
        } else {
            var = this.workflowBuilder.get().getVariableScope().findVariable(parentWorkflowDataObjectName);
        }
        if (var == null) {
            throw new IllegalArgumentException("Cannot find data object with '" + parentWorkflowDataObjectName + "' name");
        }

        this.node.addOutAssociation(new DataAssociation(subworkflowDataObjectName, parentWorkflowDataObjectName, null, null));
        addToDataOutputs(subworkflowDataObjectName, var.getType().getStringType());
        return this;
    }

    /**
     * Maps the given value into a data object's field(s). Fields are accessed using getters and then set via setter method
     * so it is expected that data object follow Java Bean convention.
     * 
     * If there are many fields given only the last one will be set with the value and other will be used to navigate to it
     * Following method <code>toDataObjectField(selectedStreet", "person", "address", "street")</code>
     * will essentially mean <code>person.getAddress().setStreet("abc")</code>
     * 
     * @param subworkflowDataObjectName name of the data object in sub workflow to be used as source
     * @param parentWorkflowDataObjectName name of the data object in parent workflow to be used as target
     * @param fields fields in data object to be accessed and last one to be set
     * @return the builder
     */
    public SubWorkflowNodeBuilder toDataObjectField(String subworkflowDataObjectName, String parentWorkflowDataObjectName,
            String... fields) {

        Variable var;
        if (forEachNode != null) {
            var = ((VariableScope) forEachNode.getCompositeNode().getDefaultContext(VariableScope.VARIABLE_SCOPE))
                    .findVariable(parentWorkflowDataObjectName);
        } else {
            var = this.workflowBuilder.get().getVariableScope().findVariable(parentWorkflowDataObjectName);
        }

        if (var == null) {
            throw new IllegalArgumentException("Cannot find data object with '" + parentWorkflowDataObjectName + "' name");
        }

        String dotExpression = parentWorkflowDataObjectName;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }

        DataAssociation out = new DataAssociation(subworkflowDataObjectName, "#{" + dotExpression + "}", null, null);
        node.addOutAssociation(out);
        addToDataOutputs(subworkflowDataObjectName, resolveTypeOfField(var, fields));
        return this;
    }

    /**
     * Appends given value to a list based data object (or its field(s) when set). Fields are accessed using getters and then
     * set via setter method
     * so it is expected that data object follow Java Bean convention.
     * 
     * If there are many fields given only the last one will be set with the value and other will be used to navigate to it
     * Following method <code>appendToDataObjectField(selectedPhone", "person", "contact", "phones")</code>
     * will essentially mean <code>person.getContact().getPhones.add("abc")</code>
     * 
     * @param subworkflowDataObjectName name of the data object in sub workflow to be used as source
     * @param parentWorkflowDataObjectName name of the data object in parent workflow to be used as target
     * @param fields fields in data object to be accessed and last one to be set
     * @return the builder
     */
    public SubWorkflowNodeBuilder appendToDataObjectField(String subworkflowDataObjectName, String parentWorkflowDataObjectName,
            String... fields) {

        String dotExpression = parentWorkflowDataObjectName;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }
        dotExpression += "[+]";

        DataAssociation out = new DataAssociation(subworkflowDataObjectName, "#{" + dotExpression + "}", null, null);
        node.addOutAssociation(out);
        return this;
    }

    /**
     * Removes given value from a list based data object (or its field(s) when set). Fields are accessed using getters and then
     * set via setter method
     * so it is expected that data object follow Java Bean convention.
     * 
     * If there are many fields given only the last one will be set with the value and other will be used to navigate to it
     * Following method <code>removeFromDataObjectField("selectedPhone", "person", "contact", "phones")</code>
     * will essentially mean <code>person.getContact().getPhones.remove("abc")</code>
     * 
     * @param subworkflowDataObjectName name of the data object in sub workflow to be used as source
     * @param parentWorkflowDataObjectName name of the data object in parent workflow to be used as target
     * @param fields fields in data object to be accessed and last one to be set
     * @return the builder
     */
    public SubWorkflowNodeBuilder removeFromDataObjectField(String subworkflowDataObjectName,
            String parentWorkflowDataObjectName, String... fields) {

        String dotExpression = parentWorkflowDataObjectName;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }
        dotExpression += "[-]";

        DataAssociation out = new DataAssociation(subworkflowDataObjectName, "#{" + dotExpression + "}", null, null);
        node.addOutAssociation(out);
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

    /**
     * Instructs to repeat this node based on the input collection. This will create new node for each element in the
     * collection.
     * <br/>
     * The item will be named <code>item</code> and as such can be easily accessed by node data mapping<br/>
     * 
     * @param inputCollectionExpression expression that will deliver collection to repeat service on each item
     * @return the builder
     */
    public SubWorkflowNodeBuilder repeat(Supplier<Collection<?>> inputCollectionExpression) {
        return repeat("#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                + "}", "item", null, null);
    }

    /**
     * Instructs to repeat this node based on the input collection and collect all results into the output collection.
     * This will create new node for each element in the input collection.
     * <br/>
     * The input element will be named <code>item</code> and as such can be easily accessed by node data mapping<br/>
     * The output element will be named <code>outItem</code> and as such can be easily accessed by node data output
     * 
     * @param inputCollectionExpression expression that will deliver collection to repeat service on each item
     * @param outputCollectionExpression expression that will deliver collection that will be populated with results of calling
     *        the service
     * @return the builder
     */
    public SubWorkflowNodeBuilder repeat(Supplier<Collection<?>> inputCollectionExpression,
            Supplier<Collection<?>> outputCollectionExpression) {
        return repeat("#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                + "}", "item",
                "#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                        + "}",
                "outItem");
    }

    /**
     * Instructs to repeat this node based on the input collection. This will create new node for each element in the
     * collection.
     * <br/>
     * The item will be named as given with <code>inputName</code> and as such can be easily accessed by node data
     * mapping<br/>
     * 
     * @param inputCollectionExpression expression that will deliver collection to repeat service on each item
     * @return the builder
     */
    public SubWorkflowNodeBuilder repeat(Supplier<Collection<?>> inputCollectionExpression, String inputName) {
        return repeat("#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                + "}", inputName, null, null);
    }

    /**
     * Instructs to repeat this node based on the input collection and collect all results into the output collection.
     * This will create new node for each element in the input collection.
     * <br/>
     * The input element will be named based on given <code>inputName</code> and as such can be easily accessed by node
     * data mapping<br/>
     * The output element will be named based on given <code>outputName</code> and as such can be easily accessed by
     * node data output
     * mapping<br/>
     * 
     * @param inputCollectionExpression expression that will deliver collection to repeat service on each item
     * @param outputCollectionExpression expression that will deliver collection that will be populated with results of calling
     *        the service
     * @return the builder
     */
    public SubWorkflowNodeBuilder repeat(Supplier<Collection<?>> inputCollectionExpression, String inputName,
            Supplier<Collection<?>> outputCollectionExpression, String outputName) {
        return repeat("#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                + "}", inputName,
                "#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                        + "}",
                outputName);
    }

    /**
     * Instructs to repeat this node based on the data object that is given via <code>dataObjectName</code>. That data object
     * must
     * be of type collection. This will create new node for each element in the
     * collection.
     * <br/>
     * The item will be named <code>item</code> and as such can be easily accessed by node data mapping<br/>
     * 
     * @param dataObjectName name of the data object (of type collection) that should be used as input collection
     * @return the builder
     */
    public SubWorkflowNodeBuilder repeat(String dataObjectName) {
        return repeat(dataObjectName, "item", null, null);
    }

    /**
     * Instructs to repeat this node based on the data object that is given via <code>dataObjectName</code>. That data object
     * must
     * be of type collection. This will create new node for each element in the
     * collection.
     * <br/>
     * The item will be named as given by <code>inputName</code> and as such can be easily accessed by node data
     * mapping<br/>
     * 
     * @param dataObjectName name of the data object (of type collection) that should be used as input collection
     * @param inputName name of the item of the collection to be referenced by task
     * @return the builder
     */
    public SubWorkflowNodeBuilder repeat(String dataObjectName, String inputName) {
        return repeat(dataObjectName, inputName, null, null);
    }

    /**
     * Instructs to repeat this node based on the data object that is given via <code>dataObjectName</code>. That data object
     * must be of type collection. This will create new node for each element in the collection. At the end each result will be
     * collected and added to the
     * output collection that is represented by data object named <code>toDataObject</code>
     * <br/>
     * The item will be named as given by <code>inputName</code> and as such can be easily accessed by node data
     * mapping<br/>
     * 
     * @param dataObjectName name of the data object (of type collection) that should be used as input collection
     * @param inputName name of the item of the collection to be referenced by task
     * @param toDataObject name of the data object that the results should be added to
     * @return the builder the builder
     */
    public SubWorkflowNodeBuilder repeat(String dataObjectName, String inputName, String toDataObject) {
        return repeat(dataObjectName, inputName, toDataObject, "outItem");
    }

    /**
     * Instructs to repeat this node based on the data object that is given via <code>dataObjectName</code>. That data object
     * must be of type collection. This will create new node for each element in the collection. At the end each result will be
     * collected and added to the
     * output collection that is represented by data object named <code>toDataObject</code>
     * <br/>
     * The item will be named as given by <code>inputName</code> and as such can be easily accessed by node data
     * mapping<br/>
     * 
     * @param dataObjectName name of the data object (of type collection) that should be used as input collection
     * @param inputName name of the item of the collection to be referenced by task
     * @param toDataObject name of the data object that the results should be added to
     * @param outputName name of the result of the execution that will be collected to the output collection
     * @return the builder the builder
     */
    public SubWorkflowNodeBuilder repeat(String dataObjectName, String inputName, String toDataObject, String outputName) {
        Node origNode = getNode();

        workflowBuilder.container().removeNode(origNode);
        new ArrayList<>(getNode().getIncomingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE)).forEach(conn -> {
            getNode().removeIncomingConnection(NodeImpl.CONNECTION_DEFAULT_TYPE, conn);

            ((Node) conn.getFrom()).removeOutgoingConnection(NodeImpl.CONNECTION_DEFAULT_TYPE, conn);
        });

        forEachNode = new ForEachNode();
        forEachNode.setId(ids.incrementAndGet());
        forEachNode.setName("Repeat of " + getNode().getName());
        forEachNode.setMetaData("UniqueId", origNode.getMetaData().get("UniqueId"));
        forEachNode.setCollectionExpression(dataObjectName);
        forEachNode.setVariable(inputName, new ObjectDataType(resolveItemType(dataObjectName)));

        if (toDataObject != null) {
            forEachNode.setOutputVariable(outputName, new ObjectDataType(resolveItemType(toDataObject)));
            forEachNode.setOutputCollectionExpression(toDataObject);
        }
        CompositeContextNode subProcessNode = new CompositeContextNode();
        VariableScope variableScope = new VariableScope();
        subProcessNode.addContext(variableScope);
        subProcessNode.setDefaultContext(variableScope);
        subProcessNode.setAutoComplete(true);
        subProcessNode.setMetaData("hidden", true);
        subProcessNode.setMetaData("UniqueId", origNode.getMetaData().get("UniqueId") + ":container");
        subProcessNode.setName(node.getName() + " (Wrapper)");
        subProcessNode.setCancelRemainingInstances(false);

        StartNode startNode = new StartNode();
        startNode.setName("");
        startNode.setMetaData("UniqueId", origNode.getMetaData().get("UniqueId") + ":start");
        startNode.setMetaData("hidden", true);
        subProcessNode.addNode(startNode);

        subProcessNode.addNode(origNode);

        EndNode endNode = new EndNode();
        endNode.setName("");
        endNode.setMetaData("UniqueId", origNode.getMetaData().get("UniqueId") + ":end");
        endNode.setMetaData("hidden", true);
        subProcessNode.addNode(endNode);

        ConnectionImpl connection = new ConnectionImpl(startNode, Node.CONNECTION_DEFAULT_TYPE, origNode,
                Node.CONNECTION_DEFAULT_TYPE);
        connection.setMetaData("UniqueId", "SequenceFlow_" + origNode.getMetaData().get("UniqueId") + ":start");

        ConnectionImpl connection3 = new ConnectionImpl(origNode, Node.CONNECTION_DEFAULT_TYPE, endNode,
                Node.CONNECTION_DEFAULT_TYPE);
        connection3.setMetaData("UniqueId", "SequenceFlow_" + origNode.getMetaData().get("UniqueId") + ":end");

        forEachNode.addNode(subProcessNode);

        variableScope = ((VariableScope) forEachNode.getCompositeNode()
                .getDefaultContext(VariableScope.VARIABLE_SCOPE));

        for (DataAssociation in : ((SubProcessNode) origNode).getInAssociations()) {
            String varName = in.getSources().get(0);
            Variable var = workflowBuilder.get().getVariableScope().findVariable(varName);
            if (var != null) {
                variableScope.addVariable(var);
            }
        }

        for (DataAssociation out : ((SubProcessNode) origNode).getOutAssociations()) {
            String varName = out.getTarget();
            Variable var = workflowBuilder.get().getVariableScope().findVariable(varName);
            if (var != null) {
                variableScope.addVariable(var);
            }
        }

        forEachNode.linkIncomingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE, subProcessNode.getId(),
                NodeImpl.CONNECTION_DEFAULT_TYPE);
        forEachNode.linkOutgoingConnections(subProcessNode.getId(), NodeImpl.CONNECTION_DEFAULT_TYPE,
                NodeImpl.CONNECTION_DEFAULT_TYPE);

        workflowBuilder.container().addNode(forEachNode);

        connect();

        workflowBuilder.container(subProcessNode);

        return this;
    }

    public WorkflowBuilder endRepeatAndThen() {
        workflowBuilder.putOnContext(forEachNode);
        workflowBuilder.putBuilderOnContext(null);
        workflowBuilder.container(workflowBuilder.get());
        return workflowBuilder;
    }

    @Override
    protected Node getNode() {
        if (forEachNode != null) {
            return forEachNode;
        }

        return this.node;
    }

    /**
     * Sets custom attribute for this node
     * 
     * @param name name of the attribute, must not be null
     * @param value value of the attribute, must not be null
     * @return the builder
     */
    public SubWorkflowNodeBuilder customAttribute(String name, Object value) {
        return (SubWorkflowNodeBuilder) super.customAttribute(name, value);
    }

    @SuppressWarnings("unchecked")
    private void addToDataInputs(String name, String type) {
        ((Map<String, String>) this.node.getMetaData(Metadata.DATA_INPUTS)).put(name, type);
    }

    @SuppressWarnings("unchecked")
    private void addToDataOutputs(String name, String type) {
        ((Map<String, String>) this.node.getMetaData(Metadata.DATA_OUTPUTS)).put(name, type);
    }

    private String resolveTypeOfField(Variable var, String[] fields) {
        Class<?> clazz = var.getType().getClassType();
        int counter = 0;
        while (counter < fields.length) {
            Field field;
            try {
                field = clazz.getDeclaredField(fields[counter]);
                clazz = field.getType();

            } catch (NoSuchFieldException | SecurityException e) {
                try {
                    field = clazz.getField(fields[counter]);
                    clazz = field.getType();

                } catch (NoSuchFieldException | SecurityException e1) {
                }
            }
            counter++;
        }
        return clazz.getCanonicalName();
    }

}
