package io.automatiko.engine.workflow.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.Work;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.base.core.impl.ParameterDefinitionImpl;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.HumanTaskNode;
import io.automatiko.engine.workflow.process.executable.core.Metadata;

/**
 * Builder responsible for building an user (tasks assigned to human actors) node
 */
public class UserNodeBuilder extends AbstractNodeBuilder {

    private HumanTaskNode node;

    private Work work;

    public UserNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new HumanTaskNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));

        this.node.setMetaData(Metadata.DATA_INPUTS, new HashMap<>());
        this.node.setMetaData(Metadata.DATA_OUTPUTS, new HashMap<>());

        this.work = this.node.getWork();
        work.setName("Human Task");

        this.work.setParameter("TaskName", toCamelCase(name));

        workflowBuilder.get().addNode(node);

        contect();
    }

    /**
     * Specifies users that should be eligible to claim and work on this task
     * 
     * @param users list of users
     * @return the builder
     */
    public UserNodeBuilder users(String... users) {
        this.work.setParameter("ActorId", Stream.of(users).collect(Collectors.joining(",")));
        return this;
    }

    /**
     * Specifies groups that should be eligible to claim and work on this task
     * 
     * @param groups list of groups
     * @return the builder
     */
    public UserNodeBuilder groups(String... groups) {
        this.work.setParameter("Groups", Stream.of(groups).collect(Collectors.joining(",")));
        return this;
    }

    /**
     * Specifies what users should be excluded from being able to work on this task
     * 
     * @param users
     * @return the builder
     */
    public UserNodeBuilder excludedUsers(String... users) {
        this.work.setParameter("ExcludedUsers", Stream.of(users).collect(Collectors.joining(",")));
        return this;
    }

    /**
     * Specifies description of this task, can include expressions in format <code>#{dataObjectName}</code>
     * 
     * @param description description of the task
     * @return the builder
     */
    public UserNodeBuilder description(String description) {
        addToDataInputs("Description", String.class.getCanonicalName());
        this.work.setParameter("Description", description);
        return this;
    }

    /**
     * Creates data input based on value of the data object given by name. Name of the input is the same as
     * name of the data object used as source.
     * 
     * @param name name of the data object
     * @return the builder
     */
    public UserNodeBuilder dataObjectAsInput(String name) {

        return dataObjectAsInput(name, name);
    }

    /**
     * Creates data input based on value of the data object given by name
     * 
     * @param name name of the data object to be used as source of the input of the task
     * @param inputName name of the input on the user task
     * @return the builder
     */
    public UserNodeBuilder dataObjectAsInput(String name, String inputName) {
        Variable var = this.workflowBuilder.get().getVariableScope().findVariable(name);

        if (var == null) {
            throw new IllegalArgumentException("Cannot find data object with '" + name + "' name");
        }
        this.work.addParameterDefinition(new ParameterDefinitionImpl(inputName, var.getType()));
        this.node.addInAssociation(new DataAssociation(name, inputName, null, null));
        addToDataInputs(inputName, var.getType().getStringType());
        return this;
    }

    /**
     * Creates data input based on literal value
     * 
     * @param <T> type of the literal
     * @param inputName name of the input
     * @param value value to be associated with the input
     * @return the builder
     */
    public <T> UserNodeBuilder literalAsInput(String inputName, T value) {

        ObjectDataType type = new ObjectDataType(value.getClass());
        this.work.addParameterDefinition(new ParameterDefinitionImpl(inputName, type));
        if (value instanceof String) {
            this.work.setParameter(inputName, value.toString());
        } else {
            this.work.setParameter(inputName, "#{" + value.toString().replace("\"", "\\\"") + "}");
        }
        addToDataInputs(inputName, type.getStringType());
        return this;
    }

    /**
     * Creates data input based on given expression that will be evaluated at the service call
     * 
     * @param <T> type of data
     * @param inputName name of the input the value will be associated with
     * @param type type of the data the expression will return
     * @param expression expression to be evaluated
     * @return the builder
     */
    public <T> UserNodeBuilder expressionAsInput(String inputName, Class<T> type, Supplier<T> expression) {

        ObjectDataType dataType = new ObjectDataType(type);
        this.work.addParameterDefinition(new ParameterDefinitionImpl(inputName, dataType));
        this.work.setParameter(inputName,
                "#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                        + "}");
        addToDataInputs(inputName, dataType.getStringType());
        return this;
    }

    /**
     * Maps give output to the data object. This will take the output given by user and assign to the data object
     * 
     * @param name name of the output expected to be provided by user
     * @param dataObjectName data object name
     * @return the builder
     */
    public UserNodeBuilder outputToDataObject(String name, String dataObjectName) {
        Variable var = this.workflowBuilder.get().getVariableScope().findVariable(dataObjectName);

        if (var == null) {
            throw new IllegalArgumentException("Cannot find data object with '" + dataObjectName + "' name");
        }

        this.node.addOutAssociation(new DataAssociation(name, dataObjectName, null, null));
        addToDataOutputs(name, var.getType().getStringType());
        return this;
    }

    /**
     * Maps the given value into a data object's field(s). Fields are accessed using getters and then set via setter method
     * so it is expected that data object follow Java Bean convention.
     * 
     * If there are many fields given only the last one will be set with the value and other will be used to navigate to it
     * Following method <code>toDataObjectField("person", "abc", "address", "street")</code>
     * will essentially mean <code>person.getAddress().setStreet("abc")</code>
     * 
     * @param outputName name of the output of the task
     * @param dataObjectName name of the data object
     * @param fields fields in data object to be accessed and last one to be set
     * @return the builder
     */
    public UserNodeBuilder toDataObjectField(String outputName, String dataObjectName, String... fields) {

        String dotExpression = dataObjectName;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }

        DataAssociation out = new DataAssociation(outputName, "#{" + dotExpression + "}", null, null);
        node.addOutAssociation(out);
        return this;
    }

    /**
     * Appends given value to a list based data object (or its field(s) when set). Fields are accessed using getters and then
     * set via setter method
     * so it is expected that data object follow Java Bean convention.
     * 
     * If there are many fields given only the last one will be set with the value and other will be used to navigate to it
     * Following method <code>appendToDataObjectField("person", "abc", "contact", "phones")</code>
     * will essentially mean <code>person.getContact().getPhones.add("abc")</code>
     * 
     * @param outputName name of the output of the task
     * @param dataObjectName name of the data object
     * @param fields fields in data object to be accessed and last one to be set
     * @return the builder
     */
    public UserNodeBuilder appendToDataObjectField(String outputName, String dataObjectName, String... fields) {

        String dotExpression = dataObjectName;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }
        dotExpression += "[+]";

        DataAssociation out = new DataAssociation(outputName, "#{" + dotExpression + "}", null, null);
        node.addOutAssociation(out);
        return this;
    }

    /**
     * Removes given value from a list based data object (or its field(s) when set). Fields are accessed using getters and then
     * set via setter method
     * so it is expected that data object follow Java Bean convention.
     * 
     * If there are many fields given only the last one will be set with the value and other will be used to navigate to it
     * Following method <code>removeFromDataObjectField("person", "abc", "contact", "phones")</code>
     * will essentially mean <code>person.getContact().getPhones.remove("abc")</code>
     * 
     * @param outputName name of the output of the task
     * @param dataObjectName name of the data object
     * @param fields fields in data object to be accessed and last one to be set
     * @return the builder
     */
    public UserNodeBuilder removeFromDataObjectField(String outputName, String dataObjectName, String... fields) {

        String dotExpression = dataObjectName;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }
        dotExpression += "[-]";

        DataAssociation out = new DataAssociation(outputName, "#{" + dotExpression + "}", null, null);
        node.addOutAssociation(out);
        return this;
    }

    /**
     * Specifies what form template should be used when presenting to users
     * 
     * This requires <code>automatiko-user-tasks-management-addon</code> to be added to project dependencies
     * 
     * @param form name of the form template to be used
     * @return the builder
     */
    public UserNodeBuilder form(String form) {
        this.work.setParameter("FormName", form);
        return this;
    }

    /**
     * Specifies what email subject should be used when sending email notifications. The email subject can
     * include references to data objects via <code>#{dataObjectName}</code> constructs
     * 
     * This requires <code>automatiko-user-tasks-email-addon</code> to be added to project dependencies
     * 
     * @param emailSubject email subject to be used when sending notifications
     * @return the builder
     */
    public UserNodeBuilder notificationEmailSubject(String emailSubject) {
        this.work.setParameter("EmailSubject", emailSubject);
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

    private String toCamelCase(String name) {
        if (name == null) {
            return null;
        }

        StringBuilder converted = new StringBuilder();
        int index = 0;
        for (String item : name.split("[\\W_]+")) {

            if (index == 0) {
                converted.append(item.toLowerCase());
            } else {
                converted.append(StringUtils.capitalize(item.toLowerCase()));
            }

            index++;
        }

        return converted.toString();
    }
}
