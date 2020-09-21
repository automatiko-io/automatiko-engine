
package io.automatik.engine.codegen.process;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatik.engine.codegen.CodeGenConstants.CAMEL_CONNECTOR;
import static io.automatik.engine.codegen.CodeGenConstants.INCOMING_PROP_PREFIX;
import static io.automatik.engine.codegen.CodeGenConstants.MQTT_CONNECTOR;
import static io.automatik.engine.codegen.CodegenUtils.interpolateTypes;
import static io.automatik.engine.codegen.CodegenUtils.isApplicationField;
import static io.automatik.engine.codegen.CodegenUtils.isProcessField;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.codegen.BodyDeclarationComparator;
import io.automatik.engine.codegen.CodegenUtils;
import io.automatik.engine.codegen.GeneratorContext;
import io.automatik.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatik.engine.services.utils.StringUtils;
import io.automatik.engine.workflow.compiler.canonical.TriggerMetaData;

public class MessageConsumerGenerator {

    private final String relativePath;

    private GeneratorContext context;

    private WorkflowProcess process;
    private final String packageName;
    private final String resourceClazzName;
    private final String processClazzName;
    private String processId;
    private String dataClazzName;
    private String modelfqcn;
    private final String processName;
    private final String appCanonicalName;
    private final String messageDataEventClassName;
    private DependencyInjectionAnnotator annotator;

    private TriggerMetaData trigger;

    public MessageConsumerGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn,
            String processfqcn, String appCanonicalName, String messageDataEventClassName, TriggerMetaData trigger) {
        this.context = context;
        this.process = process;
        this.trigger = trigger;
        this.packageName = process.getPackageName();
        this.processId = process.getId();
        this.processName = processId.substring(processId.lastIndexOf('.') + 1);
        String classPrefix = StringUtils.capitalize(processName) + CodegenUtils.version(process.getVersion());
        this.resourceClazzName = classPrefix + "MessageConsumer_" + trigger.getOwnerId();
        this.relativePath = packageName.replace(".", "/") + "/" + resourceClazzName + ".java";
        this.modelfqcn = modelfqcn;
        this.dataClazzName = modelfqcn.substring(modelfqcn.lastIndexOf('.') + 1);
        this.processClazzName = processfqcn;
        this.appCanonicalName = appCanonicalName;
        this.messageDataEventClassName = messageDataEventClassName;
    }

    public MessageConsumerGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
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
        if (connector.equals(MQTT_CONNECTOR)) {
            String sanitizedName = CodegenUtils.triggerSanitizedName(trigger, process.getVersion());
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".topic", trigger.getName());
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".host", "localhost");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".port", "1883");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".client-id",
                    sanitizedName + "-consumer");
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".failure-strategy", "ignore");
            context.setApplicationProperty("quarkus.automatik.messaging.as-cloudevents", "false");
        } else if (connector.equals(CAMEL_CONNECTOR)) {
            String sanitizedName = CodegenUtils.triggerSanitizedName(trigger, process.getVersion());
            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".endpoint-uri", "");
        }
    }

    protected String consumerTemplate(String connector) {
        if (connector.equals(MQTT_CONNECTOR)) {
            return "/class-templates/MQTTMessageConsumerTemplate.java";
        } else if (connector.equals(CAMEL_CONNECTOR)) {
            return "/class-templates/CamelMessageConsumerTemplate.java";
        } else {
            return "/class-templates/MessageConsumerTemplate.java";
        }
    }

    public String generate() {
        String sanitizedName = CodegenUtils.triggerSanitizedName(trigger, process.getVersion());
        String connector = CodegenUtils.getConnector(context);
        if (connector != null) {

            context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".connector", connector);
            appendConnectorSpecificProperties(connector);
        }
        CompilationUnit clazz = parse(this.getClass().getResourceAsStream(consumerTemplate(connector)));
        clazz.setPackageDeclaration(process.getPackageName());
        clazz.addImport(modelfqcn);

        ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class).get();
        template.setName(resourceClazzName);

        template.findAll(ClassOrInterfaceType.class).forEach(cls -> interpolateTypes(cls, dataClazzName));
        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("configure"))
                .forEach(md -> md.addAnnotation("javax.annotation.PostConstruct"));
        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("consume"))
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
            annotator.withApplicationComponent(template);

            template.findAll(FieldDeclaration.class, fd -> isProcessField(fd))
                    .forEach(fd -> annotator.withNamedInjection(fd, processId));
            template.findAll(FieldDeclaration.class, fd -> isApplicationField(fd))
                    .forEach(fd -> annotator.withInjection(fd));
            template.findAll(FieldDeclaration.class, fd -> fd.getVariables().get(0).getNameAsString().equals("converter"))
                    .forEach(fd -> {
                        annotator.withInjection(fd);
                        fd.getVariable(0)
                                .setType(fd.getVariable(0).getTypeAsString().replace("$DataType$", trigger.getDataType()));
                    });

            template.findAll(FieldDeclaration.class, fd -> fd.getVariable(0).getNameAsString().equals("useCloudEvents"))
                    .forEach(fd -> annotator.withConfigInjection(fd, "quarkus.automatik.messaging.as-cloudevents"));

            template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("consume"))
                    .forEach(md -> annotator.withIncomingMessage(md, sanitizedName));
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
        BlockStmt body = new BlockStmt();
        if (trigger.getCorrelation() != null) {

            body.addStatement(new ReturnStmt(new StringLiteralExpr(trigger.getCorrelation())));

        } else if (trigger.getCorrelationExpression() != null) {

            body.addStatement(new ReturnStmt(new NameExpr(trigger.getCorrelationExpression())));
        } else {
            body.addStatement(new ReturnStmt(new NullLiteralExpr()));
        }

        boolean cloudEvents = context.getBuildContext().config().messaging().asCloudevents();

        if (cloudEvents) {

            template.findAll(MethodDeclaration.class).stream()
                    .filter(md -> md.getNameAsString().equals("correlationEvent")).forEach(md -> {
                        md.setBody(body);
                        md.getParameters().get(0).setType(messageDataEventClassName);
                    });
        } else {

            template.findAll(MethodDeclaration.class).stream()
                    .filter(md -> md.getNameAsString().equals("correlationPayload")).forEach(md -> {
                        md.setBody(body);
                        md.getParameters().get(0).setType(trigger.getDataType());
                    });

        }
        template.getMembers().sort(new BodyDeclarationComparator());
        return clazz.toString();
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
}
