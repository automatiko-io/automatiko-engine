
package io.automatiko.engine.codegen.process;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatiko.engine.codegen.CodegenUtils.interpolateTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.codegen.Generated;
import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.BodyDeclarationComparator;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.codegen.ImportsOrganizer;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.compiler.canonical.ProcessToExecModelGenerator;
import io.automatiko.engine.workflow.compiler.canonical.TriggerMetaData;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.FaultNode;
import io.automatiko.engine.workflow.process.core.node.RuleSetNode;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;

public class FunctionFlowGenerator {

    public static final Pattern PARAMETER_MATCHER = Pattern.compile("\\{([\\S|\\p{javaWhitespace}&&[^\\}]]+)\\}",
            Pattern.DOTALL);

    private final String relativePath;

    private final GeneratorContext context;
    private WorkflowProcess process;
    private final String functionClazzName;
    private String processId;
    private final String processName;
    private String version = "";
    private String fversion = "";
    private String dataClazzName;
    private String modelfqcn;
    private final String appCanonicalName;
    private final String processClazzName;
    private DependencyInjectionAnnotator annotator;

    private Map<String, String> signals;
    private Map<String, Node> signalNodes;

    private List<TriggerMetaData> triggers;

    public FunctionFlowGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn,
            String processfqcn, String appCanonicalName) {
        this.context = context;
        this.process = process;
        this.processId = ProcessToExecModelGenerator.extractProcessId(process.getId(), null);
        this.processName = processId.substring(processId.lastIndexOf('.') + 1);
        if (process.getVersion() != null && !process.getVersion().trim().isEmpty()) {
            this.version = CodegenUtils.version(process.getVersion());
            this.fversion = CodegenUtils.version(process.getVersion(), ".v");
        }
        this.appCanonicalName = appCanonicalName;
        String classPrefix = StringUtils.capitalize(processName);
        this.functionClazzName = classPrefix + "Functions" + version;
        this.relativePath = process.getPackageName().replace(".", "/") + "/" + functionClazzName + ".java";
        this.modelfqcn = modelfqcn + "Output";
        this.dataClazzName = modelfqcn.substring(modelfqcn.lastIndexOf('.') + 1);
        this.processClazzName = processfqcn;
    }

    public FunctionFlowGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
        return this;
    }

    public FunctionFlowGenerator withSignals(Map<String, String> signals, Map<String, Node> signalNodes) {
        this.signals = signals;
        this.signalNodes = signalNodes;
        return this;
    }

    public FunctionFlowGenerator withTriggers(List<TriggerMetaData> triggers) {
        this.triggers = triggers;
        return this;
    }

    public String className() {
        return functionClazzName;
    }

    public String generatedFilePath() {
        return relativePath;
    }

    protected boolean useInjection() {
        return this.annotator != null;
    }

    public String generate() {
        CompilationUnit clazz = parse(
                this.getClass().getResourceAsStream("/class-templates/FunctionFlowTemplate.java"));
        clazz.setPackageDeclaration(process.getPackageName());
        clazz.addImport(modelfqcn);

        ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new IllegalStateException("Cannot find the class in FunctionFlowTemplate"));
        template.setName(functionClazzName);

        List<String> discoveredFunctions = new ArrayList<>();

        // first to initiate the function flow

        template.findFirst(MethodDeclaration.class, md -> md.getNameAsString().equals("startTemplate")).ifPresent(md -> {
            md.setName(processId.toLowerCase() + version);
            md.getBody().get().findFirst(StringLiteralExpr.class, s -> s.getValue().equals("$TypePrefix$"))
                    .ifPresent(s -> s.setValue(process.getPackageName() + "." + processId + fversion));

            if (useInjection()) {
                String trigger = functionTrigger(process);
                String filter = functionFilter(process);
                if (filter != null) {
                    Matcher matcher = PARAMETER_MATCHER.matcher(filter);
                    while (matcher.find()) {
                        String paramName = matcher.group(1);

                        Optional<String> value = context.getApplicationProperty(paramName);
                        if (value.isPresent()) {
                            filter = filter.replaceAll("\\{" + paramName + "\\}", value.get());
                        } else {
                            throw new IllegalArgumentException("Missing argument declared in as function filter with name '"
                                    + paramName + "'. Define it in application.properties file");
                        }
                    }
                }
                annotator.withCloudEventMapping(md, trigger, filter);
            }
        });

        MethodDeclaration callTemplate = template
                .findFirst(MethodDeclaration.class, md -> md.getNameAsString().equals("callTemplate")).get();

        discoveredFunctions.add(definedFunctionTrigger(process));

        // for each "execution" node add new function
        for (Node node : process.getNodesRecursively()) {

            if (isExecutionNode(node)) {

                discoveredFunctions.add(definedFunctionTrigger(node));

                MethodDeclaration flowStepFunction = callTemplate.clone();

                if (useInjection()) {
                    String trigger = functionTrigger(node);
                    String filter = functionFilter(node);
                    if (filter != null) {
                        Matcher matcher = PARAMETER_MATCHER.matcher(filter);
                        while (matcher.find()) {
                            String paramName = matcher.group(1);

                            Optional<String> value = context.getApplicationProperty(paramName);
                            if (value.isPresent() && !value.get().isEmpty()) {
                                filter = filter.replaceAll("\\{" + paramName + "\\}", value.get());
                            } else {
                                throw new IllegalArgumentException("Missing argument declared in as function filter with name '"
                                        + paramName + "'. Define it in application.properties file");
                            }
                        }
                    }
                    annotator.withCloudEventMapping(flowStepFunction, trigger, filter);
                }

                flowStepFunction.getBody().get().findFirst(StringLiteralExpr.class, s -> s.getValue().equals("$StartFromNode$"))
                        .ifPresent(s -> s.setValue((String) node.getMetaData().get("UniqueId")));
                flowStepFunction.getBody().get().findFirst(StringLiteralExpr.class, s -> s.getValue().equals("$TypePrefix$"))
                        .ifPresent(s -> s.setValue(process.getPackageName() + "." + processId + fversion + "."));
                flowStepFunction.getBody().get().findFirst(StringLiteralExpr.class, s -> s.getValue().equals("$ThisNode$"))
                        .ifPresent(s -> s.setValue(node.getName()));

                flowStepFunction.setName(sanitizeIdentifier(node.getName() + version).toLowerCase());

                template.addMember(flowStepFunction);
            } else if (node instanceof EndNode || node instanceof FaultNode) {
                discoveredFunctions.add(definedFunctionTrigger(node));
            }
        }
        // remove the template method
        callTemplate.removeForced();

        // process signals

        MethodDeclaration signalTemplate = template
                .findFirst(MethodDeclaration.class, md -> md.getNameAsString().equals("signalTemplate")).get();
        Optional.ofNullable(signals).ifPresent(signalsMap -> {
            AtomicInteger index = new AtomicInteger(0);
            signalsMap.entrySet().stream().filter(e -> Objects.nonNull(e.getKey())).forEach(entry -> {
                String signalName = entry.getKey();
                String signalType = entry.getValue();
                MethodDeclaration flowSignalFunction = produceSignalFunction("", signalName, signalType, signalTemplate, index,
                        signalNodes.get(signalName));
                template.addMember(flowSignalFunction);
            });
        });

        // process triggers (consume messages)

        if (triggers != null && !triggers.isEmpty()) {
            AtomicInteger index = new AtomicInteger(0);
            for (TriggerMetaData trigger : triggers) {
                if (trigger.getType().equals(TriggerMetaData.TriggerType.ConsumeMessage)) {
                    String signalName = trigger.getName();
                    String signalType = trigger.getDataType();

                    MethodDeclaration flowSignalFunction = produceSignalFunction("Message-", signalName, signalType,
                            signalTemplate, index,
                            (Node) trigger.getContext("_node_"));
                    VariableDeclarationExpr correlationVar = flowSignalFunction.findFirst(VariableDeclarationExpr.class,
                            vd -> vd.getVariable(0).getNameAsString().equals("correlation")).get();
                    if (trigger.getCorrelation() != null) {
                        correlationVar.getVariable(0).setInitializer(new StringLiteralExpr(trigger.getCorrelation()));
                    } else if (trigger.getCorrelationExpression() != null) {
                        correlationVar.getVariable(0).setInitializer(new NameExpr(trigger.getCorrelationExpression()));
                    }

                    template.addMember(flowSignalFunction);
                }
            }
        }

        // remove the template method
        signalTemplate.removeForced();

        Map<String, String> typeInterpolations = new HashMap<>();
        typeInterpolations.put("$Clazz$", functionClazzName);
        typeInterpolations.put("$Type$", dataClazzName);
        template.findAll(ClassOrInterfaceType.class).forEach(cls -> interpolateTypes(cls, typeInterpolations));

        if (useInjection()) {
            template.findAll(FieldDeclaration.class, CodegenUtils::isProcessField)
                    .forEach(fd -> annotator.withNamedInjection(fd, processId + version));

            template.findAll(FieldDeclaration.class, CodegenUtils::isApplicationField)
                    .forEach(fd -> annotator.withInjection(fd));

            template.findAll(FieldDeclaration.class, CodegenUtils::isIdentitySupplierField)
                    .forEach(fd -> annotator.withInjection(fd));

            template.findAll(FieldDeclaration.class, CodegenUtils::isEventSourceField)
                    .forEach(fd -> annotator.withInjection(fd));

            template.findAll(MethodDeclaration.class, md -> md.isPublic()).forEach(md -> annotator.withFunction(md));
        }

        NodeList<Expression> items = NodeList
                .nodeList(discoveredFunctions.stream().map(s -> new StringLiteralExpr(s)).collect(Collectors.toList()));

        template.addAnnotation(new NormalAnnotationExpr(new Name(Generated.class.getCanonicalName()),
                NodeList.nodeList(new MemberValuePair("value", new ArrayInitializerExpr(items)),
                        new MemberValuePair("reference",
                                new StringLiteralExpr(
                                        context.getApplicationProperty("quarkus.google.cloud.project-id").orElse("missing"))),
                        new MemberValuePair("name",
                                new StringLiteralExpr(StringUtils.capitalize(
                                        ProcessToExecModelGenerator.extractProcessId(processId, version)))),
                        new MemberValuePair("hidden", new BooleanLiteralExpr(false)))));

        template.getMembers().sort(new BodyDeclarationComparator());
        ImportsOrganizer.organize(clazz);
        return clazz.toString();
    }

    private boolean isExecutionNode(Node node) {
        if (Boolean.parseBoolean((String) node.getMetaData().getOrDefault("functionFlowContinue", "false"))) {
            // if node has "functionFlowContinue" set then it should not create new function but continue with workflow execution in the same call
            return false;
        }

        if (node instanceof WorkItemNode || node instanceof ActionNode || node instanceof RuleSetNode
                || node instanceof SubProcessNode || node instanceof EventNode) {

            if (node instanceof BoundaryEventNode) {
                return false;
            }

            // ignore those that are attached to start node in the top level process
            if (node.getParentContainer() instanceof WorkflowProcess
                    && node.getIncomingConnections().values().stream().flatMap(c -> c.stream())
                            .anyMatch(c -> c.getFrom() instanceof StartNode)) {
                return false;
            }
            return true;
        }
        return false;
    }

    private String sanitizeIdentifier(String name) {
        return name.replaceAll("\\s", "").toLowerCase();
    }

    private String functionTrigger(Node node) {

        if (context.getApplicationProperty("quarkus.automatiko.target-deployment").orElse("unknown").equals("gcp-pubsub")) {
            return "google.cloud.pubsub.topic.v1.messagePublished";
        }

        return (String) node.getMetaData().getOrDefault("functionType",
                process.getPackageName() + "." + processId + fversion + "."
                        + sanitizeIdentifier(node.getName()).toLowerCase());
    }

    private String functionTrigger(WorkflowProcess process) {

        if (context.getApplicationProperty("quarkus.automatiko.target-deployment").orElse("unknown").equals("gcp-pubsub")) {
            return "google.cloud.pubsub.topic.v1.messagePublished";
        }

        return (String) process.getMetaData().getOrDefault("functionType",
                process.getPackageName() + "." + processId + fversion);
    }

    private String definedFunctionTrigger(Node node) {

        return (String) node.getMetaData().getOrDefault("functionType",
                process.getPackageName() + "." + processId + fversion + "."
                        + sanitizeIdentifier(node.getName()).toLowerCase());
    }

    private String definedFunctionTrigger(WorkflowProcess process) {

        return (String) process.getMetaData().getOrDefault("functionType",
                process.getPackageName() + "." + processId + fversion);
    }

    private String functionFilter(Node node) {

        String filter = (String) node.getMetaData().get("functionFilter");

        if (filter == null && context.getApplicationProperty("quarkus.automatiko.target-deployment").orElse("unknown")
                .equals("gcp-pubsub")) {
            return "source=//pubsub.googleapis.com/projects/{quarkus.google.cloud.project-id}/topics/"
                    + definedFunctionTrigger(node);
        }

        return filter;
    }

    private String functionFilter(WorkflowProcess process) {

        String filter = (String) process.getMetaData().get("functionFilter");

        if (filter == null && context.getApplicationProperty("quarkus.automatiko.target-deployment").orElse("unknown")
                .equals("gcp-pubsub")) {
            return "source=//pubsub.googleapis.com/projects/{quarkus.google.cloud.project-id}/topics/"
                    + definedFunctionTrigger(process);
        }

        return filter;
    }

    private MethodDeclaration produceSignalFunction(String signalNamePrefix, String signalName, String signalType,
            MethodDeclaration signalTemplate,
            AtomicInteger index, Node node) {

        if (signalType == null) {
            throw new IllegalStateException(
                    "Workflow as Function Flow with signals requires to have event data associated with signal");
        }

        String methodName = sanitizeIdentifier(signalName + version) + "_" + index.getAndIncrement();

        MethodDeclaration flowSignalFunction = signalTemplate.clone();

        if (signalType != null) {
            flowSignalFunction.findAll(ClassOrInterfaceType.class).forEach(name -> {
                String identifier = name.getNameAsString();
                name.setName(identifier.replace("$signalType$", signalType));
            });
        }

        flowSignalFunction.findAll(StringLiteralExpr.class).forEach(vv -> {
            String s = vv.getValue();
            String interpolated = s.replace("$signalName$", signalNamePrefix + signalName);
            vv.setString(interpolated);
        });

        if (useInjection()) {
            String trigger = functionTrigger(node);
            String filter = functionFilter(node);
            if (filter != null) {
                Matcher matcher = PARAMETER_MATCHER.matcher(filter);
                while (matcher.find()) {
                    String paramName = matcher.group(1);

                    Optional<String> value = context.getApplicationProperty(paramName);
                    if (value.isPresent() && !value.get().isEmpty()) {
                        filter = filter.replaceAll("\\{" + paramName + "\\}", value.get());
                    } else {
                        throw new IllegalArgumentException("Missing argument declared in as function filter with name '"
                                + paramName + "'. Define it in application.properties file");
                    }
                }
            }
            annotator.withCloudEventMapping(flowSignalFunction, trigger, filter);
        }

        flowSignalFunction.getBody().get().findFirst(StringLiteralExpr.class, s -> s.getValue().equals("$TypePrefix$"))
                .ifPresent(s -> s.setValue(process.getPackageName() + "." + processId + fversion + "."));
        flowSignalFunction.getBody().get().findFirst(StringLiteralExpr.class, s -> s.getValue().equals("$ThisNode$"))
                .ifPresent(s -> s.setValue(methodName));

        flowSignalFunction.setName(methodName);

        return flowSignalFunction;
    }

}
