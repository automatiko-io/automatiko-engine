package io.automatiko.engine.workflow.builder;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
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
        Variable var = this.workflowBuilder.get().getVariableScope().findVariable(parentWorkflowDataObjectName);

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

        Variable var = this.workflowBuilder.get().getVariableScope().findVariable(parentWorkflowDataObjectName);

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
