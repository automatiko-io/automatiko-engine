
package io.automatiko.engine.codegen.process;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatiko.engine.codegen.CodeGenConstants.AMQP_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.CAMEL_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.DIRECT_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.HTTP_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.INCOMING_PROP_PREFIX;
import static io.automatiko.engine.codegen.CodeGenConstants.JMS_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.KAFKA_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.MQTT_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.OPERATOR_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.PULSAR_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.RABBITMQ_CONNECTOR;
import static io.automatiko.engine.codegen.CodegenUtils.interpolateTypes;
import static io.automatiko.engine.codegen.CodegenUtils.isApplicationField;
import static io.automatiko.engine.codegen.CodegenUtils.isProcessField;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.Functions;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.BodyDeclarationComparator;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.codegen.ImportsOrganizer;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.services.execution.BaseFunctions;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.compiler.canonical.TriggerMetaData;

public class MessageConsumerGenerator {

    private final String relativePath;

    private GeneratorContext context;

    private WorkflowProcess process;
    private final String packageName;
    private final String resourceClazzName;
    private final String processClazzName;
    private String processId;
    private String version = "";
    private String dataClazzName;
    private String classPrefix;
    private String modelfqcn;
    private final String processName;
    private final String appCanonicalName;
    private final String messageDataEventClassName;
    private DependencyInjectionAnnotator annotator;

    private TriggerMetaData trigger;

    private boolean persistence;

    private String namespaces;

