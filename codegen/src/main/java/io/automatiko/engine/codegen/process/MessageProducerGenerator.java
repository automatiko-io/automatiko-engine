
package io.automatiko.engine.codegen.process;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatiko.engine.codegen.CodeGenConstants.CAMEL_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.KAFKA_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.MQTT_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.OUTGOING_PROP_PREFIX;
import static io.automatiko.engine.codegen.CodegenUtils.interpolateTypes;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.Functions;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.BodyDeclarationComparator;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.services.execution.BaseFunctions;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.compiler.canonical.TriggerMetaData;

public class MessageProducerGenerator {

    private static final String EVENT_DATA_VAR = "eventData";

    private final String relativePath;

    private GeneratorContext context;

    private WorkflowProcess process;
    private final String packageName;
    private final String resourceClazzName;
    private final String modelClazzName;
    private String processId;
    private final String processName;
    private final String classPrefix;
    private final String messageDataEventClassName;
    private DependencyInjectionAnnotator annotator;

    private TriggerMetaData trigger;

    public MessageProducerGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn,
            String processfqcn, String messageDataEventClassName, TriggerMetaData trigger) {
        this.context = context;
        this.process = process;
        this.trigger = trigger;
        this.packageName = process.getPackageName();
        this.processId = process.getId();
        this.processName = processId.substring(processId.lastIndexOf('.') + 1);
        this.classPrefix = StringUtils.capitalize(processName) + CodegenUtils.version(process.getVersion());
        this.resourceClazzName = classPrefix + "MessageProducer_" + trigger.getOwnerId();
        this.relativePath = packageName.replace(".", "/") + "/" + resourceClazzName + ".java";
        this.messageDataEventClassName = messageDataEventClassName;
        this.modelClazzName = modelfqcn;
    }

    public MessageProducerGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
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

            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".topic",
                    (String) trigger.getContext("topic", trigger.getName()));
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".host", "${mqtt.server:localhost}");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".port", "${mqtt.port:1883}");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".client-id",
                    classPrefix + "-producer");
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents", "false");

            context.addInstruction(
                    "Properties for MQTT based message event '" + trigger.getDescription() + "'");
            context.addInstruction(
                    "\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                            + ".topic' should be used to configure MQTT topic defaults to '"
                            + trigger.getContext("topic", trigger.getName()) + "'");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".host' should be used to configure MQTT host that defaults to localhost");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".port' should be used to configure MQTT port that defaults to 1883");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".client-id' should be used to configure MQTT client id that defaults to '" + classPrefix
                    + "-producer'");
        } else if (connector.equals(CAMEL_CONNECTOR)) {
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".endpoint-uri", "");
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents", "false");
            context.addInstruction(
                    "Properties for Apache Camel based message event '" + trigger.getDescription() + "'");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".endpoint-uri' should be used to configure Apache Camel location");
        } else if (connector.equals(KAFKA_CONNECTOR)) {

            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".bootstrap.servers",
                    "${kafka.servers:localhost:9092}");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".topic",
                    (String) trigger.getContext("topic", trigger.getName()));
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".key.serializer",
                    "org.apache.kafka.common.serialization.StringSerializer");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".value.serializer",
                    "org.apache.kafka.common.serialization.StringSerializer");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".group.id",
                    classPrefix + "-consumer");
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents", "false");
            context.addInstruction(
                    "Properties for Apache Kafka based message event '" + trigger.getDescription() + "'");
            context.addInstruction(
                    "\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                            + ".topic' should be used to configure Kafka topic defaults to '"
                            + trigger.getContext("topic", trigger.getName()) + "'");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".bootstrap.servers' should be used to configure Kafka bootstrap servers host that defaults to localhost:9092");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".key.serializer' should be used to configure key deserializer port that defaults to StringSerializer");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".value.serializer' should be used to configure key deserializer port that defaults to StringSerializer");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".group.id' should be used to configure Kafka group id that defaults to '" + classPrefix
                    + "-consumer'");
        }
    }

    protected String producerTemplate(String connector) {
        if (connector.equals(MQTT_CONNECTOR)) {
            return "/class-templates/MQTTMessageProducerTemplate.java";
        } else if (connector.equals(CAMEL_CONNECTOR)) {
            return "/class-templates/CamelMessageProducerTemplate.java";
        } else if (connector.equals(KAFKA_CONNECTOR)) {
            return "/class-templates/KafkaMessageProducerTemplate.java";
        } else {
            return "/class-templates/MessageProducerTemplate.java";
        }
    }

    public String generate() {

        String sanitizedName = CodegenUtils.triggerSanitizedName(trigger, process.getVersion());
        String connector = CodegenUtils.getConnector(OUTGOING_PROP_PREFIX + sanitizedName + ".connector", context,
                (String) trigger.getContext("connector"));
        if (connector != null) {

            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".connector", connector);

            appendConnectorSpecificProperties(connector);
        }

        CompilationUnit clazz = parse(
                this.getClass().getResourceAsStream(producerTemplate(connector)));
        clazz.setPackageDeclaration(process.getPackageName());

        // add functions so they can be easily accessed in message producer classes
        clazz.addImport(new ImportDeclaration(BaseFunctions.class.getCanonicalName(), true, true));
        context.getBuildContext().classThatImplement(Functions.class.getCanonicalName())
                .forEach(c -> clazz.addImport(new ImportDeclaration(c, true, true)));

        ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class).get();
        template.setName(resourceClazzName);

        template.findAll(ClassOrInterfaceType.class).forEach(cls -> interpolateTypes(cls, trigger.getDataType()));
        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("produce"))
                .forEach(md -> md.getParameters().stream().filter(p -> p.getNameAsString().equals(EVENT_DATA_VAR))
                        .forEach(p -> p.setType(trigger.getDataType())));
        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("configure"))
                .forEach(md -> md.addAnnotation("javax.annotation.PostConstruct"));
        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("marshall"))
                .forEach(md -> {
                    md.getParameters().stream().filter(p -> p.getNameAsString().equals(EVENT_DATA_VAR))
                            .forEach(p -> p.setType(trigger.getDataType()));
                    md.findAll(ClassOrInterfaceType.class).forEach(
                            t -> t.setName(t.getNameAsString().replace("$DataEventType$", messageDataEventClassName)));
                });

        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("convert"))
                .forEach(md -> {
                    md.setType(md.getTypeAsString().replace("$DataType$", trigger.getDataType()));

                    md.findAll(CastExpr.class)
                            .forEach(c -> c.setType(c.getTypeAsString().replace("$DataType$", trigger.getDataType())));
                    md.findAll(ClassOrInterfaceType.class)
                            .forEach(t -> t.setName(t.getNameAsString().replace("$DataType$", trigger.getDataType())));

                });

        String topicExpression = (String) trigger.getContext("topicExpression");
        if (topicExpression != null) {
            template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("topic"))
                    .forEach(md -> {
                        BlockStmt body = new BlockStmt();

                        ClassOrInterfaceType stringType = new ClassOrInterfaceType(null, String.class.getCanonicalName());

                        if (topicExpression.contains("id")) {
                            VariableDeclarationExpr idField = new VariableDeclarationExpr(stringType, "id");
                            body.addStatement(new AssignExpr(idField,
                                    new MethodCallExpr(new NameExpr("pi"), "getId"), AssignExpr.Operator.ASSIGN));
                        }

                        if (topicExpression.contains("businessKey")) {
                            VariableDeclarationExpr businessKeyField = new VariableDeclarationExpr(stringType, "businessKey");
                            body.addStatement(new AssignExpr(businessKeyField,
                                    new MethodCallExpr(new NameExpr("pi"), "getCorrelationKey"), AssignExpr.Operator.ASSIGN));
                        }
                        VariableScope variableScope = (VariableScope) ((io.automatiko.engine.workflow.process.core.WorkflowProcess) process)
                                .getDefaultContext(VariableScope.VARIABLE_SCOPE);

                        for (Variable var : variableScope.getVariables()) {

                            if (topicExpression.contains(var.getSanitizedName())) {
                                ClassOrInterfaceType varType = new ClassOrInterfaceType(null, var.getType().getStringType());
                                VariableDeclarationExpr v = new VariableDeclarationExpr(
                                        varType,
                                        var.getSanitizedName());
                                body.addStatement(new AssignExpr(v,
                                        new CastExpr(varType,
                                                new MethodCallExpr(new MethodCallExpr(new NameExpr("pi"), "getVariables"),
                                                        "get")
                                                                .addArgument(new StringLiteralExpr(var.getName()))),
                                        AssignExpr.Operator.ASSIGN));
                            }
                        }
                        body.addStatement(new ReturnStmt(new NameExpr(topicExpression)));
                        md.setBody(body);
                    });
        }

        if (useInjection()) {
            annotator.withApplicationComponent(template);

            FieldDeclaration emitterField = template.findFirst(FieldDeclaration.class)
                    .filter(fd -> fd.getVariable(0).getNameAsString().equals("emitter")).get();
            annotator.withInjection(emitterField);
            annotator.withOutgoingMessage(emitterField, sanitizedName);
            emitterField.getVariable(0).setType(annotator.emitterType("Message"));

            template.findAll(FieldDeclaration.class, fd -> fd.getVariable(0).getNameAsString().equals("useCloudEvents"))
                    .forEach(fd -> annotator.withConfigInjection(fd, "quarkus.automatiko.messaging.as-cloudevents"));

        }
        template.getMembers().sort(new BodyDeclarationComparator());
        return clazz.toString();
    }

}
