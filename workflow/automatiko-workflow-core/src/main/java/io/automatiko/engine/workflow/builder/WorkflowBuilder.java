package io.automatiko.engine.workflow.builder;

import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

/**
 * Entry point for building workflows as code. Start with <code>newWorkflow(...)</code> static method and continue with adding
 * nodes.
 */
public class WorkflowBuilder {

    public class Connectors {
        public static final String KAFKA = "kafka";
        public static final String MQTT = "mqtt";
        public static final String AMQP = "amqp";
        public static final String CAMEL = "camel";
        public static final String JMS = "jms";
        public static final String HTTP = "http";
        public static final String PULSAR = "pulsar";
        public static final String RABBITMQ = "rabbitmq";
        public static final String DIRECT = "direct";
    }

    protected Map<String, List<String>> diagram = new LinkedHashMap<>();

    protected final ExecutableProcess process;

    protected final AtomicLong ids = new AtomicLong(0);

    protected NodeContainer container;

    protected Node currentNode;

    protected AdditionalPathNodeBuilder currentAdditionalPath;

    protected AbstractNodeBuilder currentBuilder;

    protected Map<String, JoinNodeBuilder> joins = new HashMap<>();

    private WorkflowBuilder(ExecutableProcess process) {
        this.process = process;
        this.process.setMetaData("DiagramInfo", diagram);
        this.container = this.process;
    }

    /**
     * Creates new workflow definition with given id and name without version
     * 
     * @param id unique identifier of the workflow
     * @param name descriptive (but short) name of the workflow
     * @return the builder
     */
    public static WorkflowBuilder newWorkflow(String id, String name) {
        return newWorkflow(id, name, null, null);
    }

    /**
     * Creates new workflow definition with given id and name and version
     * 
     * @param id unique identifier of the workflow
     * @param name descriptive (but short) name of the workflow
     * @param version version of the workflow, expected to be number based
     * @return the builder
     */
    public static WorkflowBuilder newWorkflow(String id, String name, String version) {
        return newWorkflow(id, name, version, null);
    }

    /**
     * Creates new workflow definition with given id and name, version and description
     * 
     * @param id unique identifier of the workflow
     * @param name descriptive (but short) name of the workflow
     * @param version version of the workflow, expected to be number based
     * @param description human targeted description of the workflow definition, can also include references to variables
     *        <code>#{dataObjectName}</code>
     * @return the builder
     */
    public static WorkflowBuilder newWorkflow(String id, String name, String version, String description) {
        ExecutableProcess process = new ExecutableProcess();

        process.setId(id);
        process.setName(name);
        process.setVersion(version);
        process.setVisibility(ExecutableProcess.PUBLIC_VISIBILITY);
        process.setAutoComplete(true);
        process.setType(ExecutableProcess.WORKFLOW_TYPE);
        process.setPackageName("org.acme.test");

        if (description != null) {
            process.setMetaData("Documentation", description);
        }

        return new WorkflowBuilder(process);
    }

    /**
     * Creates new private (used as sub workflow)workflow definition with given id and name without version
     * 
     * @param id unique identifier of the workflow
     * @param name descriptive (but short) name of the workflow
     * @return the builder
     */
    public static WorkflowBuilder newPrivateWorkflow(String id, String name) {
        return newPrivateWorkflow(id, name, null, null);
    }

    /**
     * Creates new private (used as sub workflow)workflow definition with given id and name and version
     * 
     * @param id unique identifier of the workflow
     * @param name descriptive (but short) name of the workflow
     * @param version version of the workflow, expected to be number based
     * @return the builder
     */
    public static WorkflowBuilder newPrivateWorkflow(String id, String name, String version) {
        return newPrivateWorkflow(id, name, version, null);
    }

