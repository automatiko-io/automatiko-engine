package io.automatiko.engine.workflow.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.Work;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
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

    public UserNodeBuilder dataObjectAsInput(String name) {

        return dataObjectAsInput(name, name);
    }

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
