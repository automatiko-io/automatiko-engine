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
public class StartOnMessageNodeBuilder extends AbstractNodeBuilder {

    private StartNode node;

    private EventTrigger trigger;

    private EventTypeFilter eventFilter;

    public StartOnMessageNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        this(name, workflowBuilder, false);
    }

    public StartOnMessageNodeBuilder(String name, WorkflowBuilder workflowBuilder, boolean interrupting) {
        super(workflowBuilder);
        this.node = new StartNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));
        this.node.setInterrupting(interrupting);

        this.node.setMetaData(Metadata.TRIGGER_TYPE, "ConsumeMessage");
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
    public StartOnMessageNodeBuilder type(Class<?> type) {
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
     * <li>pulsar</li>
     * <li>rabbitmq</li>
     * <li>direct</li>
     * </ul>
     * 
     * @param connector one of the supported connectors
     * @return the builder
     */
    public StartOnMessageNodeBuilder connector(String connector) {
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
    public StartOnMessageNodeBuilder ackMode(String mode) {
        node.setMetaData("ack-mode", mode);
        return this;
    }

    /**
     * Configures merge of multiple channels using merge mode - gets all the messages as they come, without any defined order.
     * Messages from different producers may be interleaved
     * 
     * @return the builder
     */
    public StartOnMessageNodeBuilder mergeChannels() {
        node.setMetaData("mergeMode", "MERGE");
        return this;
    }

    /**
     * Configures merge of multiple channels using one mode - picks a single producer, discarding the other producer
     * 
     * @return the builder
     */
    public StartOnMessageNodeBuilder pickOneFromChannels() {
        node.setMetaData("mergeMode", "ONE");
        return this;
    }

    /**
     * Configures merge of multiple channels using concat mode - concatenates the producers. The messages from one producer are
     * received until the messages from other producers are received
     * 
     * @return the builder
     */
    public StartOnMessageNodeBuilder concatChannels() {
        node.setMetaData("mergeMode", "CONCAT");
        return this;
    }

    /**
     * Expression to be used to extract correlation key to be used to look up workflow instances for the message
     * 
     * @param expression correlation key expression
     * @return the builder
     */
    public StartOnMessageNodeBuilder correlation(String expression) {
        node.setMetaData(Metadata.TRIGGER_CORRELATION_EXPR, expression);
        return this;
    }

    /**
     * Disables auto configuration of the connector for given consumer. Requires manual configuration via property files
     * 
     * @return the builder
     */
    public StartOnMessageNodeBuilder disableAutoConfiguration() {
        node.setMetaData("autoConfiguration", "false");
        return this;
    }

    /**
     * Expression to be used to extract correlation key to be used to look up workflow instances for the message
     * 
     * @param expression the correlation key expression
     * @return the builder
     */
    public <T> StartOnMessageNodeBuilder correlation(Correlation<T> expression) {

        node.setMetaData(Metadata.TRIGGER_CORRELATION_EXPR,
                BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()));
        return this;
    }

    /**
     * Filter expression to be used to identify if given message should be processed
     * 
     * @param expression the filter expression
     * @return the builder
     */
    public StartOnMessageNodeBuilder filter(String expression) {
        node.setMetaData(Metadata.TRIGGER_FILTER_EXPR, expression);
        return this;
    }

    /**
     * Filter expression to be used to identify if given message should be processed
     * 
     * @param expression the filter expression
     * @return the builder
     */
    public <T> StartOnMessageNodeBuilder filter(Filter<T> filter) {

        node.setMetaData(Metadata.TRIGGER_FILTER_EXPR,
                BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()));
        return this;
    }

    /**
     * Name of the channel to use, mainly for direct connector
     * 
     * @param name name of the channel
     * @return the builder
     */
    public StartOnMessageNodeBuilder channel(String name) {
        node.setMetaData(Metadata.TRIGGER_CHANNEL, name);
        return this;
    }

    /**
     * Name of the topic to listen on
     * 
     * @param topic name of the topic
     * @return the builder
     */
    public StartOnMessageNodeBuilder topic(String topic) {
        node.setMetaData("topic", topic);
        return this;
    }

    /**
     * Endpoint URI to be used when using Apache Camel integration
     * 
     * @param uri end point uri
     * @return the builder
     */
    public StartOnMessageNodeBuilder endpointUri(String uri) {
        node.setMetaData("url", uri);
        return this;
    }

    /**
     * Name of the data object the message payload should be mapped to
     * 
     * @param name data object name
     * @return the builder
     */
    public StartOnMessageNodeBuilder toDataObject(String name) {
        if (name != null) {

            Variable var = workflowBuilder.get().getVariableScope().findVariable(name);
            if (var == null) {
                throw new IllegalArgumentException("No data object with name '" + name + " found");
            }
            node.setMetaData(Metadata.TRIGGER_MAPPING, name);
            node.addOutAssociation(new DataAssociation("event", name, null, null));
            trigger.addInMapping(name, node.getOutMapping(name));
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
    public StartOnMessageNodeBuilder toDataObjectField(String name, Object value, String... fields) {

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
    public StartOnMessageNodeBuilder appendToDataObjectField(String name, String... fields) {

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
    public StartOnMessageNodeBuilder removeFromDataObjectField(String name, String... fields) {

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
     * Sets cloud events as message format for this message for this node
     * 
     * @return the builder
     */
    public StartOnMessageNodeBuilder cloudEvents() {
        return (StartOnMessageNodeBuilder) super.customAttribute("cloudEvents", true);
    }

    /**
     * Sets custom attribute for this node
     * 
     * @param name name of the attribute, must not be null
     * @param value value of the attribute, must not be null
     * @return the builder
     */
    public StartOnMessageNodeBuilder customAttribute(String name, Object value) {
        return (StartOnMessageNodeBuilder) super.customAttribute(name, value);
    }

}
