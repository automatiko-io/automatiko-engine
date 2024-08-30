
package io.automatiko.engine.codegen.process;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatiko.engine.codegen.CodegenUtils.interpolateTypes;

import java.util.Collection;
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
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.BodyDeclarationComparator;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.codegen.ImportsOrganizer;
import io.automatiko.engine.codegen.context.ApplicationBuildContext;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.compiler.canonical.UserTaskModelMetaData;
import io.automatiko.engine.workflow.process.core.node.FaultNode;
import io.automatiko.engine.workflow.util.PatternConstants;

/**
 * AbstractResourceGenerator
 */
public abstract class AbstractResourceGenerator {

    public static final Pattern PARAMETER_MATCHER = Pattern.compile("\\{([\\S|\\p{javaWhitespace}&&[^\\}]]+)\\}",
            Pattern.DOTALL);
    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractResourceGenerator.class);

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
    private String parentProcessPrefix = "";
    private String parentProcessName = "process";
    private String parentProcessId = "id";
    private final String appCanonicalName;
    private DependencyInjectionAnnotator annotator;

    private String pathPrefix = "{id}";

    private boolean startable;
    private boolean dynamic;
    private List<UserTaskModelMetaData> userTasks;
    private Map<String, String> signals;
    private Map<String, Node> signalNodes;
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

        if (this.parentProcess != null) {
            this.parentProcessPrefix = parentProcess.getId() + CodegenUtils.version(parentProcess.getVersion());
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

    public AbstractResourceGenerator withSignals(Map<String, String> signals, Map<String, Node> signalNodes) {
        this.signals = signals;
        this.signalNodes = signalNodes;
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

        // path prefix can be provided via property in the application.properties or by process itself
        Optional<String> resourcePathPrefix = process.getMetaData().get("referencePrefix") != null
                ? Optional.of((String) process.getMetaData().get("referencePrefix"))
                : context.getApplicationProperty("quarkus.automatiko.resource-path-prefix");

        if (resourcePathPrefix.isPresent()) {
            ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Compilation unit doesn't contain a class or interface declaration!"));

            template.findAll(ClassOrInterfaceDeclaration.class, md -> md.getAnnotationByName("Path").isPresent())
                    .forEach(md -> {

                        AnnotationExpr pathAnotation = md.getAnnotationByName("Path").get();

                        String value = pathAnotation.asSingleMemberAnnotationExpr().getMemberValue()
                                .toStringLiteralExpr().get().getValue();

                        pathAnotation.asSingleMemberAnnotationExpr()
                                .setMemberValue(new StringLiteralExpr(resourcePathPrefix.get() + value));

                    });
        }

        Optional<String> resourcePathFormat = context.getApplicationProperty("quarkus.automatiko.resource-path-format");

        if (resourcePathFormat.isPresent()) {
            ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Compilation unit doesn't contain a class or interface declaration!"));

            template.findAll(ClassOrInterfaceDeclaration.class, md -> md.getAnnotationByName("Path").isPresent())
                    .forEach(md -> {

                        AnnotationExpr pathAnotation = md.getAnnotationByName("Path").get();

                        String value = pathAnotation.asSingleMemberAnnotationExpr().getMemberValue()
                                .toStringLiteralExpr().get().getValue();
                        value = Stream.of(value.split("/")).map(item -> {
                            if (resourcePathFormat.get().equals("dash")) {
                                return StringUtils.toDashCase(item);
                            } else if (resourcePathFormat.get().equals("camel")) {
                                return StringUtils.toCamelCase(item);
                            }
                            return item;
                        }).collect(Collectors.joining("/"));

                        pathAnotation.asSingleMemberAnnotationExpr()
                                .setMemberValue(new StringLiteralExpr(value));

                    });

            template.findAll(MethodDeclaration.class, md -> md.getAnnotationByName("Path").isPresent())
                    .forEach(md -> {

                        AnnotationExpr pathAnotation = md.getAnnotationByName("Path").get();

                        String value = pathAnotation.asSingleMemberAnnotationExpr().getMemberValue()
                                .toStringLiteralExpr().get().getValue();
                        value = Stream.of(value.split("/")).map(item -> {
                            if (item.startsWith("{") && item.endsWith("}")) {
                                return item;
                            }
                            if (resourcePathFormat.get().equals("dash")) {
                                return StringUtils.toDashCase(item);
                            } else if (resourcePathFormat.get().equals("camel")) {
                                return StringUtils.toCamelCase(item);
                            }
                            return item;
                        }).collect(Collectors.joining("/"));

                        pathAnotation.asSingleMemberAnnotationExpr()
                                .setMemberValue(new StringLiteralExpr(value));

                    });
        }

        ImportsOrganizer.organize(clazz);
        return clazz.toString();
    }

    public CompilationUnit generateCompilationUnit() {
        CompilationUnit clazz = parse(this.getClass().getResourceAsStream(getResourceTemplate()));
        clazz.setPackageDeclaration(process.getPackageName());
        clazz.addImport(modelfqcn);

        ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow(
                () -> new NoSuchElementException("Compilation unit doesn't contain a class or interface declaration!"));

        String category = (String) process.getMetaData().getOrDefault("category", process.getName());
        String categoryDescription = (String) process.getMetaData().getOrDefault("categoryDescription",
                process.getMetaData().getOrDefault("Documentation", processName).toString());

        template.addAnnotation(new NormalAnnotationExpr(new Name("org.eclipse.microprofile.openapi.annotations.tags.Tag"),
                NodeList.nodeList(new MemberValuePair("name", new StringLiteralExpr(category)),
                        new MemberValuePair("description", new StringLiteralExpr(categoryDescription)))));

        template.setName(resourceClazzName);
        AtomicInteger index = new AtomicInteger(0);
        AtomicInteger uindex = new AtomicInteger(0);
        // Generate signals endpoints
        Optional.ofNullable(signals).ifPresent(signalsMap -> {
            // using template class to the endpoints generation
            CompilationUnit signalClazz = parse(this.getClass().getResourceAsStream(getSignalResourceTemplate()));

            ClassOrInterfaceDeclaration signalTemplate = signalClazz.findFirst(ClassOrInterfaceDeclaration.class)
                    .orElseThrow(() -> new NoSuchElementException("SignalResourceTemplate class not found!"));

            signalsMap.entrySet().stream().filter(e -> Objects.nonNull(e.getKey())).forEach(entry -> {

                String signalName = entry.getKey();
                String signalType = entry.getValue();

                String methodName = sanitizeName(signalName) + "_" + index.getAndIncrement();

                signalTemplate.findAll(MethodDeclaration.class).forEach(md -> {
                    MethodDeclaration cloned = md.clone();
                    BlockStmt body = cloned.getBody().get();
                    if (signalType == null) {
                        body.findAll(NameExpr.class, nameExpr -> "data".equals(nameExpr.getNameAsString()))
                                .forEach(name -> name.replace(new NullLiteralExpr()));
                    }
                    MethodDeclaration signalMethod = new MethodDeclaration()
                            .setName(cloned.getNameAsString() + "_" + methodName)
                            .setPublic(true).setType(cloned.getType())
                            // Remove data parameter ( payload ) if signalType is null
                            .setParameters(signalType == null ? removeLastParam(cloned)
                                    : cloned.getParameters())
                            .setBody(body).setAnnotations(cloned.getAnnotations())
                            .setThrownExceptions(cloned.getThrownExceptions());

                    template.addMember(signalMethod);
                    if (signalNodes.containsKey(signalName)) {
                        Collection<FaultNode> errors = ProcessNodeLocator.findFaultNodes(process, signalNodes.get(signalName));
                        if (!errors.isEmpty()) {

                            // add error responses to complete task method based on errors found
                            addDefinedError(errors, signalMethod);
                        }
                    }
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
                String methodSuffix = sanitizeName(userTask.getName()) + "_" + sanitizeName(processId) + "_"
                        + uindex.getAndIncrement();

                Collection<FaultNode> errors = ProcessNodeLocator.findFaultNodes(process, userTask.getHumanTaskNode());

                userTaskTemplate.findAll(MethodDeclaration.class).forEach(md -> {

                    MethodDeclaration cloned = md.clone();
                    template.addMethod(cloned.getName() + "_" + methodSuffix, Keyword.PUBLIC).setType(cloned.getType())
                            .setParameters(cloned.getParameters()).setBody(cloned.getBody().get())
                            .setAnnotations(cloned.getAnnotations())
                            .setThrownExceptions(cloned.getThrownExceptions());

                    if (!errors.isEmpty() && cloned.getNameAsString().startsWith("completeTask")) {
                        // add error responses to complete task method based on errors found
                        addDefinedError(errors, cloned);
                    }
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
        template.findAll(ConstructorDeclaration.class).forEach(this::interpolateConstructor);
        template.findAll(FieldDeclaration.class).forEach(this::interpolateFields);
        template.findAll(NameExpr.class).forEach(this::interpolateVariables);
        template.findAll(MethodCallExpr.class).forEach(this::interpolateMethodCall);

        if (useInjection()) {

            template.findAll(FieldDeclaration.class, CodegenUtils::isProcessField).stream()
                    .filter(fd -> fd.getVariable(0).getNameAsString().startsWith("subprocess_"))
                    .forEach(fd -> annotator.withNamedInjection(fd, processId + version));
            //
            //            template.findAll(FieldDeclaration.class, CodegenUtils::isApplicationField)
            //                    .forEach(fd -> annotator.withInjection(fd));
            //            template.findAll(FieldDeclaration.class, CodegenUtils::isIdentitySupplierField)
            //                    .forEach(fd -> annotator.withInjection(fd));

            boolean tracingAvailable = context.getBuildContext().isTracingSupported();

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
            Optional<MethodDeclaration> createResourceMethod = template.findAll(MethodDeclaration.class).stream()
                    .filter(md -> md.getNameAsString().equals("create_" + processName)).findFirst();
            createResourceMethod.ifPresent(template::remove);
            Optional<MethodDeclaration> createResourceFormMethod = template.findAll(MethodDeclaration.class).stream()
                    .filter(md -> md.getNameAsString().equals("create_" + processName + "_form")).findFirst();
            createResourceFormMethod.ifPresent(template::remove);
        } else {
            Collection<FaultNode> errors = ProcessNodeLocator.findFaultNodes(process);
            if (!errors.isEmpty()) {
                Optional<MethodDeclaration> createResourceMethod = template.findAll(MethodDeclaration.class).stream()
                        .filter(md -> md.getNameAsString().equals("create_" + processName)).findFirst();
                // add error responses to complete task method based on errors found
                addDefinedError(errors, createResourceMethod.get());
            }
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
                                    .addAnnotation(new SingleMemberAnnotationExpr(new Name("jakarta.ws.rs.PathParam"),
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

            if (subunit.getPackageDeclaration().isPresent() && !subunit.getPackageDeclaration().get().getNameAsString()
                    .equals(clazz.getPackageDeclaration().get().getNameAsString())) {

                clazz.addImport(subunit.getPackageDeclaration().get().getNameAsString(), false, true);

            }

            subunit.getImports().stream().filter(imp -> imp.isAsterisk()).forEach(imp -> clazz.addImport(imp));
        }

        collectSubProcessModels(modelfqcn.substring(modelfqcn.lastIndexOf('.') + 1), template, subprocesses);

        enableValidation(template);
        securityAnnotated(template);
        try {
            template.getMembers().sort(new BodyDeclarationComparator());
        } catch (IllegalArgumentException e) {
            // unable to sort members of the class
        }

        return clazz;
    }

    protected void addDefinedError(Collection<FaultNode> errors, MethodDeclaration cloned) {
        NormalAnnotationExpr apiResponses = (NormalAnnotationExpr) cloned.getAnnotationByName("APIResponses")
                .orElse(null);

        if (apiResponses == null) {
            return;
        }

        MemberValuePair value = apiResponses.getPairs().stream()
                .filter(pair -> pair.getNameAsString().startsWith("value")).findFirst().orElse(null);

        if (value != null) {

            ArrayInitializerExpr responses = (ArrayInitializerExpr) value.getValue();

            for (FaultNode error : errors) {
                String responseCode = error.getFaultName();
                try {

                    int status = Integer.parseInt(error.getFaultName());
                    if (status < 100 || status > 999) {
                        // invalid http response code, fallbacks to 500
                        responseCode = "500";
                        LOGGER.warn(
                                "Invalid error code '{}' for service interface defined in process '{}' error name '{}' will be represented as 500",
                                error.getFaultName(), processId, error.getErrorName());
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warn(
                            "Invalid error code '{}' for service interface defined in process '{}' error name '{}' will be represented as 500",
                            error.getFaultName(), processId, error.getErrorName());
                    responseCode = "500";
                }

                NormalAnnotationExpr customError = new NormalAnnotationExpr(new Name("APIResponse"),
                        NodeList.nodeList(new MemberValuePair("responseCode",
                                new StringLiteralExpr(responseCode)),
                                new MemberValuePair("description",
                                        new StringLiteralExpr(
                                                "Process instance aborted due to defined error - '" + error.getErrorName()
                                                        + "'"))));

                if (error.getStructureRef() != null) {
                    NormalAnnotationExpr schemaExpr = new NormalAnnotationExpr(new Name("Schema"),
                            NodeList.nodeList(new MemberValuePair("implementation",
                                    new NameExpr(error.getStructureRef() + ".class"))));

                    NormalAnnotationExpr content = new NormalAnnotationExpr(new Name("Content"),
                            NodeList.nodeList(
                                    new MemberValuePair("mediaType", new StringLiteralExpr("application/json")),
                                    new MemberValuePair("schema", schemaExpr)));
                    customError.getPairs().add(new MemberValuePair("content", content));
                }

                responses.getValues().add(customError);
            }
        }
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

    public void collectSubProcessModels(String dataClassName, ClassOrInterfaceDeclaration template,
            List<AbstractResourceGenerator> subprocessGenerators) {

    }

    private void enableValidation(ClassOrInterfaceDeclaration template) {
        Optional.ofNullable(context).map(GeneratorContext::getBuildContext)
                .filter(ApplicationBuildContext::isValidationSupported)
                .ifPresent(c -> template.findAll(Parameter.class).stream()
                        .filter(param -> param.getTypeAsString().equals(dataClazzName + "Input"))
                        .forEach(this::insertValidationAnnotations));
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
        String interpolated = s.replace("$name$", processName).replace("$id$", processId).replace("$version$", version)
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
        String interpolated = methodName.asString().replace("$name$", processName).replace("$prefix$", pathPrefix).replace(
                "$parentprocessprefix$", parentProcessPrefix);
        m.setName(interpolated);

        m.getParameters().forEach(p -> p.setName(
                p.getNameAsString().replace("$name$", processName).replace("$prefix$", pathPrefix).replace(
                        "$parentprocess$", parentProcessName)));
    }

    private void interpolateConstructor(ConstructorDeclaration c) {
        SimpleName methodName = c.getName();
        String interpolated = methodName.asString().replace("$ResourceType$", resourceClazzName);
        c.setName(interpolated);

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
        return name.replaceAll("\\s", "_").replaceAll("\\.", "_").replaceAll("-", "_");
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

    public String processId() {
        return this.processId;
    }

    public String version() {
        return this.version;
    }

    public String generatorModelClass() {
        return this.dataClazzName;
    }
}