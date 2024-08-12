package io.automatiko.engine.workflow.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.automatiko.engine.workflow.base.core.Work;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.base.core.impl.ParameterDefinitionImpl;
import io.automatiko.engine.workflow.base.core.impl.WorkImpl;
import io.automatiko.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.ForEachNode;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;

/**
 * Builder responsible for building an service node
 */
public class RestServiceNodeBuilder extends AbstractNodeBuilder {

    private static final String SERVICE_TASK_TYPE = "Service Task";

    private WorkItemNode node;

    private ForEachNode forEachNode;

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

        workflowBuilder.container().addNode(node);

        connect();
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
        return new ErrorNodeBuilder("error on " + node.getName(), (String) this.node.getMetaData("UniqueId"),
                workflowBuilder)
                        .errorCodes(errorCodes);
    }

    /**
     * Adds error handling node that allows to move to different path in case of an error
     * 
     * @param errorCodes list of codes to listen on
     * @return the builder
     */
    @SafeVarargs
    public final ErrorNodeBuilder onError(Class<? extends Throwable>... exceptions) {
        workflowBuilder.putOnContext(getNode());
        workflowBuilder.putBuilderOnContext(null);
        return new ErrorNodeBuilder("error on " + node.getName(), (String) this.node.getMetaData("UniqueId"),
                workflowBuilder)
                        .errorCodes(Stream.of(exceptions).map(c -> c.getName()).toArray(String[]::new));
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

    /**
     * Instructs to repeat this node based on the input collection. This will create new node for each element in the
     * collection.
     * <br/>
     * The item will be named <code>item</code> and as such can be easily accessed by service node data mapping<br/>
     * <code>
     * ServiceNodeBuilder service = builder.restService("get pet");<br/>
     * <br/>
     * service.repeat(() -> java.util.List.of("John", "Mary", "Mike")).openApi("api/swagger.json").operation("getPetById").fromDataObject("item");<br/>
     * </code>
     * <br/>
     * Above example will invoke operation <code>getPetById</code> of <code>PetStore</code> service three times
     * as the input collection returns list that consists of three names. Each name is then mapped to the service node
     * via the <code>item</code> data object.
     * 
     * @param inputCollectionExpression expression that will deliver collection to repeat service on each item
     * @return the builder
     */
    public RestServiceNodeBuilder repeat(Supplier<Collection<?>> inputCollectionExpression) {
        return repeat("#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                + "}", "item", null, null);
    }

    /**
     * Instructs to repeat this node based on the input collection and collect all results into the output collection.
     * This will create new node for each element in the input collection.
     * <br/>
     * The input element will be named <code>item</code> and as such can be easily accessed by service node data mapping<br/>
     * The output element will be named <code>outItem</code> and as such can be easily accessed by service node data output
     * mapping<br/>
     * <code>
     * RestServiceNodeBuilder service = builder.restService("restService");<br/>
     * List<?> myOutputs = builder.dataObject(List.class, "myOutputs");<br/>
     * <br/>
     * service.toDataObject("outItem", service.openApi("api/swagger.json").operation("getPetById")).fromDataObject("item")
     *           .repeat(() -> java.util.List.of("John", "Mary", "Mike"), () -> myOutputs).then()
     *           .end("that's it");
     * </code>
     * <br/>
     * Above example will invoke operation <code>getPetById</code> of <code>PetStore</code> service three times
     * as the input collection returns list that consists of three names. Each name is then mapped to the service node
     * via the <code>item</code> data object.
     * 
     * @param inputCollectionExpression expression that will deliver collection to repeat service on each item
     * @param outputCollectionExpression expression that will deliver collection that will be populated with results of calling
     *        the service
     * @return the builder
     */
    public RestServiceNodeBuilder repeat(Supplier<Collection<?>> inputCollectionExpression,
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
     * The item will be named as given with <code>inputName</code> and as such can be easily accessed by service node data
     * mapping<br/>
     * <code>
     * RestServiceNodeBuilder service = builder.restService("restService");<br/>
     * <br/>
     * service.repeat(() -> java.util.List.of("John", "Mary", "Mike"), "name").openApi("api/swagger.json").operation("getPetById").fromDataObject("name");<br/>
     * </code>
     * <br/>
     * Above example will invoke operation <code>getPetById</code> of <code>PetStore</code> service three times
     * as the input collection returns list that consists of three names. Each name is then mapped to the service node
     * via the <code>item</code> data object.
     * 
     * @param inputCollectionExpression expression that will deliver collection to repeat service on each item
     * @return the builder
     */
    public RestServiceNodeBuilder repeat(Supplier<Collection<?>> inputCollectionExpression, String inputName) {
        return repeat("#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                + "}", inputName, null, null);
    }

    /**
     * Instructs to repeat this node based on the input collection and collect all results into the output collection.
     * This will create new node for each element in the input collection.
     * <br/>
     * The input element will be named based on given <code>inputName</code> and as such can be easily accessed by service node
     * data mapping<br/>
     * The output element will be named based on given <code>outputName</code> and as such can be easily accessed by service
     * node data output
     * mapping<br/>
     * <code>
     * RestServiceNodeBuilder service = builder.restService("restService");<br/>
     * List<?> myOutputs = builder.dataObject(List.class, "myOutputs");<br/>
     * <br/>
     * service.toDataObject("greeting", service.openApi("api/swagger.json").operation("getPetById")).fromDataObject("name")
     *           .repeat(() -> java.util.List.of("John", "Mary", "Mike"), "name", () -> myOutputs, "greeting").then()
     *           .end("that's it");
     * </code>
     * <br/>
     * Above example will invoke operation <code>getPetById</code> of <code>PetStore</code> service three times
     * as the input collection returns list that consists of three names. Each name is then mapped to the service node
     * via the <code>item</code> data object.
     * 
     * @param inputCollectionExpression expression that will deliver collection to repeat service on each item
     * @param outputCollectionExpression expression that will deliver collection that will be populated with results of calling
     *        the service
     * @return the builder
     */
    public RestServiceNodeBuilder repeat(Supplier<Collection<?>> inputCollectionExpression, String inputName,
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
     * The item will be named <code>item</code> and as such can be easily accessed by service node data mapping<br/>
     * <code>
     * RestServiceNodeBuilder service = builder.restService("restService");<br/>
     * <br/>
     * service.repeat("myCollection").openApi("api/swagger.json").operation("getPetById").fromDataObject("item");<br/>
     * </code>
     * <br/>
     * Above example will invoke operation <code>getPetById</code> of <code>PetStore</code> service as many times as the
     * input collection elements.
     * Each element is then mapped to the service node via the <code>item</code> data object.
     * 
     * @param dataObjectName name of the data object (of type collection) that should be used as input collection
     * @return the builder
     */
    public RestServiceNodeBuilder repeat(String dataObjectName) {
        return repeat(dataObjectName, "item", null, null);
    }

    /**
     * Instructs to repeat this node based on the data object that is given via <code>dataObjectName</code>. That data object
     * must
     * be of type collection. This will create new node for each element in the
     * collection.
     * <br/>
     * The item will be named as given by <code>inputName</code> and as such can be easily accessed by service node data
     * mapping<br/>
     * <code>
     * RestServiceNodeBuilder service = builder.restService("restService");<br/>
     * <br/>
     * service.repeat("myCollection", "name").openApi("api/swagger.json").operation("getPetById").fromDataObject("name");<br/>
     * </code>
     * <br/>
     * Above example will invoke operation <code>getPetById</code> of <code>PetStore</code> service as many times as the
     * input collection elements.
     * Each element is then mapped to the service node via the <code>inputName</code> data object.
     * 
     * @param dataObjectName name of the data object (of type collection) that should be used as input collection
     * @param inputName name of the item of the collection to be referenced by task
     * @return the builder
     */
    public RestServiceNodeBuilder repeat(String dataObjectName, String inputName) {
        return repeat(dataObjectName, inputName, null, null);
    }

    /**
     * Instructs to repeat this node based on the data object that is given via <code>dataObjectName</code>. That data object
     * must be of type collection. This will create new node for each element in the collection. At the end each result will be
     * collected and added to the
     * output collection that is represented by data object named <code>toDataObject</code>
     * <br/>
     * The item will be named as given by <code>inputName</code> and as such can be easily accessed by service node data
     * mapping<br/>
     * <code>
     * RestServiceNodeBuilder service = builder.restService("restService");<br/>
     * <br/>
     * service.toDataObject("outItem",
     *          service.openApi("api/swagger.json").operation("getPetById").fromDataObject("name").repeat("myCollection", "name", "myOutputs")).then()
     *          .end("that's it");
     * </code>
     * <br/>
     * Above example will invoke operation <code>getPetById</code> of <code>PetStore</code> service as many times as the
     * input collection elements.
     * Each element is then mapped to the service node via the <code>inputName</code> data object. Each result that is mapped
     * via <code>outItem</code>
     * is added to <code>myOutputs</code> data object
     * 
     * @param dataObjectName name of the data object (of type collection) that should be used as input collection
     * @param inputName name of the item of the collection to be referenced by task
     * @param toDataObject name of the data object that the results should be added to
     * @return the builder the builder
     */
    public RestServiceNodeBuilder repeat(String dataObjectName, String inputName, String toDataObject) {
        return repeat(dataObjectName, inputName, toDataObject, "outItem");
    }

    /**
     * Instructs to repeat this node based on the data object that is given via <code>dataObjectName</code>. That data object
     * must be of type collection. This will create new node for each element in the collection. At the end each result will be
     * collected and added to the
     * output collection that is represented by data object named <code>toDataObject</code>
     * <br/>
     * The item will be named as given by <code>inputName</code> and as such can be easily accessed by service node data
     * mapping<br/>
     * <code>
     * RestServiceNodeBuilder service = builder.restService("restService");<br/>
     * <br/>
     * service.toDataObject("greeting",
     *          service.openApi("api/swagger.json").operation("getPetById").fromDataObject("name").repeat("myCollection", "name", "myOutputs", "greeting")).then()
     *          .end("that's it");
     * </code>
     * <br/>
     * Above example will invoke operation <code>getPetById</code> of <code>PetStore</code> service as many times as the
     * input collection elements.
     * Each element is then mapped to the service node via the <code>inputName</code> data object. Each result that is mapped
     * via <code>outItem</code>
     * is added to <code>myOutputs</code> data object
     * 
     * @param dataObjectName name of the data object (of type collection) that should be used as input collection
     * @param inputName name of the item of the collection to be referenced by task
     * @param toDataObject name of the data object that the results should be added to
     * @param outputName name of the result of the execution that will be collected to the output collection
     * @return the builder the builder
     */
    public RestServiceNodeBuilder repeat(String dataObjectName, String inputName, String toDataObject, String outputName) {
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
        forEachNode.setVariable(inputName, new ObjectDataType());

        if (toDataObject != null) {
            forEachNode.setOutputVariable(outputName, new ObjectDataType());
            forEachNode.setOutputCollectionExpression(toDataObject);
        }
        CompositeContextNode subProcessNode = new CompositeContextNode();
        VariableScope variableScope = new VariableScope();
        subProcessNode.addContext(variableScope);
        subProcessNode.setDefaultContext(variableScope);
        subProcessNode.setAutoComplete(true);
        subProcessNode.setMetaData("hidden", true);
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

        forEachNode.linkIncomingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE, subProcessNode.getId(),
                NodeImpl.CONNECTION_DEFAULT_TYPE);
        forEachNode.linkOutgoingConnections(subProcessNode.getId(), NodeImpl.CONNECTION_DEFAULT_TYPE,
                NodeImpl.CONNECTION_DEFAULT_TYPE);

        workflowBuilder.container().addNode(forEachNode);

        connect();

        workflowBuilder.container(subProcessNode);

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
     * Sets custom attribute for this node
     * 
     * @param name name of the attribute, must not be null
     * @param value value of the attribute, must not be null
     * @return the builder
     */
    public RestServiceNodeBuilder customAttribute(String name, Object value) {
        return (RestServiceNodeBuilder) super.customAttribute(name, value);
    }

    public WorkflowBuilder endRepeatAndThen() {
        workflowBuilder.putOnContext(forEachNode);
        workflowBuilder.putBuilderOnContext(null);
        workflowBuilder.container(workflowBuilder.get());
        return workflowBuilder;
    }
}
