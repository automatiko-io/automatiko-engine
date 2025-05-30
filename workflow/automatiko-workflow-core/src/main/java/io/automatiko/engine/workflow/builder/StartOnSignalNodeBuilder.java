package io.automatiko.engine.workflow.builder;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.EventTrigger;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.executable.core.Metadata;

/**
 * Builder responsible for building an start node
 */
public class StartOnSignalNodeBuilder extends AbstractNodeBuilder {

    private StartNode node;

    private EventTrigger trigger;

    private EventTypeFilter eventFilter;

    public StartOnSignalNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        this(name, workflowBuilder, false);
    }

    public StartOnSignalNodeBuilder(String name, WorkflowBuilder workflowBuilder, boolean interrupting) {
        super(workflowBuilder);
        this.node = new StartNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));
        this.node.setInterrupting(interrupting);

        this.node.setMetaData(Metadata.TRIGGER_TYPE, "Signal");
        this.node.setMetaData(Metadata.TRIGGER_REF, name);
        this.trigger = new EventTrigger();
        this.eventFilter = new EventTypeFilter();
        eventFilter.setType(name);
        trigger.addEventFilter(eventFilter);

        this.node.addTrigger(trigger);

        workflowBuilder.container().addNode(node);
    }

    /**
     * Specifies the type of the message content. If not given it is taken from the data object payload will be mapped to.
     * 
     * @param type class of the message payload
     * @return the builder
     */
    public StartOnSignalNodeBuilder signal(String signal) {
        this.node.setMetaData(Metadata.MESSAGE_TYPE, signal);
        return this;
    }

    /**
     * Name of the data object the message payload should be mapped to
     * 
     * @param name data object name
     * @return the builder
     */
    public StartOnSignalNodeBuilder toDataObject(String name) {
        if (name != null) {

            Variable var = workflowBuilder.get().getVariableScope().findVariable(name);
            if (var == null) {
                throw new IllegalArgumentException("No data object with name '" + name + " found");
            }
            node.setMetaData(Metadata.TRIGGER_MAPPING, name);
            node.addOutAssociation(new DataAssociation("event", name, null, null));
            trigger.addInMapping(name, node.getOutMapping(name));

        }
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
     * @param name name of the data object
     * @param value value to be assigned
     * @param fields fields in data object to be accessed and last one to be set
     * @return the builder
     */
    public StartOnSignalNodeBuilder toDataObjectField(String name, Object value, String... fields) {

        String dotExpression = name;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }
        node.setMetaData(Metadata.TRIGGER_MAPPING, name);
        DataAssociation out = new DataAssociation("event", "#{" + dotExpression + "}", null, null);
        node.addOutAssociation(out);

        node.setMetaData(Metadata.MESSAGE_TYPE, this.node.getMetaData().getOrDefault(Metadata.MESSAGE_TYPE, "Object"));
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
     * @param name name of the data object
     * @param value value to be assigned
     * @param fields fields in data object to be accessed and last one to be set
     * @return the builder
     */
    public StartOnSignalNodeBuilder appendToDataObjectField(String name, String... fields) {

        String dotExpression = name;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }
        dotExpression += "[+]";
        node.setMetaData(Metadata.TRIGGER_MAPPING, name);
        DataAssociation out = new DataAssociation("event", "#{" + dotExpression + "}", null, null);
        node.addOutAssociation(out);
        node.setMetaData(Metadata.MESSAGE_TYPE, this.node.getMetaData().getOrDefault(Metadata.MESSAGE_TYPE, "Object"));
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
     * @param name name of the data object
     * @param value value to be assigned
     * @param fields fields in data object to be accessed and last one to be set
     * @return the builder
     */
    public StartOnSignalNodeBuilder removeFromDataObjectField(String name, String... fields) {

        String dotExpression = name;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }
        dotExpression += "[-]";
        node.setMetaData(Metadata.TRIGGER_MAPPING, name);
        DataAssociation out = new DataAssociation("event", "#{" + dotExpression + "}", null, null);
        node.addOutAssociation(out);
        node.setMetaData(Metadata.MESSAGE_TYPE, this.node.getMetaData().getOrDefault(Metadata.MESSAGE_TYPE, "Object"));
        return this;
    }

    @Override
    protected Node getNode() {
        return this.node;
    }

    /**
     * Sets custom attribute for this node
     * 
     * @param name name of the attribute, must not be null
     * @param value value of the attribute, must not be null
     * @return the builder
     */
    public StartOnSignalNodeBuilder customAttribute(String name, Object value) {
        return (StartOnSignalNodeBuilder) super.customAttribute(name, value);
    }

}
