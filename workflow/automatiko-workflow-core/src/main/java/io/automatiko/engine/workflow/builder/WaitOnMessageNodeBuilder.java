package io.automatiko.engine.workflow.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.event.EventFilter;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.executable.core.Metadata;

/**
 * Builder responsible for building an start node
 */
public class WaitOnMessageNodeBuilder extends AbstractNodeBuilder {

    private EventNode node;

    private EventTypeFilter eventFilter;

    public WaitOnMessageNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new EventNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));

        this.node.setMetaData("functionFlowContinue", "true");
        this.node.setMetaData(Metadata.EVENT_TYPE, "message");
        this.node.setMetaData(Metadata.TRIGGER_TYPE, "ConsumeMessage");
        this.node.setMetaData(Metadata.TRIGGER_REF, name);
        this.eventFilter = new EventTypeFilter();
        eventFilter.setType("Message-" + name);
        List<EventFilter> eventFilters = new ArrayList<EventFilter>();
        eventFilters.add(eventFilter);
        this.node.setEventFilters(eventFilters);

        workflowBuilder.container().addNode(node);

        contect();
    }

    /**
     * Specifies the type of the message content. If not given it is taken from the data object payload will be mapped to.
     * 
     * @param type class of the message payload
     * @return the builder
     */
    public WaitOnMessageNodeBuilder type(Class<?> type) {
        node.setMetaData(Metadata.MESSAGE_TYPE, type.getCanonicalName());
        return this;
    }

    /**
     * Optional connector name to be used if there are more connectors used in the project.
     * If only one is defined as project dependency it is auto discovered.<br/>
     * Supported connectors are:
     * <ul>
     * <li>kafka</li>
     * <li>mqtt</li>
     * <li>amqp</li>
     * <li>camel</li>
     * <li>http</li>
     * <li>jms</li>
     * </ul>
     * 
     * @param connector one of the supported connectors
     * @return the builder
     */
    public WaitOnMessageNodeBuilder connector(String connector) {
        node.setMetaData("connector", connector);
        return this;
    }

    /**
     * Optional acknowledgment mode of messages.
     * 
     * Supported values:
     * 
     * <ul>
     * <li>none - no ack at all</li>
     * <li>manual - manual done on message level (default)</li>
     * <li>post - after message consumer is done with processing</li>
     * <li>pre - before message consumer starts processing</li>
     * </ul>
     * 
     * @param mode ack mode
     * @return the builder
     */
    public WaitOnMessageNodeBuilder ackMode(String mode) {
        node.setMetaData("ack-mode", mode);
        return this;
    }

    /**
     * Expression to be used to extract correlation key to be used to look up workflow instances for the message
     * 
     * @param expression correlation key expression
     * @return the builder
     */
    public WaitOnMessageNodeBuilder correlation(String expression) {
        node.setMetaData(Metadata.TRIGGER_CORRELATION_EXPR, expression);
        return this;
    }

    /**
     * Filter expression to be used to identify if given message should be processed
     * 
     * @param expression the filter expression
     * @return the builder
     */
    public WaitOnMessageNodeBuilder filter(String expression) {
        node.setMetaData(Metadata.TRIGGER_FILTER_EXPR, expression);
        return this;
    }

    /**
     * Name of the topic to listen on
     * 
     * @param topic name of the topic
     * @return the builder
     */
    public WaitOnMessageNodeBuilder topic(String topic) {
        node.setMetaData("topic", topic);
        return this;
    }

    /**
     * Endpoint URI to be used when using Apache Camel integration
     * 
     * @param uri end point uri
     * @return the builder
     */
    public WaitOnMessageNodeBuilder endpointUri(String uri) {
        node.setMetaData("url", uri);
        return this;
    }

    /**
     * Name of the data object the message payload should be mapped to
     * 
     * @param name data object name
     * @return the builder
     */
    public WaitOnMessageNodeBuilder toDataObject(String name) {
        if (name != null) {

            Variable var = workflowBuilder.get().getVariableScope().findVariable(name);
            if (var == null) {
                throw new IllegalArgumentException("No data object with name '" + name + " found");
            }

            node.setVariableName(name);
            node.addOutAssociation(new DataAssociation("event", name, null, null));
            node.setMetaData(Metadata.MESSAGE_TYPE, var.getType().getClassType().getCanonicalName());
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
    public WaitOnMessageNodeBuilder toDataObjectField(String name, Object value, String... fields) {

        String dotExpression = name;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }
        node.setVariableName(name);
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
    public WaitOnMessageNodeBuilder appendToDataObjectField(String name, String... fields) {

        String dotExpression = name;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }
        dotExpression += "[+]";
        node.setVariableName(name);
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
    public WaitOnMessageNodeBuilder removeFromDataObjectField(String name, String... fields) {

        String dotExpression = name;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }
        dotExpression += "[-]";
        node.setVariableName(name);
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
    public WaitOnMessageNodeBuilder customAttribute(String name, Object value) {
        return (WaitOnMessageNodeBuilder) super.customAttribute(name, value);
    }

}
