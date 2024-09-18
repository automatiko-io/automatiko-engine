package io.automatiko.engine.workflow.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.ForEachNode;
import io.automatiko.engine.workflow.process.executable.core.Metadata;

/**
 * Builder responsible for building an end node that sends a message
 */
public class SendMessageNodeBuilder extends AbstractNodeBuilder {

    private ActionNode node;

    private ForEachNode forEachNode;

    public SendMessageNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new ActionNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));

        this.node.setMetaData(Metadata.TRIGGER_REF, name);
        this.node.setMetaData(Metadata.TRIGGER_TYPE, "ProduceMessage");
        this.node.setMetaData("functionFlowContinue", "true");

        workflowBuilder.container().addNode(node);

        connect();
    }

    /**
     * Specifies the type of the message content. If not given it is taken from the data being mapped as the content.
     * 
     * @param type class of the message payload
     * @return the builder
     */
    public SendMessageNodeBuilder type(Class<?> type) {
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
    public SendMessageNodeBuilder connector(String connector) {
        node.setMetaData("connector", connector);
        return this;
    }

    /**
     * Name of the channel to use, mainly for direct connector
     * 
     * @param name name of the channel
     * @return the builder
     */
    public SendMessageNodeBuilder channel(String name) {
        node.setMetaData(Metadata.TRIGGER_CHANNEL, name);
        return this;
    }

    /**
     * Maps given data object to the payload of the message
     * 
     * @param name name of the data object
     * @return the builder
     */
    public SendMessageNodeBuilder fromDataObject(String name) {
        if (name != null) {

            Variable var;
            if (forEachNode != null) {
                var = ((VariableScope) forEachNode.getCompositeNode().getDefaultContext(VariableScope.VARIABLE_SCOPE))
                        .findVariable(name);
            } else {
                var = this.workflowBuilder.get().getVariableScope().findVariable(name);
            }
            if (var == null) {
                throw new IllegalArgumentException("No data object with name '" + name + " found");
            }
            node.setMetaData(Metadata.MAPPING_VARIABLE, name);
            node.setMetaData(Metadata.MESSAGE_TYPE, var.getType().getClassType().getCanonicalName());
        }
        return this;
    }

    /**
     * Creates data input based on given expression that will be evaluated at the service call
     * 
     * @param <T> type of data
     * @param type type of the data the expression will return
     * @param expression expression to be evaluated
     * @return the builder
     */
    public <T> SendMessageNodeBuilder expressionAsInput(Class<T> type, Supplier<T> expression) {

        ObjectDataType dataType = new ObjectDataType(type);

        String source = "#{"
                + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()) + "}";

        node.setMetaData(Metadata.MAPPING_VARIABLE, source);
        node.setMetaData(Metadata.MESSAGE_TYPE, dataType.getClassType().getCanonicalName());
        return this;
    }

    /**
     * NOTE: Applies to MQTT connector only<br/>
     * The topic expression to be used while sending message. It is used when the topic needs to be calculated and it is not
     * a constant that comes from the name of the node
     * 
     * @param expression expression to be evaluated to get the topic name
     * @return the builder
     */
    public SendMessageNodeBuilder mqttTopic(String expression) {
        node.setMetaData("topicExpression", expression);
        return this;
    }

    /**
     * The topic to be used while sending message.
     * 
     * @param topic destination topic
     * @return the builder
     */
    public SendMessageNodeBuilder topic(String topic) {
        node.setMetaData("topic", topic);
        return this;
    }

    /**
     * NOTE: Applies to KAFKA connector only<br/>
     * The key expression to be used while sending message. By default the <code>businessKey</code> of the workflow
     * instance is used as Kafka record key, in case it should be something else an expression can be provided
     * 
     * @param expression expression to be evaluated to get the key
     * @return the builder
     */
    public SendMessageNodeBuilder kafkaKey(String expression) {
        node.setMetaData("keyExpression", expression);
        return this;
    }

    /**
     * NOTE: Applies to AMQP connector only<br/>
     * The address expression to be used while sending message. It is used when the address needs to be calculated and it is not
     * a constant that comes from the name of the node
     * 
     * @param expression expression to be evaluated to get the topic name
     * @return the builder
     */
    public SendMessageNodeBuilder amqpAddress(String expression) {
        node.setMetaData("addressExpression", expression);
        return this;
    }

    /**
     * NOTE: Applies to RabbitMQ connector only<br/>
     * The routing key expression to be used while sending message. It is used when the routing key needs to be calculated and
     * it is not
     * a constant that comes from the name of the node
     * 
     * @param expression expression to be evaluated to get the topic name
     * @return the builder
     */
    public SendMessageNodeBuilder rabbitmqRoutingKey(String expression) {
        node.setMetaData("routingKeyExpression", expression);
        return this;
    }

    /**
     * Optional header value that is used by following connectors
     * <ul>
     * <li>jms - header names must start with <code>JMS</code></li>
     * <li>camel - header names must start with <code>Camel</code></li>
     * <li>http - header names must start with <code>HTTM</code></li>
     * </ul>
     * 
     * @param name name of the header
     * @param value value of the header
     * @return the builder
     */
    public SendMessageNodeBuilder header(String name, String value) {
        node.setMetaData(name, name);
        return this;
    }

    /**
     * Endpoint URI to be used when using Apache Camel integration
     * 
     * @param uri end point uri
     * @return the builder
     */
    public SendMessageNodeBuilder endpointUri(String uri) {
        node.setMetaData("url", uri);
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
    public SendMessageNodeBuilder repeat(Supplier<Collection<?>> inputCollectionExpression) {
        return repeat("#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                + "}", "item");
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
    public SendMessageNodeBuilder repeat(Supplier<Collection<?>> inputCollectionExpression, String inputName) {
        return repeat("#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                + "}", inputName);
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
    public SendMessageNodeBuilder repeat(String dataObjectName) {
        return repeat(dataObjectName, "item");
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
    public SendMessageNodeBuilder repeat(String dataObjectName, String inputName) {

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

        forEachNode.addNode(origNode);

        VariableScope variableScope = ((VariableScope) forEachNode.getCompositeNode()
                .getDefaultContext(VariableScope.VARIABLE_SCOPE));

        for (DataAssociation in : ((ActionNode) origNode).getInAssociations()) {
            String varName = in.getSources().get(0);
            Variable var = workflowBuilder.get().getVariableScope().findVariable(varName);
            if (var != null) {
                variableScope.addVariable(var);
            }
        }

        for (DataAssociation out : ((ActionNode) origNode).getOutAssociations()) {
            String varName = out.getTarget();
            Variable var = workflowBuilder.get().getVariableScope().findVariable(varName);
            if (var != null) {
                variableScope.addVariable(var);
            }
        }

        forEachNode.linkIncomingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE, node.getId(),
                NodeImpl.CONNECTION_DEFAULT_TYPE);
        forEachNode.linkOutgoingConnections(node.getId(), NodeImpl.CONNECTION_DEFAULT_TYPE,
                NodeImpl.CONNECTION_DEFAULT_TYPE);

        workflowBuilder.container().addNode(forEachNode);

        connect();

        return this;
    }

    @Override
    protected Node getNode() {
        if (forEachNode != null) {
            return forEachNode;
        }

        return this.node;
    }

    /**
     * Sets cloud events as message format for this message for this node
     * 
     * @return the builder
     */
    public SendMessageNodeBuilder cloudEvents() {
        return (SendMessageNodeBuilder) super.customAttribute("cloudEvents", true);
    }

    /**
     * Sets binary cloud events as message structure for this message for this node
     * 
     * @return the builder
     */
    public SendMessageNodeBuilder cloudEventsBinary() {
        cloudEvents();
        return (SendMessageNodeBuilder) super.customAttribute("cloudEventsBinary", true);
    }

    /**
     * Sets custom attribute for this node
     * 
     * @param name name of the attribute, must not be null
     * @param value value of the attribute, must not be null
     * @return the builder
     */
    public SendMessageNodeBuilder customAttribute(String name, Object value) {
        return (SendMessageNodeBuilder) super.customAttribute(name, value);
    }
}
