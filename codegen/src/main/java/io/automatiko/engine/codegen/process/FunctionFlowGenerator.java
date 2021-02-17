
package io.automatiko.engine.codegen.process;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatiko.engine.codegen.CodegenUtils.interpolateTypes;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.BodyDeclarationComparator;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.compiler.canonical.ProcessToExecModelGenerator;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.RuleSetNode;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;

public class FunctionFlowGenerator {
    private final String relativePath;

    private final GeneratorContext context;
    private WorkflowProcess process;
    private final String functionClazzName;
    private String processId;
    private final String processName;
    private String version = "";
    private String dataClazzName;
    private String modelfqcn;
    private final String appCanonicalName;
    private final String processClazzName;
    private DependencyInjectionAnnotator annotator;

    public FunctionFlowGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn,
            String processfqcn, String appCanonicalName) {
        this.context = context;
        this.process = process;
        this.processId = ProcessToExecModelGenerator.extractProcessId(process.getId(), null);
        this.processName = processId.substring(processId.lastIndexOf('.') + 1);
        if (process.getVersion() != null && !process.getVersion().trim().isEmpty()) {
            this.version = CodegenUtils.version(process.getVersion());
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

        // first to initiate the function flow

        template.findFirst(MethodDeclaration.class, md -> md.getNameAsString().equals("startTemplate")).ifPresent(md -> {
            md.setName(processId.toLowerCase());
            md.getBody().get().findFirst(StringLiteralExpr.class, s -> s.getValue().equals("$TypePrefix$"))
                    .ifPresent(s -> s.setValue(process.getPackageName() + "." + processId));

            if (useInjection()) {
                String trigger = (String) process.getMetaData().getOrDefault("functionType",
                        process.getPackageName() + "." + processId);
                annotator.withCloudEventMapping(md, trigger);
            }
        });

        MethodDeclaration callTemplate = template
                .findFirst(MethodDeclaration.class, md -> md.getNameAsString().equals("callTemplate")).get();

        // for each "execution" node add new function
        for (Node node : process.getNodesRecursively()) {

            if (isExecutionNode(node)) {
                MethodDeclaration flowStepFunction = callTemplate.clone();

                if (useInjection()) {
                    String trigger = (String) node.getMetaData().getOrDefault("functionType",
                            process.getPackageName() + "." + processId + "."
                                    + sanitizeIdentifier(node.getName()).toLowerCase());

                    annotator.withCloudEventMapping(flowStepFunction, trigger);
                }

                flowStepFunction.getBody().get().findFirst(StringLiteralExpr.class, s -> s.getValue().equals("$StartFromNode$"))
                        .ifPresent(s -> s.setValue((String) node.getMetaData().get("UniqueId")));
                flowStepFunction.getBody().get().findFirst(StringLiteralExpr.class, s -> s.getValue().equals("$TypePrefix$"))
                        .ifPresent(s -> s.setValue(process.getPackageName() + "." + processId + "."));
                flowStepFunction.getBody().get().findFirst(StringLiteralExpr.class, s -> s.getValue().equals("$ThisNode$"))
                        .ifPresent(s -> s.setValue(node.getName()));

                flowStepFunction.setName(sanitizeIdentifier(node.getName()).toLowerCase());

                template.addMember(flowStepFunction);
            }
        }
        // remove the template method
        callTemplate.removeForced();

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

        template.getMembers().sort(new BodyDeclarationComparator());
        return clazz.toString();
    }

    private boolean isExecutionNode(Node node) {
        if (node instanceof WorkItemNode || node instanceof ActionNode || node instanceof RuleSetNode
                || node instanceof SubProcessNode || node instanceof EventNode) {

            // ignore those that are attached to start node
            if (node.getIncomingConnections().values().stream().flatMap(c -> c.stream())
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
}
