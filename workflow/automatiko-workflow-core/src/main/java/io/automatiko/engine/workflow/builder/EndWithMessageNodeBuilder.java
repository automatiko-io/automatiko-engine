package io.automatiko.engine.workflow.builder;

import java.util.function.Supplier;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.executable.core.Metadata;

/**
 * Builder responsible for building an end node that sends a message
 */
public class EndWithMessageNodeBuilder extends AbstractNodeBuilder {

    private EndNode node;

    public EndWithMessageNodeBuilder(String name, boolean terminate, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new EndNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));
        this.node.setTerminate(false);

        this.node.setMetaData(Metadata.TRIGGER_REF, name);
        this.node.setMetaData(Metadata.TRIGGER_TYPE, "ProduceMessage");

        workflowBuilder.container().addNode(node);

        contect();
    }

    /**
     * Specifies the type of the message content. If not given it is taken from the data being mapped as the content.
     * 
     * @param type class of the message payload
     * @return the builder
     */
    public EndWithMessageNodeBuilder type(Class<?> type) {
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
    public EndWithMessageNodeBuilder connector(String connector) {
        node.setMetaData("connector", connector);
        return this;
    }

    /**
     * Maps given data object to the payload of the message
     * 
     * @param name name of the data object
     * @return the builder
     */
    public EndWithMessageNodeBuilder fromDataObject(String name) {
        if (name != null) {

            Variable var = workflowBuilder.get().getVariableScope().findVariable(name);
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
    public <T> EndWithMessageNodeBuilder expressionAsInput(Class<T> type, Supplier<T> expression) {

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
    public EndWithMessageNodeBuilder mqttTopic(String expression) {
        node.setMetaData("topicExpression", expression);
        return this;
    }

    /**
     * The topic to be used while sending message.
     * 
     * @param topic destination topic
     * @return the builder
     */
    public EndWithMessageNodeBuilder topic(String topic) {
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
    public EndWithMessageNodeBuilder kafkaKey(String expression) {
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
    public EndWithMessageNodeBuilder amqpAddress(String expression) {
        node.setMetaData("addressExpression", expression);
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
    public EndWithMessageNodeBuilder header(String name, String value) {
        node.setMetaData(name, name);
        return this;
    }

    /**
     * Completes given workflow path and returns the builder
     * 
     * @return the builder
     */
    public WorkflowBuilder done() {
        return workflowBuilder;
    }

    @Override
    protected Node getNode() {
        return this.node;
    }
}