    public MessageConsumerGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn,
            String processfqcn, String appCanonicalName, String messageDataEventClassName, TriggerMetaData trigger) {
        this.context = context;
        this.process = process;
        this.trigger = trigger;
        this.packageName = process.getPackageName();
        this.processId = process.getId();
        this.processName = processId.substring(processId.lastIndexOf('.') + 1);
        if (process.getVersion() != null && !process.getVersion().trim().isEmpty()) {
            this.version = CodegenUtils.version(process.getVersion());
        }
        this.classPrefix = StringUtils.capitalize(processName) + CodegenUtils.version(process.getVersion());
        this.resourceClazzName = classPrefix + "MessageConsumer" + trigger.getOwnerId();
        this.relativePath = packageName.replace(".", "/") + "/" + resourceClazzName + ".java";
        this.modelfqcn = modelfqcn;
        this.dataClazzName = modelfqcn.substring(modelfqcn.lastIndexOf('.') + 1);
        this.processClazzName = processfqcn;
        this.appCanonicalName = appCanonicalName;
        this.messageDataEventClassName = messageDataEventClassName;

        this.namespaces = (String) trigger.getContext("namespaces",
                (String) process.getMetaData().getOrDefault("namespaces", "JOSDK_ALL_NAMESPACES"));
    }

    public MessageConsumerGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
        return this;
    }

    public MessageConsumerGenerator withPersistence(boolean persistence) {
        this.persistence = persistence;
        return this;
    }

    public String className() {
        return resourceClazzName;
    }

    public String generatedFilePath() {
        return relativePath;
    }

    protected boolean useInjection() {
        return this.annotator != null;
    }

    protected void appendConnectorSpecificProperties(String connector) {
        String sanitizedName = CodegenUtils.triggerSanitizedName(trigger, process.getVersion());
        if (connector.equals(MQTT_CONNECTOR)) {

            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".topic",
                    (String) trigger.getContext("topic", trigger.getName()));
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".host", "${mqtt.server:localhost}");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".port", "${mqtt.port:1883}");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".client-id",
                    classPrefix + "-consumer");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".auto-keep-alive", "true");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".keep-alive-seconds", "600");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".failure-strategy", "ignore");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".broadcast", "true");
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents", "false");

            context.addInstruction(
                    "Properties for MQTT based message event '" + trigger.getDescription() + "'");
            context.addInstruction(
                    "\t'" + INCOMING_PROP_PREFIX + sanitizedName
                            + ".topic' should be used to configure MQTT topic defaults to '"
                            + trigger.getContext("topic", trigger.getName()) + "'");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".host' should be used to configure MQTT host that defaults to localhost");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".port' should be used to configure MQTT port that defaults to 1883");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".client-id' should be used to configure MQTT client id that defaults to '" + classPrefix
                    + "-consumer'");

        } else if (connector.equals(CAMEL_CONNECTOR)) {

            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".endpoint-uri",
                    (String) trigger.getContext("url", ""));
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".failure-strategy", "ignore");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".broadcast", "true");
            context.addInstruction(
                    "Properties for Apache Camel based message event '" + trigger.getDescription() + "'");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".endpoint-uri' should be used to configure Apache Camel location");
        } else if (connector.equals(KAFKA_CONNECTOR)) {

            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".bootstrap.servers",
                    "${kafka.bootstrap.servers:localhost\\\\:9092}");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".topic",
                    (String) trigger.getContext("topic", trigger.getName()));
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".key.deserializer",
                    "org.apache.kafka.common.serialization.StringDeserializer");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".value.deserializer",
                    "org.apache.kafka.common.serialization.StringDeserializer");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".group.id",
                    classPrefix + "-consumer");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".broadcast", "true");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".failure-strategy", "ignore");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".cloud-events", "false");
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents",
                    isServerlessProcess() ? "true" : "false");
            context.addInstruction(
                    "Properties for Apache Kafka based message event '" + trigger.getDescription() + "'");
            context.addInstruction(
                    "\t'" + INCOMING_PROP_PREFIX + sanitizedName
                            + ".topic' should be used to configure Kafka topic defaults to '"
                            + trigger.getContext("topic", trigger.getName()) + "'");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".bootstrap.servers' should be used to configure Kafka bootstrap servers host that defaults to localhost:9092");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".key.deserializer' should be used to configure key deserializer port that defaults to StringDeserializer");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".value.deserializer' should be used to configure key deserializer port that defaults to StringDeserializer");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".group.id' should be used to configure Kafka group id that defaults to '" + classPrefix
                    + "-consumer'");
        } else if (connector.equals(JMS_CONNECTOR)) {

            context.setApplicationProperty("quarkus.index-dependency.sjms.group-id", "io.smallrye.reactive");
            context.setApplicationProperty("quarkus.index-dependency.sjms.artifact-id", "smallrye-reactive-messaging-jms");
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents",
                    isServerlessProcess() ? "true" : "false");

            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".destination", sanitizedName.toUpperCase());
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".broadcast", "true");
            if (trigger.getContext("selector") != null) {
                context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".selector",
                        trigger.getContext("selector").toString().replaceAll("=", "\\="));
            }
            context.addInstruction(
                    "Properties for JMS based message event '" + trigger.getDescription() + "'");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".destination' should be used to configure destination (queue or topic) name, defaults to '"
                    + context.getApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".destination")
                            .orElse(sanitizedName.toUpperCase())
                    + "'");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".selector' should be used to configure JMS selector to filter messages that will be consumed");
        } else if (connector.equals(AMQP_CONNECTOR)) {

            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".address", sanitizedName.toUpperCase());
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".failure-strategy", "release");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".cloud-events", "false");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".broadcast", "true");
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents",
                    isServerlessProcess() ? "true" : "false");
            context.addInstruction(
                    "Properties for AMQP based message event '" + trigger.getDescription() + "'");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".address' should be used to configure address name, defaults to '"
                    + context.getApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".address")
                            .orElse(sanitizedName.toUpperCase())
                    + "'");
        } else if (connector.equals(HTTP_CONNECTOR)) {

            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".path", "/" + sanitizedName.toLowerCase());
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".buffer-size", "10");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".broadcast", "true");

            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents",
                    isServerlessProcess() ? "true" : "false");
            context.addInstruction(
                    "Properties for HTTP based message event '" + trigger.getDescription() + "'");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".path' should be used to configure path, defaults to '"
                    + context.getApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".path")
                            .orElse("/" + sanitizedName.toLowerCase())
                    + "'");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".buffer-size' should be used to configure buffer size in case consumer cannot keep up, defaults to '"
                    + context.getApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".buffer-size")
                            .orElse("10")
                    + "'");
        } else if (connector.equals(PULSAR_CONNECTOR)) {

            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".topic",
                    (String) trigger.getContext("topic", trigger.getName()));
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".schema", "STRING");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".broadcast", "true");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".failure-strategy", "nack");
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents",
                    isServerlessProcess() ? "true" : "false");

            context.addInstruction(
                    "Properties for Apache Pulsar based message event '" + trigger.getDescription() + "'");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".serviceUrl' should be used to configure server name, defaults to '"
                    + context.getApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".serviceUrl")
                            .orElse("pulsar://localhost:6650")
                    + "'");
            context.addInstruction(
                    "\t'" + INCOMING_PROP_PREFIX + sanitizedName
                            + ".topic' should be used to configure Apache Pulsar topic defaults to '"
                            + trigger.getContext("topic", trigger.getName()) + "'");
        } else if (connector.equals(RABBITMQ_CONNECTOR)) {

            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".queue.name", sanitizedName.toUpperCase());
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".failure-strategy", "reject");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".exchange.name", "\\\"\\\"");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".routing-keys",
                    "${" + INCOMING_PROP_PREFIX + sanitizedName + ".queue.name" + "}");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".broadcast", "true");
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents",
                    isServerlessProcess() ? "true" : "false");
            context.addInstruction(
                    "Properties for RabbitMQ based message event '" + trigger.getDescription() + "'");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".queue.name' should be used to configure queue name, defaults to '"
                    + context.getApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".queue.name")
                            .orElse(sanitizedName.toUpperCase())
                    + "'");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".exchange.name' should be used to configure exchange name, defaults to '"
                    + context.getApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".exchange.name")
                            .orElse("\"\"")
                    + "'");
            context.addInstruction("\t'" + INCOMING_PROP_PREFIX + sanitizedName
                    + ".routing-keys' should be used to configure exchange name, defaults to '"
                    + context.getApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".routing-keys")
                            .orElse("${" + INCOMING_PROP_PREFIX + sanitizedName + ".queue.name" + "}")
                    + "'");
        } else if (connector.equals(DIRECT_CONNECTOR)) {

        }
    }

    protected String consumerTemplate(String connector) {
        if (connector.equals(MQTT_CONNECTOR)) {
            return "/class-templates/MQTTMessageConsumerTemplate.java";
        } else if (connector.equals(CAMEL_CONNECTOR)) {
            return "/class-templates/CamelMessageConsumerTemplate.java";
        } else if (connector.equals(KAFKA_CONNECTOR)) {
            return "/class-templates/KafkaMessageConsumerTemplate.java";
        } else if (connector.equals(OPERATOR_CONNECTOR)) {
            return "/class-templates/OperatorMessageConsumerTemplate.java";
        } else if (connector.equals(JMS_CONNECTOR)) {
            return "/class-templates/JMSMessageConsumerTemplate.java";
        } else if (connector.equals(AMQP_CONNECTOR)) {
            return "/class-templates/AMQPMessageConsumerTemplate.java";
        } else if (connector.equals(HTTP_CONNECTOR)) {
            return "/class-templates/HTTPMessageConsumerTemplate.java";
        } else if (connector.equals(PULSAR_CONNECTOR)) {
            return "/class-templates/PulsarMessageConsumerTemplate.java";
        } else if (connector.equals(RABBITMQ_CONNECTOR)) {
            return "/class-templates/RabbitMQMessageConsumerTemplate.java";
        } else if (connector.equals(DIRECT_CONNECTOR)) {
            return "/class-templates/DirectMessageConsumerTemplate.java";
        } else {
            return "/class-templates/MessageConsumerTemplate.java";
        }
    }

    public String generate() {
        String sanitizedName = CodegenUtils.triggerSanitizedName(trigger, process.getVersion());
        String connector = CodegenUtils.getConnector(INCOMING_PROP_PREFIX + sanitizedName + ".connector", context,
                (String) trigger.getContext("connector"));

        if (connector != null && !OPERATOR_CONNECTOR.equals(connector) && !DIRECT_CONNECTOR.equals(connector)) {

            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".connector", connector);
            appendConnectorSpecificProperties(connector);
        }

        // operator consumer only supports starting message endpoints
        if (OPERATOR_CONNECTOR.equals(connector) && !trigger.isStart()) {
            return null;
        }

        CompilationUnit clazz = parse(this.getClass().getResourceAsStream(consumerTemplate(connector)));
        clazz.setPackageDeclaration(process.getPackageName());
        clazz.addImport(modelfqcn);

        VariableScope variableScope = (VariableScope) ((io.automatiko.engine.workflow.process.core.WorkflowProcess) process)
                .getDefaultContext(VariableScope.VARIABLE_SCOPE);

        for (Variable var : variableScope.getVariables()) {
            if (var.getType() instanceof ObjectDataType && var.getType().getClassType() != null) {
                Class<?> varType = var.getType().getClassType();

                clazz.addImport(varType.getCanonicalName());
            }
        }

        // add functions so they can be easily accessed in message consumer classes
        clazz.addImport(new ImportDeclaration(BaseFunctions.class.getCanonicalName(), true, true));
        context.getBuildContext().classThatImplement(Functions.class.getCanonicalName())
                .forEach(c -> clazz.addImport(new ImportDeclaration(c, true, true)));

        ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class).get();
        template.setName(resourceClazzName);

        template.findAll(ClassOrInterfaceType.class).forEach(cls -> interpolateTypes(cls, dataClazzName));
        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("configure"))
                .forEach(md -> md.addAnnotation("jakarta.annotation.PostConstruct"));
        template.findAll(MethodDeclaration.class).stream()
                .filter(md -> md.getNameAsString().equals("consume") || md.getNameAsString().equals("cleanup")
                        || md.getNameAsString().equals("reconcile"))
                .forEach(md -> {
                    md.findAll(StringLiteralExpr.class)
                            .forEach(str -> str.setString(str.asString().replace("$Trigger$", trigger.getName())));
                    md.findAll(ClassOrInterfaceType.class).forEach(
                            t -> t.setName(t.getNameAsString().replace("$DataEventType$", messageDataEventClassName)));
                    md.findAll(ClassOrInterfaceType.class)
                            .forEach(t -> t.setName(t.getNameAsString().replace("$DataType$", trigger.getDataType())));

                    md.setType(md.getTypeAsString().replace("$DataType$", trigger.getDataType()));
                });

        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("convert"))
                .forEach(md -> {
                    md.setType(md.getTypeAsString().replace("$DataType$", trigger.getDataType()));

                    md.findAll(CastExpr.class)
                            .forEach(c -> c.setType(c.getTypeAsString().replace("$DataType$", trigger.getDataType())));
                    md.findAll(ClassOrInterfaceType.class)
                            .forEach(t -> t.setName(t.getNameAsString().replace("$DataType$", trigger.getDataType())));

                });

        if (trigger.getModelRef().startsWith("#")) {
            template.findAll(MethodCallExpr.class).stream().filter(m -> m.getNameAsString().endsWith("$ModelRef$"))
                    .forEach(m -> {
                        m.getParentNode().ifPresent(p -> p.removeForced());
                    });
        } else {
            template.findAll(MethodCallExpr.class).forEach(this::interpolateStrings);
        }
        if (useInjection()) {
            if (!OPERATOR_CONNECTOR.equals(connector)) {
                annotator.withApplicationComponent(template);
            }

            template.findAll(FieldDeclaration.class, fd -> isProcessField(fd))
                    .forEach(fd -> annotator.withNamedInjection(fd, processId + version));
            template.findAll(FieldDeclaration.class, fd -> isApplicationField(fd))
                    .forEach(fd -> annotator.withInjection(fd));
            template.findAll(FieldDeclaration.class, fd -> fd.getVariables().get(0).getNameAsString().equals("converter"))
                    .forEach(fd -> {
                        annotator.withInjection(fd);
                        fd.getVariable(0)
                                .setType(fd.getVariable(0).getTypeAsString().replace("$DataType$", trigger.getDataType()));
                    });

            template.findAll(FieldDeclaration.class, fd -> fd.getVariable(0).getNameAsString().equals("useCloudEvents"))
                    .forEach(fd -> {
                        Object cloudEvents = trigger.getContext("cloudEvents");
                        if (cloudEvents == null) {
                            annotator.withConfigInjection(fd, "quarkus.automatiko.messaging.as-cloudevents");
                        } else {
                            fd.getVariable(0)
                                    .setInitializer(new MethodCallExpr(
                                            new NameExpr(Optional.class.getCanonicalName()),
                                            "of")
                                                    .addArgument(new BooleanLiteralExpr(
                                                            Boolean.parseBoolean(cloudEvents.toString()))));
                        }
                    });

            template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("consume"))
                    .forEach(md -> {

                        annotator.withIncomingMessage(md, sanitizedName);

                        String mergeMode = (String) trigger.getContext("mergeMode");

                        if (mergeMode != null) {
                            md.addAnnotation(new SingleMemberAnnotationExpr(
                                    new Name("io.smallrye.reactive.messaging.annotations.Merge"),
                                    new FieldAccessExpr(new TypeExpr(
                                            new ClassOrInterfaceType(null,
                                                    "io.smallrye.reactive.messaging.annotations.Merge.Mode")),
                                            mergeMode)));
                        }

                        if (context.getBuildContext().isTracingSupported()) {
                            md.addAnnotation("io.opentelemetry.instrumentation.annotations.WithSpan");
                        }
                        String ackMode = (String) trigger.getContext("ack-mode");
                        if ("NONE".equalsIgnoreCase(ackMode)) {
                            annotator.withNoneMessageAckMode(md);
                        } else if ("MANUAL".equalsIgnoreCase(ackMode)) {
                            annotator.withManualMessageAckMode(md);
                        } else if ("PRE".equalsIgnoreCase(ackMode)) {
                            annotator.withPreMessageAckMode(md);
                        } else if ("POST".equalsIgnoreCase(ackMode)) {
                            annotator.withPostMessageAckMode(md);
                        }
                    });
        } else {
            template.findAll(FieldDeclaration.class, fd -> isProcessField(fd))
                    .forEach(fd -> initializeProcessField(fd, template));

            template.findAll(FieldDeclaration.class, fd -> isApplicationField(fd))
                    .forEach(fd -> initializeApplicationField(fd, template));

            template.findAll(FieldDeclaration.class, fd -> fd.getVariables().get(0).getNameAsString().equals("converter"))
                    .forEach(fd -> {
                        fd.getVariable(0)
                                .setType(fd.getVariable(0).getTypeAsString().replace("$DataType$", trigger.getDataType()));
                    });
        }
        BlockStmt filterExpressionBody = new BlockStmt();
        if (trigger.getContext("filterExpression") != null) {

            filterExpressionBody.addStatement(new ReturnStmt(new NameExpr((String) trigger.getContext("filterExpression"))));
        } else {
            filterExpressionBody.addStatement(new ReturnStmt(new BooleanLiteralExpr(true)));
        }

        BlockStmt body = new BlockStmt();
        if (trigger.getCorrelation() != null) {

            body.addStatement(new ReturnStmt(new StringLiteralExpr(trigger.getCorrelation())));

        } else if (trigger.getCorrelationExpression() != null) {

            body.addStatement(new ReturnStmt(new NameExpr(trigger.getCorrelationExpression())));
        } else {
            body.addStatement(new ReturnStmt(new NullLiteralExpr()));
        }

        boolean cloudEvents = context.getBuildContext().config().messaging().asCloudevents() ||
                Boolean.parseBoolean(
                        context.getApplicationProperty("quarkus.automatiko.messaging.as-cloudevents").orElse("false"));

        if (cloudEvents) {

            template.findAll(MethodDeclaration.class).stream()
                    .filter(md -> md.getNameAsString().equals("correlationEvent")).forEach(md -> {
                        md.setBody(body);
                        md.getParameters().get(0).setType(messageDataEventClassName);
                    });

            template.findAll(MethodDeclaration.class).stream()
                    .filter(md -> md.getNameAsString().equals("acceptedEvent")).forEach(md -> {
                        md.setBody(filterExpressionBody);
                        md.getParameters().get(0).setType(messageDataEventClassName);
                    });
        } else {

            template.findAll(MethodDeclaration.class).stream()
                    .filter(md -> md.getNameAsString().equals("correlationPayload")).forEach(md -> {
                        md.setBody(body);
                        md.getParameters().get(0).setType(trigger.getDataType());
                    });
            template.findAll(MethodDeclaration.class).stream()
                    .filter(md -> md.getNameAsString().equals("acceptedPayload")).forEach(md -> {
                        md.setBody(filterExpressionBody);
                        md.getParameters().get(0).setType(trigger.getDataType());
                    });
        }

        template.addMember(
                new MethodDeclaration().setName("canStartInstance").setType(Boolean.class).setModifiers(Keyword.PROTECTED)
                        .setBody(new BlockStmt().addStatement(new ReturnStmt(new BooleanLiteralExpr(trigger.isStart())))));

        // add connector and message name as static fields of the class
        FieldDeclaration connectorField = new FieldDeclaration().setStatic(true).setFinal(true)
                .addVariable(new VariableDeclarator(new ClassOrInterfaceType(null, "String"), "CONNECTOR",
                        new StringLiteralExpr(connector)));
        template.addMember(connectorField);

        FieldDeclaration messageNameField = new FieldDeclaration().setStatic(true).setFinal(true)
                .addVariable(new VariableDeclarator(new ClassOrInterfaceType(null, "String"), "MESSAGE",
                        new StringLiteralExpr(trigger.getName())));
        template.addMember(messageNameField);

        if (OPERATOR_CONNECTOR.equals(connector)) {
            // add event filter
            String eventFilterClass = (String) trigger.getContext("eventFilter");

            if (eventFilterClass != null && !eventFilterClass.isEmpty()) {
                AnnotationExpr controllerAnnotation = template.getAnnotationByName("Controller").get();
                ((NormalAnnotationExpr) controllerAnnotation).addPair("eventFilters",
                        new NameExpr(eventFilterClass + ".class"));

            }
        }

        template.getMembers().sort(new BodyDeclarationComparator());
        ImportsOrganizer.organize(clazz);
        return clazz.toString().replaceAll("\\$DataType\\$", trigger.getDataType())
                .replaceAll("\\$DataEventType\\$", messageDataEventClassName)
                .replaceAll("\\$ProcessId\\$", processId + version)
                .replaceAll("\\$GenControllerParam\\$", (String) trigger.getContext("generationAware", "true"))
                .replaceAll("\\$ControllerParam\\$",
                        "{" + Stream.of(namespaces.split(","))
                                .filter(s -> !s.trim().isEmpty()).map(s -> "\"" + s.trim() + "\"")
                                .collect(Collectors.joining(",")) + "}");
    }

    private void initializeProcessField(FieldDeclaration fd, ClassOrInterfaceDeclaration template) {
        fd.getVariable(0).setInitializer(new ObjectCreationExpr().setType(processClazzName));
    }

    private void initializeApplicationField(FieldDeclaration fd, ClassOrInterfaceDeclaration template) {
        fd.getVariable(0).setInitializer(new ObjectCreationExpr().setType(appCanonicalName));
    }

    private void interpolateStrings(MethodCallExpr vv) {
        String s = vv.getNameAsString();
        String interpolated = s.replace("$ModelRef$", StringUtils.capitalize(trigger.getModelRef()));
        vv.setName(interpolated);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((processId == null) ? 0 : processId.hashCode());
        result = prime * result + ((trigger == null && trigger.getName() != null) ? 0 : trigger.getName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MessageConsumerGenerator other = (MessageConsumerGenerator) obj;
        if (processId == null) {
            if (other.processId != null)
                return false;
        } else if (!processId.equals(other.processId))
            return false;
        if (trigger == null) {
            if (other.trigger != null)
                return false;
        } else if (!trigger.getName().equals(other.trigger.getName()))
            return false;
        return true;
    }

    private boolean isServerlessProcess() {
        return (boolean) process.getMetaData().getOrDefault("IsServerlessWorkflow", false);
    }
}
