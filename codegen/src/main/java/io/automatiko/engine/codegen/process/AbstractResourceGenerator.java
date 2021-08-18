
package io.automatiko.engine.codegen.process;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatiko.engine.codegen.CodegenUtils.interpolateTypes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.BodyDeclarationComparator;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.codegen.context.ApplicationBuildContext;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.compiler.canonical.UserTaskModelMetaData;
import io.automatiko.engine.workflow.util.PatternConstants;

/**
 * AbstractResourceGenerator
 */
public abstract class AbstractResourceGenerator {

    public static final Pattern PARAMETER_MATCHER = Pattern.compile("\\{([\\S|\\p{javaWhitespace}&&[^\\}]]+)\\}",
            Pattern.DOTALL);

    private final String relativePath;

    private final GeneratorContext context;
    private WorkflowProcess process;
    private WorkflowProcess parentProcess;
    private final String resourceClazzName;
    private final String processClazzName;
    private String processId;
    private String version = "";
    private String dataClazzName;
    private String modelfqcn;
    private final String processName;
    private String parentProcessName = "process";
    private String parentProcessId = "id";
    private final String appCanonicalName;
    private DependencyInjectionAnnotator annotator;

    private String pathPrefix = "{id}";

    private boolean startable;
    private boolean dynamic;
    private List<UserTaskModelMetaData> userTasks;
    private Map<String, String> signals;
    private List<AbstractResourceGenerator> subprocesses;

    private boolean persistence;