    /**
     * Creates new private (used as sub workflow) workflow definition with given id and name, version and description
     * 
     * @param id unique identifier of the workflow
     * @param name descriptive (but short) name of the workflow
     * @param version version of the workflow, expected to be number based
     * @param description human targeted description of the workflow definition, can also include references to variables
     *        <code>#{dataObjectName}</code>
     * @return the builder
     */
    public static WorkflowBuilder newPrivateWorkflow(String id, String name, String version, String description) {
        ExecutableProcess process = new ExecutableProcess();

        process.setId(id);
        process.setName(name);
        process.setVersion(version);
        process.setVisibility(ExecutableProcess.PRIVATE_VISIBILITY);
        process.setAutoComplete(true);
        process.setType(ExecutableProcess.WORKFLOW_TYPE);
        process.setPackageName("org.acme.test");

        if (description != null) {
            process.setMetaData("Documentation", description);
        }

        return new WorkflowBuilder(process);
    }

    /**
     * Returns the built workflow definition.
     * 
     * @return workflow definition built so far
     */
    public ExecutableProcess get() {
        return process;
    }

    /**
     * Sets custom attribute for this workflow definition
     * 
     * @param name name of the attribute, must not be null
     * @param value value of the attribute, must not be null
     * @return the builder
     */
    public WorkflowBuilder customAttribute(String name, Object value) {
        if (name != null && value != null) {
            process.setMetaData(name, value);
        }
        return this;
    }

    /**
     * Adds tags to the workflow definition
     * 
     * @param tags non null tags to be assigned to the workflow definition
     * @return the builder
     */
    public WorkflowBuilder tags(String... tags) {
        if (tags != null && tags.length > 0) {
            String existingTags = (String) process.getMetaData("tags");
            process.setMetaData("tags",
                    (existingTags == null ? "" : existingTags + ",") + Stream.of(tags).collect(Collectors.joining(",")));
        }
        return this;
    }

