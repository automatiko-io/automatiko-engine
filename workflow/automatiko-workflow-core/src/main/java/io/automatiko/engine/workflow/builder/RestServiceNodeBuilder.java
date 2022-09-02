package io.automatiko.engine.workflow.builder;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.automatiko.engine.workflow.base.core.Work;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.base.core.impl.ParameterDefinitionImpl;
import io.automatiko.engine.workflow.base.core.impl.WorkImpl;
import io.automatiko.engine.workflow.base.core.timer.DateTimeUtils;
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
     * Creates data input for the service call based on given literal value
     * 
     * @param <T> type of data
     * @param inputName name of the data input to be created
     * @param value actual literal value
     * @return the builder
     */
    public <T> RestServiceNodeBuilder literalAsInput(String inputName, T value) {

        ObjectDataType type = new ObjectDataType(value.getClass());
        node.getWork().addParameterDefinition(new ParameterDefinitionImpl(inputName, type));
        if (value instanceof String) {
            node.getWork().setParameter(inputName, value.toString());
        } else {
            node.getWork().setParameter(inputName, "#{" + value.toString().replace("\"", "\\\"") + "}");
        }

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
    public <T> RestServiceNodeBuilder expressionAsInput(String inputName, Class<T> clazz, Supplier<T> expression) {

        ObjectDataType dataType = new ObjectDataType(clazz);
        node.getWork().addParameterDefinition(new ParameterDefinitionImpl(inputName, dataType));
        node.getWork().setParameter(inputName,
                "#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                        + "}");

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
    public RestServiceNodeBuilder toDataObjectField(String name, Object value, String... fields) {

        String dotExpression = name;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }

        DataAssociation out = new DataAssociation("Result", "#{" + dotExpression + "}", null, null);
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
     * @param name name of the data object
     * @param value value to be assigned
     * @param fields fields in data object to be accessed and last one to be set
     * @return the builder
     */
    public RestServiceNodeBuilder appendToDataObjectField(String name, Object value, String... fields) {

        String dotExpression = name;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }
        dotExpression += "[+]";

        DataAssociation out = new DataAssociation("Result", "#{" + dotExpression + "}", null, null);
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
     * @param name name of the data object
     * @param value value to be assigned
     * @param fields fields in data object to be accessed and last one to be set
     * @return the builder
     */
    public RestServiceNodeBuilder removeFromDataObjectField(String name, Object value, String... fields) {

        String dotExpression = name;

        if (fields != null && fields.length > 0) {
            dotExpression = dotExpression + "." + Stream.of(fields).collect(Collectors.joining("."));
        }
        dotExpression += "[-]";

        DataAssociation out = new DataAssociation("Result", "#{" + dotExpression + "}", null, null);
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

    /**
     * Configures the maximum amount of time the node will wait to get response from the service.
     * In case it is not successfully within that time it will abort the operation and throw error with <code>408</code> error
     * code.
     * 
     * This requires <code>automatiko-fault-tolerance-addon</code> to be added to project dependencies
     * 
     * @param isoExpression ISO 8601 duration
     * @return the builder
     */
    public RestServiceNodeBuilder timeout(String isoExpression) {
        node.setMetaData("timeout", DateTimeUtils.parseDuration(isoExpression));
        return this;
    }

    /**
     * Configures the maximum amount of time the node will wait to get response from the service.
     * In case it is not successfully within that time it will abort the operation and throw error with <code>408</code> error
     * code.
     * 
     * This requires <code>automatiko-fault-tolerance-addon</code> to be added to project dependencies
     * 
     * @param amount the amount of time to wait for service call completion
     * @param unit time unit the amount is expressed in
     * @return the builder
     */
    public RestServiceNodeBuilder timeout(long amount, TimeUnit unit) {
        node.setMetaData("timeout", TimeUnit.MILLISECONDS.convert(amount, unit));
        return this;
    }

    /**
     * Disables Circuit breaker to not configure it for given node. Only required when fault tolerance addon is present
     * but should not be configured for this service node
     * 
     * This requires <code>automatiko-fault-tolerance-addon</code> to be added to project dependencies
     * 
     * @return the builder
     */
    public RestServiceNodeBuilder disableCircuitBreaker() {
        node.setMetaData("faultToleranceDisabled", "true");

        return this;
    }

    /**
     * Configures Circuit breaker with default settings (requestThreshold=20, failureRatio=0.5, delay= 5 seconds)
     * 
     * This requires <code>automatiko-fault-tolerance-addon</code> to be added to project dependencies
     * 
     * @return the builder
     */
    public RestServiceNodeBuilder circuitBreaker() {
        return circuitBreaker(20);
    }

    /**
     * Configures Circuit breaker with given request threshold and rest based on default settings (failureRatio=0.5, delay= 5
     * seconds)
     * 
     * This requires <code>automatiko-fault-tolerance-addon</code> to be added to project dependencies
     * 
     * @param requestThreshold desired request threshold
     * 
     * @return the builder
     */
    public RestServiceNodeBuilder circuitBreaker(int requestThreshold) {
        return circuitBreaker(requestThreshold, 0.5);
    }

    /**
     * Configures Circuit breaker with given request threshold and failureRatio, delay is set with default 5 seconds
     * 
     * This requires <code>automatiko-fault-tolerance-addon</code> to be added to project dependencies
     * 
     * @param requestThreshold desired request threshold
     * @param failureRatio desired failing ratio
     * @return the builder
     */
    public RestServiceNodeBuilder circuitBreaker(int requestThreshold, double failureRatio) {
        return circuitBreaker(requestThreshold, failureRatio, 5, TimeUnit.SECONDS);
    }

    /**
     * Configures Circuit breaker with given request threshold, failureRatio and delay
     * 
     * This requires <code>automatiko-fault-tolerance-addon</code> to be added to project dependencies
     * 
     * @param requestThreshold desired request threshold
     * @param failureRatio desired failing ratio
     * @param delayExpression IOS 8601 expressed duration (e.g. PT10S)
     * @return the builder
     */
    public RestServiceNodeBuilder circuitBreaker(int requestThreshold, double failureRatio, String delayExpression) {
        node.setMetaData("requestThreshold", requestThreshold);
        node.setMetaData("failureRatio", failureRatio);
        node.setMetaData("delay", DateTimeUtils.parseDuration(delayExpression));

        return this;
    }

    /**
     * Configures Circuit breaker with given request threshold, failureRatio and delay
     * 
     * This requires <code>automatiko-fault-tolerance-addon</code> to be added to project dependencies
     * 
     * @param requestThreshold desired request threshold
     * @param failureRatio desired failing ratio
     * @param delay amount of time to delay
     * @param unit time unit that delay was expressed in
     * @return the builder
     */
    public RestServiceNodeBuilder circuitBreaker(int requestThreshold, double failureRatio, long delay, TimeUnit unit) {
        node.setMetaData("requestThreshold", requestThreshold);
        node.setMetaData("failureRatio", failureRatio);
        node.setMetaData("delay", TimeUnit.MILLISECONDS.convert(delay, unit));

        return this;
    }

    @Override
    protected Node getNode() {
        return this.node;
    }
}