    public AbstractResourceGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn,
            String processfqcn, String appCanonicalName, String type) {
        this.context = context;
        this.process = process;
        this.processId = process.getId();
        this.processName = processId.substring(processId.lastIndexOf('.') + 1);
        if (process.getVersion() != null && !process.getVersion().trim().isEmpty()) {
            this.version = CodegenUtils.version(process.getVersion());
        }
        this.appCanonicalName = appCanonicalName;
        String classPrefix = StringUtils.capitalize(processName);
        this.resourceClazzName = classPrefix + type + version;
        this.relativePath = process.getPackageName().replace(".", "/") + "/" + resourceClazzName + ".java";
        this.modelfqcn = modelfqcn + "Output";
        this.dataClazzName = modelfqcn.substring(modelfqcn.lastIndexOf('.') + 1);
        this.processClazzName = processfqcn;

    }

    public AbstractResourceGenerator withParentProcess(WorkflowProcess parentProcess) {
        this.parentProcess = parentProcess;
        if (this.parentProcess != null && !isParentPublic()) {
            String processInfo = parentProcess.getId().substring(parentProcess.getId().lastIndexOf('.') + 1);

            this.parentProcessName = "subprocess_" + processInfo;
            this.parentProcessId = "id_" + processInfo;
        }
        return this;
    }

    public AbstractResourceGenerator withPersistence(boolean persistence) {
        this.persistence = persistence;
        return this;
    }

    public AbstractResourceGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
        return this;
    }

    public AbstractResourceGenerator withUserTasks(List<UserTaskModelMetaData> userTasks) {
        this.userTasks = userTasks;
        return this;
    }

    public AbstractResourceGenerator withSignals(Map<String, String> signals) {
        this.signals = signals;
        return this;
    }

    public AbstractResourceGenerator withTriggers(boolean startable, boolean dynamic) {
        this.startable = startable;
        this.dynamic = dynamic;
        return this;
    }

    public AbstractResourceGenerator withSubProcesses(List<AbstractResourceGenerator> subprocesses) {
        this.subprocesses = subprocesses;
        return this;
    }

    public AbstractResourceGenerator withPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
        return this;
    }

    public String className() {
        return resourceClazzName;
    }

    protected abstract String getResourceTemplate();

    public String generate() {

        CompilationUnit clazz = generateCompilationUnit();

        if (version != null && !version.trim().isEmpty()) {
            ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Compilation unit doesn't contain a class or interface declaration!"));

            template.findAll(ClassOrInterfaceDeclaration.class, md -> md.getAnnotationByName("Path").isPresent())
                    .forEach(md -> {

                        AnnotationExpr pathAnotation = md.getAnnotationByName("Path").get();

                        String value = pathAnotation.asSingleMemberAnnotationExpr().getMemberValue()
                                .toStringLiteralExpr().get().getValue();

                        pathAnotation.asSingleMemberAnnotationExpr()
                                .setMemberValue(new StringLiteralExpr(version.replaceFirst("_", "/v") + value));

                    });
        }

        return clazz.toString();
    }

    public CompilationUnit generateCompilationUnit() {
        CompilationUnit clazz = parse(this.getClass().getResourceAsStream(getResourceTemplate()));
        clazz.setPackageDeclaration(process.getPackageName());
        clazz.addImport(modelfqcn);

        ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow(
                () -> new NoSuchElementException("Compilation unit doesn't contain a class or interface declaration!"));

        template.setName(resourceClazzName);
        AtomicInteger index = new AtomicInteger(0);
        // Generate signals endpoints
        Optional.ofNullable(signals).ifPresent(signalsMap -> {
            // using template class to the endpoints generation
            CompilationUnit signalClazz = parse(this.getClass().getResourceAsStream(getSignalResourceTemplate()));

            ClassOrInterfaceDeclaration signalTemplate = signalClazz.findFirst(ClassOrInterfaceDeclaration.class)
                    .orElseThrow(() -> new NoSuchElementException("SignalResourceTemplate class not found!"));

            signalsMap.entrySet().stream().filter(e -> Objects.nonNull(e.getKey())).forEach(entry -> {
                String methodName = "signal_" + index.getAndIncrement();

                String signalName = entry.getKey();
                String signalType = entry.getValue();

                signalTemplate.findAll(MethodDeclaration.class).forEach(md -> {
                    MethodDeclaration cloned = md.clone();
                    BlockStmt body = cloned.getBody().get();
                    if (signalType == null) {
                        body.findAll(NameExpr.class, nameExpr -> "data".equals(nameExpr.getNameAsString()))
                                .forEach(name -> name.replace(new NullLiteralExpr()));
                    }
                    template.addMethod(methodName, Keyword.PUBLIC).setType("javax.ws.rs.core.Response")
                            // Remove data parameter ( payload ) if signalType is null
                            .setParameters(signalType == null ? removeLastParam(cloned)
                                    : cloned.getParameters())
                            .setBody(body).setAnnotations(cloned.getAnnotations());
                });

                if (signalType != null) {
                    template.findAll(ClassOrInterfaceType.class).forEach(name -> {
                        String identifier = name.getNameAsString();
                        name.setName(identifier.replace("$signalType$", signalType));
                    });
                }

                template.findAll(StringLiteralExpr.class).forEach(vv -> {
                    String s = vv.getValue();
                    String interpolated = s.replace("$signalName$", signalName);
                    interpolated = interpolated.replace("$signalPath$", sanitizeName(signalName));
                    vv.setString(interpolated);
                });
            });
        });

        if (userTasks != null) {

            CompilationUnit userTaskClazz = parse(this.getClass().getResourceAsStream(getUserTaskResourceTemplate()));

            ClassOrInterfaceDeclaration userTaskTemplate = userTaskClazz.findFirst(ClassOrInterfaceDeclaration.class)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Compilation unit doesn't contain a class or interface declaration!"));
            for (UserTaskModelMetaData userTask : userTasks) {
                String methodSuffix = sanitizeName(userTask.getName()) + "_" + index.getAndIncrement();
                userTaskTemplate.findAll(MethodDeclaration.class).forEach(md -> {

                    MethodDeclaration cloned = md.clone();
                    template.addMethod(cloned.getName() + "_" + methodSuffix, Keyword.PUBLIC).setType(cloned.getType())
                            .setParameters(cloned.getParameters()).setBody(cloned.getBody().get())
                            .setAnnotations(cloned.getAnnotations());
                });

                template.findAll(StringLiteralExpr.class).forEach(s -> interpolateUserTaskStrings(s, userTask));
                template.findAll(ClassOrInterfaceType.class).forEach(c -> interpolateUserTaskTypes(c,
                        userTask.getInputModelClassSimpleName(), userTask.getOutputModelClassSimpleName()));
                template.findAll(NameExpr.class).forEach(c -> interpolateUserTaskNameExp(c, userTask));
                if (!userTask.isAdHoc()) {
                    template.findAll(MethodDeclaration.class).stream()
                            .filter(md -> md.getNameAsString().equals("signal_" + methodSuffix))
                            .collect(Collectors.toList()).forEach(template::remove);
                }
            }
        }

        template.findAll(StringLiteralExpr.class).forEach(this::interpolateStrings);
        Map<String, String> typeInterpolations = new HashMap<>();
        typeInterpolations.put("$Clazz$", resourceClazzName);
        typeInterpolations.put("$Type$", dataClazzName);
        template.findAll(ClassOrInterfaceType.class).forEach(cls -> interpolateTypes(cls, typeInterpolations));
        template.findAll(MethodDeclaration.class).forEach(this::interpolateMethods);
        template.findAll(FieldDeclaration.class).forEach(this::interpolateFields);
        template.findAll(NameExpr.class).forEach(this::interpolateVariables);
        template.findAll(MethodCallExpr.class).forEach(this::interpolateMethodCall);

        if (useInjection()) {

            template.findAll(FieldDeclaration.class, CodegenUtils::isProcessField)
                    .forEach(fd -> annotator.withNamedInjection(fd, processId + version));

            template.findAll(FieldDeclaration.class, CodegenUtils::isApplicationField)
                    .forEach(fd -> annotator.withInjection(fd));
            template.findAll(FieldDeclaration.class, CodegenUtils::isIdentitySupplierField)
                    .forEach(fd -> annotator.withInjection(fd));

            boolean tracingAvailable = context.getBuildContext()
                    .hasClassAvailable("org.eclipse.microprofile.opentracing.Traced");

            if (tracingAvailable) {

                FieldDeclaration tracerField = new FieldDeclaration().addVariable(new VariableDeclarator(
                        new ClassOrInterfaceType(null, "io.automatiko.engine.service.tracing.TracingAdds"), "tracer"));
                annotator.withInjection(tracerField);
                template.addMember(tracerField);

                template.findAll(MethodDeclaration.class, md -> md.getNameAsString().equals("tracing")).forEach(md -> {
                    BlockStmt body = new BlockStmt();
                    body.addStatement(
                            new MethodCallExpr(new NameExpr("tracer"), "addTags").addArgument(new NameExpr("intance")));
                    md.setBody(body);
                });
            }
        } else {
            template.findAll(FieldDeclaration.class, CodegenUtils::isProcessField)
                    .forEach(this::initializeProcessField);

            template.findAll(FieldDeclaration.class, CodegenUtils::isApplicationField)
                    .forEach(this::initializeApplicationField);
        }

        // if triggers are not empty remove createResource method as there is another
        // trigger to start process instances
        if (!startable || !isPublic()) {
            Optional<MethodDeclaration> createResourceMethod = template.findFirst(MethodDeclaration.class)
                    .filter(md -> md.getNameAsString().equals("create_" + processName));
            createResourceMethod.ifPresent(template::remove);
        }

        if (useInjection()) {
            annotator.withApplicationComponent(template);
        }

        for (AbstractResourceGenerator resourceGenerator : subprocesses) {

            resourceGenerator.withPathPrefix(
                    parentProcess == null ? pathPrefix : pathPrefix + "/" + processId + "/{id_" + processId + "}");
            CompilationUnit subunit = resourceGenerator.generateCompilationUnit();

            subunit.findFirst(ClassOrInterfaceDeclaration.class).get().findAll(MethodDeclaration.class).forEach(md -> {
                MethodDeclaration cloned = md.clone();

                interpolateMethodParams(cloned);

                Optional<AnnotationExpr> pathAnotation = cloned.getAnnotationByName("Path");
                if (pathAnotation.isPresent()) {
                    String v = pathAnotation.get().toString().replaceAll("\\{id", "#{id");
                    Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(v);
                    while (matcher.find()) {
                        String paramName = matcher.group(1);

                        if (cloned.getParameterByName(paramName).isEmpty()) {
                            cloned.addParameter(new Parameter().setName(paramName).setType(String.class)
                                    .addAnnotation(new SingleMemberAnnotationExpr(new Name("javax.ws.rs.PathParam"),
                                            new StringLiteralExpr(paramName))));

                        }
                    }
                }

                cloned.getParameters().sort(new Comparator<Parameter>() {

                    @Override
                    public int compare(Parameter o1, Parameter o2) {
                        if (o1.getAnnotations().isEmpty() && o1.getAnnotations().isEmpty()) {
                            return 0;
                        } else if (o1.getAnnotations().isEmpty() && !o1.getAnnotations().isEmpty()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });
                template.addMember(cloned);
            });

            subunit.findFirst(ClassOrInterfaceDeclaration.class).get().findAll(FieldDeclaration.class).forEach(fd -> {
                FieldDeclaration cloned = fd.clone();
                template.addMember(cloned);
            });
        }

        enableValidation(template);
        removeMetricsIfNotEnabled(template);
        securityAnnotated(template);

        template.getMembers().sort(new BodyDeclarationComparator());

        return clazz;
    }

    protected abstract String getSignalResourceTemplate();

    public abstract String getUserTaskResourceTemplate();

    private void securityAnnotated(ClassOrInterfaceDeclaration template) {
        if (useInjection() && process.getMetaData().containsKey("securityRoles")) {
            String[] roles = ((String) process.getMetaData().get("securityRoles")).split(",");
            template.findAll(MethodDeclaration.class).stream().filter(this::requiresSecurity)
                    .forEach(md -> annotator.withSecurityRoles(md, roles));
        }
    }

    private boolean requiresSecurity(MethodDeclaration md) {
        // applies to only rest annotated methods
        return getRestAnnotations().stream().map(md::getAnnotationByName).anyMatch(Optional::isPresent);
    }

    public abstract List<String> getRestAnnotations();

    private void enableValidation(ClassOrInterfaceDeclaration template) {
        Optional.ofNullable(context).map(GeneratorContext::getBuildContext)
                .filter(ApplicationBuildContext::isValidationSupported)
                .ifPresent(c -> template.findAll(Parameter.class).stream()
                        .filter(param -> param.getTypeAsString().equals(dataClazzName + "Input"))
                        .forEach(this::insertValidationAnnotations));
    }

    private void removeMetricsIfNotEnabled(ClassOrInterfaceDeclaration template) {
        Optional.ofNullable(context).map(GeneratorContext::getBuildContext)
                .filter(bc -> !bc.config().metrics().enabled())
                .ifPresent(c -> template.findAll(MethodDeclaration.class).stream().forEach(md -> {
                    md.getAnnotationByName("Counted").ifPresent(a -> a.remove());
                    md.getAnnotationByName("Metered").ifPresent(a -> a.remove());
                    md.getAnnotationByName("Timed").ifPresent(a -> a.remove());
                }));
    }

    private void insertValidationAnnotations(Parameter param) {
        param.addAnnotation("javax.validation.Valid");
        param.addAnnotation("javax.validation.constraints.NotNull");
    }

    private void initializeProcessField(FieldDeclaration fd) {
        fd.getVariable(0).setInitializer(new ObjectCreationExpr().setType(processClazzName));
    }

    private void initializeApplicationField(FieldDeclaration fd) {
        fd.getVariable(0).setInitializer(new ObjectCreationExpr().setType(appCanonicalName));
    }

    private void interpolateStrings(StringLiteralExpr vv) {
        String s = vv.getValue();
        String documentation = process.getMetaData().getOrDefault("Documentation", processName).toString();
        String interpolated = s.replace("$name$", processName).replace("$id$", processId)
                .replace("$processdocumentation$", documentation).replace("$prefix$", pathPrefix)
                .replace("$processname$", process.getName());
        vv.setString(interpolated);
    }

    private void interpolateUserTaskStrings(StringLiteralExpr vv, UserTaskModelMetaData userTask) {
        String s = vv.getValue();
        String interpolated = s.replace("$taskName$", sanitizeName(userTask.getName()));
        interpolated = interpolated.replace("$taskNodeName$", userTask.getNodeName());
        vv.setString(interpolated);
    }

    private void interpolateUserTaskNameExp(NameExpr name, UserTaskModelMetaData userTask) {
        String identifier = name.getNameAsString();

        name.setName(identifier.replace("$TaskInput$", userTask.getInputModelClassSimpleName()));

        identifier = name.getNameAsString();
        name.setName(identifier.replace("$TaskOutput$", userTask.getOutputModelClassSimpleName()));
    }

    private void interpolateMethods(MethodDeclaration m) {
        SimpleName methodName = m.getName();
        String interpolated = methodName.asString().replace("$name$", processName).replace("$prefix$", pathPrefix);
        m.setName(interpolated);

        m.getParameters().forEach(p -> p.setName(
                p.getNameAsString().replace("$name$", processName).replace("$prefix$", pathPrefix).replace("$parentprocessid$",
                        parentProcessId)));
    }

    private void interpolateMethodParams(MethodDeclaration m) {

        m.findAll(MethodCallExpr.class, mc -> mc.getNameAsString().equals("findById")).forEach(mc -> {

            mc.findAll(NameExpr.class).forEach(t -> {
                String value = "";

                if (parentProcess != null) {
                    value = parentProcessId + "+ \":\" + ";
                }

                SimpleName methodName = t.getName();
                String interpolated = methodName.asString().replace("$parentparentprocessid$", value);
                t.setName(interpolated);
            });
        });
    }

    private void interpolateFields(FieldDeclaration m) {
        SimpleName methodName = m.getVariable(0).getName();
        String interpolated = methodName.asString().replace("$name$", processName);
        m.getVariable(0).setName(interpolated);
    }

    private void interpolateVariables(NameExpr m) {
        SimpleName methodName = m.getName();
        String interpolated = methodName.asString().replace("$name$", processName)
                .replace("$parentprocess$", parentProcessName).replace("$parentprocessid$", parentProcessId);
        m.setName(interpolated);
    }

    private void interpolateMethodCall(MethodCallExpr m) {
        SimpleName methodName = m.getName();
        String interpolated = methodName.asString().replace("$name$", processName);
        m.setName(interpolated);
    }

    private void interpolateUserTaskTypes(ClassOrInterfaceType t, String inputClazzName, String outputClazzName) {
        try {
            SimpleName returnType = t.asClassOrInterfaceType().getName();
            interpolateUserTaskTypes(returnType, inputClazzName, outputClazzName);
            t.getTypeArguments().ifPresent(o -> interpolateUserTaskTypeArguments(o, inputClazzName, outputClazzName));
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private void interpolateUserTaskTypes(SimpleName returnType, String inputClazzName, String outputClazzName) {
        String identifier = returnType.getIdentifier();

        returnType.setIdentifier(identifier.replace("$TaskInput$", inputClazzName));

        identifier = returnType.getIdentifier();
        returnType.setIdentifier(identifier.replace("$TaskOutput$", outputClazzName));
    }

    private void interpolateUserTaskTypeArguments(NodeList<Type> ta, String inputClazzName, String outputClazzName) {
        ta.stream().map(Type::asClassOrInterfaceType)
                .forEach(t -> interpolateUserTaskTypes(t, inputClazzName, outputClazzName));
    }

    private String sanitizeName(String name) {
        return name.replaceAll("\\s", "_");
    }

    public String generatedFilePath() {
        return relativePath;
    }

    protected boolean useInjection() {
        return this.annotator != null;
    }

    protected boolean isPublic() {
        return WorkflowProcess.PUBLIC_VISIBILITY.equalsIgnoreCase(process.getVisibility());
    }

    protected boolean isParentPublic() {
        return WorkflowProcess.PUBLIC_VISIBILITY.equalsIgnoreCase(parentProcess.getVisibility());
    }

    protected NodeList<Parameter> removeLastParam(MethodDeclaration cloned) {
        cloned.getParameters().remove(cloned.getParameters().size() - 1);

        return cloned.getParameters();
    }
}