    /**
     * Adds tag to the workflow definition as expression
     * 
     * @param tags non null tags to be assigned to the workflow definition
     * @return the builder
     */
    public WorkflowBuilder tags(Supplier<String> tagExpression) {
        if (tagExpression != null) {
            String expression = "#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()) + "}";
            String existingTags = (String) process.getMetaData("tags");
            process.setMetaData("tags",
                    (existingTags == null ? "" : existingTags + ",") + expression);
        }
        return this;
    }

    public WorkflowBuilder instanceName(String instanceName) {
        if (instanceName != null && !instanceName.isBlank()) {
            process.setMetaData("customDescription", instanceName);
        }

        return this;
    }

    public WorkflowBuilder instanceName(Supplier<String> instanceNameExpression) {
        if (instanceNameExpression != null) {
            String expression = "#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()) + "}";
            process.setMetaData("customDescription", expression);
        }

        return this;
    }

    /**
     * Sets expiration of the workflow instance that instructs to remove the instance from storage after
     * <code>expiresAfter</code>
     * after completion
     * 
     * @param expiresAfter not null duration
     * @return the builder
     */
    public WorkflowBuilder expiresAfter(Duration expiresAfter) {
        Objects.requireNonNull(expiresAfter, "expiresAfter must not be null");
        process.setMetaData("expiresAfter", expiresAfter.toString());
        return this;
    }

    /**
     * Sets expiration of the workflow instance that instructs to remove the instance from storage after
     * <code>expiresAfter</code>
     * after completion
     * 
     * @param expiresAfter not null period
     * @return the builder
     */
    public WorkflowBuilder expiresAfter(Period expiresAfter) {
        Objects.requireNonNull(expiresAfter, "expiresAfter must not be null");
        process.setMetaData("expiresAfter", expiresAfter.toString());
        return this;
    }

    /**
     * Sets category of the workflow that is used to group workflows at documentation level
     * 
     * @param category not null name of the category
     * @return the builder
     */
    public WorkflowBuilder category(String category, String description) {
        Objects.requireNonNull(category, "Category must not be null");
        process.setMetaData("category", category);
        process.setMetaData("categoryDescription", description);
        return this;
    }

    /**
     * Sets security roles to be applied for the service api created for given workflow
     * 
     * @param roles not null names of roles to be set
     * @return the builder
     */
    public WorkflowBuilder securityRoles(String... roles) {
        Objects.requireNonNull(roles, "Roles must not be null");
        process.setMetaData("securityRoles", Stream.of(roles).collect(Collectors.joining(",")));
        return this;
    }

    /**
     * Sets access policy to be used for this workflow to protect access. It references name of the
     * <code>io.automatiko.engine.api.auth.AccessPolicy</code>
     * 
     * @param policy not null name of the policy implementation
     * @return the builder
     */
    public WorkflowBuilder accessPolicy(String policy) {
        Objects.requireNonNull(policy, "policy name must not be null");
        process.setMetaData("accessPolicy", policy);
        return this;
    }

    /**
     * Aborts workflow instance inn case it is not completed within given duration expressed as ISO 8601 duration format.
     * For example <code>P5D</code> stands for 5 days, which will abort workflow instance after 5 days from the starting time
     * 
     * @param isoExpession duration to wait before it is aborted
     * @return the builder
     */
    public WorkflowBuilder abortAfter(String isoExpession) {
        process.setMetaData("timeout", isoExpession);
        return this;
    }

    /**
     * Applies authorization policy that makes the instance only visible to users that currently participate in it.
     * Participation is based on the fact that given user can be either initiator (user who started the instance) or
     * being assigned to active user task within the workflow instance.
     * 
     * @return the builder
     */
    public WorkflowBuilder withParticipantsAccessPolicy() {
        process.setMetaData("accessPolicy", "participants");
        return this;
    }

    /**
     * Applies authorization policy that makes the instance only visible to the initiator (user who started the instance)
     * essentially making the instance private to that user
     * 
     * @return the builder
     */
    public WorkflowBuilder withInitiatorsAccessPolicy() {
        process.setMetaData("accessPolicy", "initiator");
        return this;
    }

    /**
     * Sets end of instance strategy to keep for this workflow meaning will keep completed workflow instances
     * 
     * @return the builder
     */
    public WorkflowBuilder withKeepEndOfInstanceStrategy() {
        process.setMetaData("endOfInstanceStrategy", "keep");

        return this;
    }

    /**
     * Sets end of instance strategy to archive for this workflow meaning will archive automatically completed workflow
     * instances
     * 
     * @return the builder
     */
    public WorkflowBuilder withArchiveEndOfInstanceStrategy() {
        process.setMetaData("endOfInstanceStrategy", "archive");

        return this;
    }

    /**
     * Adds data object to the workflow definition and returns it so it can be used in expressions
     * 
     * @param type type of the data object
     * @param name name of the data object that can be later on referenced by
     * @param tags optional (non null tags) to be assigned to the data object e.g. output, input, required, etc
     * @return the data object that can be target by expression in workflow nodes
     */
    public <T> T dataObject(Class<T> type, String name, String... tags) {
        dataObject(name, type, tags);
        return null;
    }

    /**
     * Adds list data object to the workflow definition and returns it so it can be used in expressions
     * 
     * @param <T> data type of the list elements
     * @param type type of the list elements
     * @param name name of the data object
     * @param tags optional tags
     * @return return null as it only records the definition
     */
    public <T> List<T> listDataObject(Class<T> type, String name, String... tags) {
        listDataObject(name, type, tags);
        return null;
    }

    /**
     * Adds list data object to the workflow definition and returns it so it can be used in expressions
     * 
     * @param name name of the data object
     * @param type type of the list elements
     * @param tags optional tags
     * @return the builder
     */
    public WorkflowBuilder listDataObject(String name, Class<?> type, String... tags) {
        Variable variable = new Variable();
        variable.setId(name);
        variable.setName(name);
        variable.setType(new ObjectDataType(List.class, List.class.getCanonicalName() + "<" + type.getCanonicalName() + ">"));
        variable.setMetaData("type", type.getCanonicalName());

        if (tags != null && tags.length > 0) {
            String variableTags = Stream.of(tags).collect(Collectors.joining(","));
            variable.setMetaData("tags", variableTags);

            if (variableTags.contains(Variable.BUSINESS_RELEVANT_TAG)) {
                this.tags("#{" + name + "}");
            }
        }

        process.getVariableScope().getVariables().add(variable);
        return this;
    }

    /**
     * Adds list data object to the workflow definition and returns it so it can be used in expressions
     * 
     * @param name name of the data object
     * @param valueExpression expression to initialize the data object with
     * @param type type of the list elements
     * @param tags optional tags
     * @return the builder
     */
    public <T> WorkflowBuilder listDataObject(String name, Supplier<T> valueExpression, Class<T> type, String... tags) {
        Variable variable = new Variable();
        variable.setId(name);
        variable.setName(name);
        variable.setType(new ObjectDataType(List.class, List.class.getCanonicalName() + "<" + type.getCanonicalName() + ">"));
        variable.setMetaData("type", type.getCanonicalName());

        if (tags != null && tags.length > 0) {
            String variableTags = Stream.of(tags).collect(Collectors.joining(","));
            variable.setMetaData("tags", variableTags);

            if (variableTags.contains(Variable.BUSINESS_RELEVANT_TAG)) {
                this.tags("#{" + name + "}");
            }
        }
        if (valueExpression != null) {
            variable.setMetaData(Variable.DEFAULT_VALUE,
                    "#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()) + "}");
        }
        process.getVariableScope().getVariables().add(variable);
        return this;
    }

    /**
     * Adds list data object to the workflow definition and returns it so it can be used in expressions
     * 
     * @param name name of the data object
     * @param valueExpression expression to initialize the data object with
     * @param type type of the list elements
     * @param tags optional tags
     * @return the builder
     */
    public <T> WorkflowBuilder listDataObject(String name, String valueExpression, Class<T> type, String... tags) {
        Variable variable = new Variable();
        variable.setId(name);
        variable.setName(name);
        variable.setType(new ObjectDataType(List.class, List.class.getCanonicalName() + "<" + type.getCanonicalName() + ">"));
        variable.setMetaData("type", type.getCanonicalName());

        if (tags != null && tags.length > 0) {
            String variableTags = Stream.of(tags).collect(Collectors.joining(","));
            variable.setMetaData("tags", variableTags);

            if (variableTags.contains(Variable.BUSINESS_RELEVANT_TAG)) {
                this.tags("#{" + name + "}");
            }
        }
        if (valueExpression != null) {
            variable.setMetaData("value",
                    "#{" + valueExpression + "}");
        }
        process.getVariableScope().getVariables().add(variable);
        return this;
    }

    public <T> List<T> listDataObject(Class<T> type, String name, Supplier<T> valueExpression, String... tags) {
        Variable variable = new Variable();
        variable.setId(name);
        variable.setName(name);
        variable.setType(new ObjectDataType(List.class, List.class.getCanonicalName() + "<" + type.getCanonicalName() + ">"));
        variable.setMetaData("type", type.getCanonicalName());

        if (tags != null && tags.length > 0) {
            String variableTags = Stream.of(tags).collect(Collectors.joining(","));
            variable.setMetaData("tags", variableTags);

            if (variableTags.contains(Variable.BUSINESS_RELEVANT_TAG)) {
                this.tags("#{" + name + "}");
            }
        }
        if (valueExpression != null) {
            variable.setMetaData(Variable.DEFAULT_VALUE,
                    "#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()) + "}");
        }
        process.getVariableScope().getVariables().add(variable);
        return null;
    }

    /**
     * Adds parameterized data object to the workflow definition and returns it so it can be used in expressions
     * 
     * @param <T> data type
     * @param type type of the data objects
     * @param name name of the data object
     * @param tags optional tags
     * @return return null as it only records the definition
     */
    public <T> T dataObject(DataObjectType<?> type, String name, String... tags) {
        dataObject(name, type, tags);
        return null;
    }

    /**
     * Adds parameterized data object to the workflow definition and returns it so it can be used in expressions
     * 
     * @param name name of the data object
     * @param type type of the data objects
     * @param tags optional tags
     * @return the builder
     */
    public WorkflowBuilder dataObject(String name, DataObjectType<?> type, String... tags) {
        Variable variable = new Variable();
        variable.setId(name);
        variable.setName(name);
        variable.setType(
                new ObjectDataType((Class<?>) type.getSuperRawType(),
                        type.getSuperType().getTypeName()));
        variable.setMetaData("type", type.getType().getTypeName());

        if (tags != null && tags.length > 0) {
            String variableTags = Stream.of(tags).collect(Collectors.joining(","));
            variable.setMetaData("tags", variableTags);

            if (variableTags.contains(Variable.BUSINESS_RELEVANT_TAG)) {
                this.tags("#{" + name + "}");
            }
        }

        process.getVariableScope().getVariables().add(variable);
        return this;
    }

    /**
     * Adds data object to the workflow definition
     * 
     * @param name name of the data object that can be later on referenced by
     * @param type type of the data object
     * @param tags optional (non null tags) to be assigned to the data object e.g. output, input, required, etc
     * @return the builder
     */
    public WorkflowBuilder dataObject(String name, Class<?> type, String... tags) {
        Variable variable = new Variable();
        variable.setId(name);
        variable.setName(name);
        variable.setType(new ObjectDataType(type));

        if (tags != null && tags.length > 0) {
            String variableTags = Stream.of(tags).collect(Collectors.joining(","));
            variable.setMetaData("tags", variableTags);

            if (variableTags.contains(Variable.BUSINESS_RELEVANT_TAG)) {
                this.tags("#{" + name + "}");
            }
        }

        process.getVariableScope().getVariables().add(variable);
        return this;
    }

    /**
     * Adds data object to the workflow definition
     * 
     * @param name name of the data object that can be later on referenced by
     * @param valueExpression expression to initialize the data object with
     * @param type type of the data object
     * @param tags optional (non null tags) to be assigned to the data object e.g. output, input, required, etc
     * @return the builder
     */
    public WorkflowBuilder dataObject(String name, String valueExpression, Class<?> type, String... tags) {
        Variable variable = new Variable();
        variable.setId(name);
        variable.setName(name);
        variable.setType(new ObjectDataType(type));

        if (tags != null && tags.length > 0) {
            String variableTags = Stream.of(tags).collect(Collectors.joining(","));
            variable.setMetaData("tags", variableTags);

            if (variableTags.contains(Variable.BUSINESS_RELEVANT_TAG)) {
                this.tags("#{" + name + "}");
            }
        }

        if (valueExpression != null) {
            variable.setMetaData(Variable.DEFAULT_VALUE, "#{" + valueExpression + "}");
        }

        process.getVariableScope().getVariables().add(variable);
        return this;
    }

    /**
     * Adds data object to the workflow definition
     * 
     * @param name name of the data object that can be later on referenced by
     * @param valueExpression expression to initialize the data object with
     * @param type type of the data object
     * @param tags optional (non null tags) to be assigned to the data object e.g. output, input, required, etc
     * @return the builder
     */
    public <T> WorkflowBuilder dataObject(String name, Supplier<T> valueExpression, Class<T> type, String... tags) {
        Variable variable = new Variable();
        variable.setId(name);
        variable.setName(name);
        variable.setType(new ObjectDataType(type));

        if (tags != null && tags.length > 0) {
            String variableTags = Stream.of(tags).collect(Collectors.joining(","));
            variable.setMetaData("tags", variableTags);

            if (variableTags.contains(Variable.BUSINESS_RELEVANT_TAG)) {
                this.tags("#{" + name + "}");
            }
        }

        if (valueExpression != null) {

            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
            String method = stackTraceElement.getMethodName();
            String expression = BuilderContext.get(method);
            variable.setMetaData(Variable.DEFAULT_VALUE,
                    "#{" + expression + "}");

        }

        process.getVariableScope().getVariables().add(variable);
        return this;
    }

    /**
     * Adds parameterized data object to the workflow definition and returns it so it can be used in expressions
     * 
     * @param <T> data type
     * @param type type of the data objects
     * @param name name of the data object
     * @param valueExpression expression to initialize the data object with
     * @param tags optional tags
     * @return return null as it only records the definition
     */
    public <T> T dataObject(Class<T> type, String name, Supplier<T> valueExpression, String... tags) {
        Variable variable = new Variable();
        variable.setId(name);
        variable.setName(name);
        variable.setType(new ObjectDataType(type));

        if (tags != null && tags.length > 0) {
            String variableTags = Stream.of(tags).collect(Collectors.joining(","));
            variable.setMetaData("tags", variableTags);

            if (variableTags.contains(Variable.BUSINESS_RELEVANT_TAG)) {
                this.tags("#{" + name + "}");
            }
        }

        if (valueExpression != null) {

            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
            String method = stackTraceElement.getMethodName();
            String expression = BuilderContext.get(method);
            variable.setMetaData(Variable.DEFAULT_VALUE,
                    "#{" + expression + "}");

        }

        process.getVariableScope().getVariables().add(variable);
        return null;
    }

    /**
     * Adds a starting node
     * 
     * @param name name of the node
     * @return the builder
     */
    public StartNodeBuilder start(String name) {
        return new StartNodeBuilder(name, this);
    }

    /**
     * Adds a starting node that is triggered based on incoming message
     * 
     * NOTE: Requires one of the messaging addons (kafka, mqtt, camel, http, jms, amqp)
     * 
     * @param name name of the node which is also the default message name
     * @return the builder
     */
    public StartOnMessageNodeBuilder startOnMessage(String name) {
        return new StartOnMessageNodeBuilder(name, this);
    }

    /**
     * Adds a starting node that is triggered based on time expression
     * 
     * @param name name of the node
     * @return the builder
     */
    public StartOnTimerNodeBuilder startOnTimer(String name) {
        return new StartOnTimerNodeBuilder(name, this);
    }

    /**
     * Adds an ending node, that will only end given path (or complete instance if no other paths exists)
     * 
     * @param name name of the node
     * @return the builder
     */
    public EndNodeBuilder end(String name) {
        return new EndNodeBuilder(name, false, this);
    }

    /**
     * Adds terminate node, that will complete instance
     * 
     * @param name name of the node
     * @return the builder
     */
    public EndNodeBuilder terminate(String name) {
        return new EndNodeBuilder(name, true, this);
    }

    /**
     * Adds an ending node, that will send message and only end given path (or complete instance if no other paths exists)
     * 
     * @param name name of the node
     * @return the builder
     */
    public EndWithMessageNodeBuilder endWithMessage(String name) {
        return new EndWithMessageNodeBuilder(name, false, this);
    }

    /**
     * Adds an ending node with error
     * 
     * @param name name of the node
     * @return the builder
     */
    public EndWithErrorNodeBuilder endWithError(String name) {
        return new EndWithErrorNodeBuilder(name, false, this);
    }

    /**
     * Adds an expression node (aka script) that will allow to invoke expressions
     * 
     * @param name name of the node
     * @return the builder
     */
    public ExpressionNodeBuilder expression(String name, String expression) {
        return new ExpressionNodeBuilder(name, this).expression(expression);
    }

    /**
     * Adds an expression node (aka script) that will allow to invoke expressions
     * 
     * @param name name of the node
     * @return the builder
     */
    public ExpressionNodeBuilder expression(String name, Expression expression) {
        return new ExpressionNodeBuilder(name, this)
                .expression(BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()));
    }

    /**
     * Adds an expression node (aka script) that will set data object given by name to the value returned by the expression
     * 
     * @param name name of the node
     * @param dataObject name of the data object to set
     * @param expression expression that produces value to be assigned to the data object
     * @return the builder
     */
    public ExpressionNodeBuilder set(String name, String dataObject, Supplier<Object> expression) {
        return new ExpressionNodeBuilder(name, this)
                .set(dataObject, BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()));
    }

    /**
     * A short cut method to add expression node that is going to log given text and values
     * The text is expected to use <code>{}</code> as place holder for each value
     * 
     * @param name name of the node
     * @param text text to be logged
     * @param values optional values that should match <code>{}</code> included in the text
     * @return the builder
     */
    public ExpressionNodeBuilder log(String name, String text, String... values) {
        return new ExpressionNodeBuilder(name, this).log(text, values);
    }

    /**
     * A short cut method to add expression node that is going to print out (to sysout) given text
     * In case data objects should be referenced in the text they need to be properly escaped
     * <code>printout("say hi", "\"Hello\" + name)"</code>
     * 
     * @param name name of the node
     * @param text text to be logged
     * @return the builder
     */
    public ExpressionNodeBuilder printout(String name, String text) {
        return new ExpressionNodeBuilder(name, this).println(text);
    }

    /**
     * Adds a service node to invoke a method of the given class
     * 
     * @param name name of the node
     * @param clazz class of the service
     * @param method name of the method to invoke
     * 
     * @return the builder
     */
    public ServiceNodeBuilder service(String name, Class<?> clazz, String method) {
        return new ServiceNodeBuilder(name, this).type(clazz.getCanonicalName(), method);
    }

    /**
     * Adds a service node to invoke a method of a class that are provided in type safe manner via returned
     * <code>ServiceNodeBuilder</code>
     * 
     * @param name name of the node
     * 
     * @return the builder
     */
    public ServiceNodeBuilder service(String name) {
        return new ServiceNodeBuilder(name, this);
    }

    /**
     * Adds a service node to invoke a rest service
     * 
     * @param name name of the node
     * 
     * @return the builder
     */
    public RestServiceNodeBuilder restService(String name) {
        return new RestServiceNodeBuilder(name, this);
    }

    /**
     * Adds a timer node
     * 
     * @param name name of the node
     * @return the builder
     */
    public TimerNodeBuilder timer(String name) {
        return new TimerNodeBuilder(name, this);
    }

    /**
     * Adds a user node
     * 
     * @param name name of the node
     * @return the builder
     */
    public UserNodeBuilder user(String name) {
        return new UserNodeBuilder(name, this);
    }

    /**
     * Adds a wait on message node
     * 
     * @param name name of the node
     * @return the builder
     */
    public WaitOnMessageNodeBuilder waitOnMessage(String name) {
        return new WaitOnMessageNodeBuilder(name, this);
    }

    /**
     * Adds a send message node
     * 
     * @param name name of the node
     * @return the builder
     */
    public SendMessageNodeBuilder sendMessage(String name) {
        return new SendMessageNodeBuilder(name, this);
    }

    /**
     * Adds a sub workflow node
     * 
     * @param name name of the node
     * @return the builder
     */
    public SubWorkflowNodeBuilder subWorkflow(String name) {
        return new SubWorkflowNodeBuilder(name, this);
    }

    /**
     * Adds an additional path node that allows to have separate workflow path
     * that starts based on timer event
     * 
     * @param name name of the node
     * @return the builder
     */
    public StartOnTimerNodeBuilder additionalPathOnTimer(String name) {
        this.currentAdditionalPath = new AdditionalPathNodeBuilder(name, this);

        return startOnTimer("on timeout");
    }

    /**
     * Adds an additional path node that allows to have separate workflow path
     * that starts based on timer event
     * 
     * @param name name of the node
     * @param abortAfter indicates if the main workflow instance should be aborted once the additional path completes
     * @return the builder
     */
    public StartOnTimerNodeBuilder additionalPathOnTimer(String name, boolean abortAfter) {
        this.currentAdditionalPath = new AdditionalPathNodeBuilder(name, this);

        return new StartOnTimerNodeBuilder(name, this, abortAfter);
    }

    /**
     * Adds an additional path node that allows to have separate workflow path
     * that starts based on incoming message
     * 
     * @param name name of the node
     * @return the builder
     */
    public StartOnMessageNodeBuilder additionalPathOnMessage(String name) {
        this.currentAdditionalPath = new AdditionalPathNodeBuilder(name, this).event("Message-" + name);

        return startOnMessage(name);
    }

    /**
     * Adds an additional path node that allows to have separate workflow path
     * that starts based on incoming message
     * 
     * @param name name of the node
     * @param abortAfter indicates if the main workflow instance should be aborted once the additional path completes
     * @return the builder
     */
    public StartOnMessageNodeBuilder additionalPathOnMessage(String name, boolean abortAfter) {
        this.currentAdditionalPath = new AdditionalPathNodeBuilder(name, this).event("Message-" + name);

        return new StartOnMessageNodeBuilder(name, this, abortAfter);
    }

    /**
     * Adds an additional path node that allows to have separate workflow path
     * that starts based on incoming signal
     * 
     * @param name name of the node
     * @return the builder
     */
    public StartOnSignalNodeBuilder additionalPathOnSignal(String name) {

        return additionalPathOnSignal(name, false);
    }

    /**
     * Adds an additional path node that allows to have separate workflow path
     * that starts based on incoming signal
     * 
     * @param name name of the node
     * @param abortAfter indicates if the main workflow instance should be aborted once the additional path completes
     * @return the builder
     */
    public StartOnSignalNodeBuilder additionalPathOnSignal(String name, boolean abortAfter) {
        this.currentAdditionalPath = new AdditionalPathNodeBuilder(name, this).event("Message-" + name);

        return new StartOnSignalNodeBuilder(name, this, abortAfter);
    }

    /**
     * Ends current additional path if any is active otherwise throws {@link IllegalStateException}
     * 
     * @return the builder
     */
    public WorkflowBuilder endAdditionalPath() {
        if (this.currentAdditionalPath == null) {
            throw new IllegalStateException("No additional path active");
        }
        this.currentAdditionalPath.end();
        return this;
    }

    /*
     * Helper methods that are considered internal
     */

    protected void putOnContext(Node node) {
        this.currentNode = node;
    }

    protected Node fetchFromContext() {
        return this.currentNode;
    }

    protected void putBuilderOnContext(AbstractNodeBuilder currentBuilder) {
        this.currentBuilder = currentBuilder;
    }

    protected void appendDiagramItem(String source, String target) {
        this.diagram.compute(source, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            if (v.contains(target)) {
                v.remove(target);
            }
            v.add(target);

            return v;
        });
    }

    protected AbstractNodeBuilder builderFromContext() {
        return this.currentBuilder;
    }

    protected Class<?> constructClass(String name) {
        return constructClass(name, Thread.currentThread().getContextClassLoader());
    }

    protected Class<?> constructClass(String name, ClassLoader cl) {
        if (name == null || name.contains(":")) {
            return Object.class;
        }

        switch (name) {
            case "Object":
                return Object.class;
            case "Integer":
                return Integer.class;
            case "Double":
                return Double.class;
            case "Float":
                return Float.class;
            case "Boolean":
                return Boolean.class;
            case "String":
                return String.class;
            case "Date":
                return Date.class;
            default:
                break;
        }

        try {
            return Class.forName(name, true, cl);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    protected NodeContainer container() {
        return container;
    }

    protected void container(NodeContainer container) {
        this.container = container;
    }
}
