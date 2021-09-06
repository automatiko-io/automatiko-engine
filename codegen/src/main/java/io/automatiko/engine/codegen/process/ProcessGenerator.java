
package io.automatiko.engine.codegen.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.lang.model.SourceVersion;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;

import io.automatiko.engine.api.Functions;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.api.runtime.process.WorkItemHandler;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.EndOfInstanceStrategy;
import io.automatiko.engine.codegen.BodyDeclarationComparator;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.codegen.process.image.SvgProcessImageGenerator;
import io.automatiko.engine.services.execution.BaseFunctions;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.AbstractProcess;
import io.automatiko.engine.workflow.compiler.canonical.ProcessMetaData;
import io.automatiko.engine.workflow.compiler.canonical.TriggerMetaData;
import io.automatiko.engine.workflow.compiler.canonical.UserTaskModelMetaData;

/**
 * Generates the Process&lt;T&gt; container for a process, which encapsulates
 * its "executable model".
 *
 * @see io.automatiko.engine.api.workflow.Process
 */
public class ProcessGenerator {

    private static final String BUSINESS_KEY = "businessKey";
    private static final String CREATE_MODEL = "createModel";
    private static final String WPI = "wpi";

    private final GeneratorContext context;

    private final String packageName;
    private final WorkflowProcess process;
    private final ProcessExecutableModelGenerator processGenerator;
    private final String typeName;
    private final String modelTypeName;
    private final String generatedFilePath;
    private final String completePath;
    private final String targetCanonicalName;
    private final String appCanonicalName;
    private String targetTypeName;
    private DependencyInjectionAnnotator annotator;
    private boolean persistence;

    private String versionSuffix = "";

    private List<CompilationUnit> additionalClasses = new ArrayList<>();

    private List<UserTaskModelMetaData> userTasks;

    public ProcessGenerator(GeneratorContext context, WorkflowProcess process, ProcessExecutableModelGenerator processGenerator,
            String typeName,
            String modelTypeName, String appCanonicalName, List<UserTaskModelMetaData> userTasks) {
        this.context = context;
        this.appCanonicalName = appCanonicalName;

        this.packageName = process.getPackageName();
        this.process = process;
        this.processGenerator = processGenerator;
        this.typeName = typeName;
        this.modelTypeName = modelTypeName;
        this.targetTypeName = typeName + "Process";
        this.targetCanonicalName = packageName + "." + targetTypeName;
        this.generatedFilePath = targetCanonicalName.replace('.', '/') + ".java";
        this.completePath = "src/main/java/" + generatedFilePath;
        this.userTasks = userTasks;
        if (process.getVersion() != null && !process.getVersion().trim().isEmpty()) {
            this.versionSuffix = CodegenUtils.version(process.getVersion());
        }

        if (!SourceVersion.isName(targetTypeName)) {
            throw new IllegalArgumentException("Process id '" + typeName + "' is not valid");
        }

    }

    public String targetCanonicalName() {
        return targetCanonicalName;
    }

    public String targetTypeName() {
        return targetTypeName;
    }

    public String generate() {
        return compilationUnit().toString();
    }

    public CompilationUnit compilationUnit() {
        CompilationUnit compilationUnit = new CompilationUnit(packageName);
        compilationUnit.addImport("io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType");
        compilationUnit.addImport("io.automatiko.engine.workflow.process.executable.core.ExecutableProcessFactory");
        compilationUnit.addImport(new ImportDeclaration(BaseFunctions.class.getCanonicalName(), true, true));
        List<String> functions = context.getBuildContext().classThatImplement(Functions.class.getCanonicalName());
        functions.forEach(c -> compilationUnit.addImport(new ImportDeclaration(c, true, true)));

        compilationUnit.getTypes().add(classDeclaration());
        return compilationUnit;
    }

    private MethodDeclaration createInstanceMethod(String processInstanceFQCN) {
        MethodDeclaration methodDeclaration = new MethodDeclaration();

        ReturnStmt returnStmt = new ReturnStmt(new ObjectCreationExpr().setType(processInstanceFQCN)
                .setArguments(NodeList.nodeList(new ThisExpr(), new NameExpr("value"), createProcessRuntime())));

        methodDeclaration.setName("createInstance").addModifier(Modifier.Keyword.PUBLIC)
                .addParameter(modelTypeName, "value").setType(processInstanceFQCN)
                .setBody(new BlockStmt().addStatement(returnStmt));
        return methodDeclaration;
    }

    private MethodDeclaration createInstanceWithBusinessKeyMethod(String processInstanceFQCN) {
        MethodDeclaration methodDeclaration = new MethodDeclaration();

        ReturnStmt returnStmt = new ReturnStmt(
                new ObjectCreationExpr().setType(processInstanceFQCN).setArguments(NodeList.nodeList(new ThisExpr(),
                        new NameExpr("value"), new NameExpr(BUSINESS_KEY), createProcessRuntime())));

        methodDeclaration.setName("createInstance").addModifier(Modifier.Keyword.PUBLIC)
                .addParameter(String.class.getCanonicalName(), BUSINESS_KEY).addParameter(modelTypeName, "value")
                .setType(processInstanceFQCN).setBody(new BlockStmt().addStatement(returnStmt));
        return methodDeclaration;
    }

    private MethodDeclaration createInstanceGenericMethod(String processInstanceFQCN) {
        MethodDeclaration methodDeclaration = new MethodDeclaration();

        ReturnStmt returnStmt = new ReturnStmt(new MethodCallExpr(new ThisExpr(), "createInstance")
                .addArgument(new CastExpr(new ClassOrInterfaceType(null, modelTypeName), new NameExpr("value"))));

        methodDeclaration.setName("createInstance").addModifier(Modifier.Keyword.PUBLIC)
                .addParameter(Model.class.getCanonicalName(), "value").setType(processInstanceFQCN)
                .setBody(new BlockStmt().addStatement(returnStmt));
        return methodDeclaration;
    }

    private MethodDeclaration createInstanceGenericWithBusinessKeyMethod(String processInstanceFQCN) {
        MethodDeclaration methodDeclaration = new MethodDeclaration();

        ReturnStmt returnStmt = new ReturnStmt(new MethodCallExpr(new ThisExpr(), "createInstance")
                .addArgument(new NameExpr(BUSINESS_KEY))
                .addArgument(new CastExpr(new ClassOrInterfaceType(null, modelTypeName), new NameExpr("value"))));

        methodDeclaration.setName("createInstance").addModifier(Modifier.Keyword.PUBLIC)
                .addParameter(String.class.getCanonicalName(), BUSINESS_KEY)
                .addParameter(Model.class.getCanonicalName(), "value").setType(processInstanceFQCN)
                .setBody(new BlockStmt().addStatement(returnStmt));
        return methodDeclaration;
    }

    private MethodDeclaration createInstanceGenericWithWorkflowInstanceMethod(String processInstanceFQCN) {

        ReturnStmt returnStmt = new ReturnStmt(new ObjectCreationExpr().setType(processInstanceFQCN).setArguments(
                NodeList.nodeList(new ThisExpr(), new NameExpr("model"), createProcessRuntime(), new NameExpr(WPI),
                        new NameExpr("versionTrack"))));
        MethodDeclaration methodDeclaration = new MethodDeclaration();

        methodDeclaration.setName("createInstance").addModifier(Modifier.Keyword.PUBLIC)
                .addParameter(WorkflowProcessInstance.class.getCanonicalName(), "wpi")
                .addParameter(modelTypeName, "model")
                .addParameter(long.class, "versionTrack")
                .setType(processInstanceFQCN)
                .setBody(new BlockStmt()
                        .addStatement(returnStmt));
        return methodDeclaration;
    }

    private MethodDeclaration createReadOnlyInstanceGenericWithWorkflowInstanceMethod(String processInstanceFQCN) {

        ReturnStmt returnStmt = new ReturnStmt(new ObjectCreationExpr().setType(processInstanceFQCN)
                .setArguments(NodeList.nodeList(new ThisExpr(), new NameExpr("model"), new NameExpr(WPI))));

        MethodDeclaration methodDeclaration = new MethodDeclaration();

        methodDeclaration.setName("createReadOnlyInstance").addModifier(Modifier.Keyword.PUBLIC)
                .addParameter(WorkflowProcessInstance.class.getCanonicalName(), WPI)
                .addParameter(modelTypeName, "model")
                .setType(processInstanceFQCN)
                .setBody(new BlockStmt()
                        .addStatement(returnStmt));
        return methodDeclaration;
    }

    private MethodDeclaration process(ProcessMetaData processMetaData) {
        return processMetaData.getGeneratedClassModel().findFirst(MethodDeclaration.class)
                .orElseThrow(() -> new NoSuchElementException("Compilation unit doesn't contain a method declaration!"))
                .setModifiers(Modifier.Keyword.PUBLIC).setType(Process.class.getCanonicalName()).setName("buildProcess");
    }

    private MethodDeclaration userTaskInputModels(ProcessMetaData processMetaData) {
        ReturnStmt returnStmt = new ReturnStmt(new NullLiteralExpr());
        BlockStmt body = new BlockStmt();

        if (userTasks != null && !userTasks.isEmpty()) {
            body = new BlockStmt();
            for (UserTaskModelMetaData userTask : userTasks) {

                body.addStatement(new IfStmt(
                        new MethodCallExpr(new StringLiteralExpr(userTask.getTaskName()), "equals",
                                NodeList.nodeList(new NameExpr("taskName"))),
                        new ReturnStmt(
                                new MethodCallExpr(new NameExpr(userTask.getInputModelClassName()), new SimpleName("fromMap"))
                                        .addArgument(new NameExpr("taskId"))
                                        .addArgument(new NameExpr("taskName")).addArgument(new NameExpr("taskData"))),
                        null));
            }
        }
        body.addStatement(returnStmt);
        MethodDeclaration methodDeclaration = new MethodDeclaration();

        methodDeclaration.setName("taskInputs").addModifier(Modifier.Keyword.PUBLIC)
                .addParameter(String.class.getCanonicalName(), "taskId")
                .addParameter(String.class.getCanonicalName(), "taskName")
                .addParameter(Map.class.getCanonicalName(), "taskData")
                .setType(Object.class.getCanonicalName())
                .setBody(body);
        return methodDeclaration;
    }

    private MethodDeclaration userTaskOutputModels(ProcessMetaData processMetaData) {
        ReturnStmt returnStmt = new ReturnStmt(new NullLiteralExpr());
        BlockStmt body = new BlockStmt();

        if (userTasks != null && !userTasks.isEmpty()) {
            body = new BlockStmt();
            for (UserTaskModelMetaData userTask : userTasks) {

                body.addStatement(new IfStmt(
                        new MethodCallExpr(new StringLiteralExpr(userTask.getTaskName()), "equals",
                                NodeList.nodeList(new NameExpr("taskName"))),
                        new ReturnStmt(
                                new MethodCallExpr(new NameExpr(userTask.getOutputModelClassName()), new SimpleName("fromMap"))
                                        .addArgument(new NameExpr("taskId"))
                                        .addArgument(new NameExpr("taskName")).addArgument(new NameExpr("taskData"))),
                        null));
            }
        }
        body.addStatement(returnStmt);
        MethodDeclaration methodDeclaration = new MethodDeclaration();

        methodDeclaration.setName("taskOutputs").addModifier(Modifier.Keyword.PUBLIC)
                .addParameter(String.class.getCanonicalName(), "taskId")
                .addParameter(String.class.getCanonicalName(), "taskName")
                .addParameter(Map.class.getCanonicalName(), "taskData")
                .setType(Object.class.getCanonicalName())
                .setBody(body);
        return methodDeclaration;
    }

    private MethodCallExpr createProcessRuntime() {
        return new MethodCallExpr(new ThisExpr(), "createProcessRuntime");
    }

    private MethodDeclaration internalConfigure(ProcessMetaData processMetaData) {
        BlockStmt body = new BlockStmt();
        MethodDeclaration internalConfigure = new MethodDeclaration().setModifiers(Modifier.Keyword.PUBLIC)
                .setType(targetTypeName).setName("configure").setBody(body);

        // always call super.configure
        body.addStatement(new MethodCallExpr(new SuperExpr(), "configure"));

        if (!processMetaData.getGeneratedHandlers().isEmpty()) {

            processMetaData.getGeneratedHandlers().forEach((name, descriptor) -> {

                CompilationUnit handler = descriptor.generateHandlerClassForService();
                ClassOrInterfaceDeclaration clazz = handler.findFirst(ClassOrInterfaceDeclaration.class).get();
                if (useInjection()) {

                    boolean tracingAvailable = context.getBuildContext()
                            .hasClassAvailable("org.eclipse.microprofile.opentracing.Traced");

                    if (tracingAvailable) {

                        FieldDeclaration tracerField = new FieldDeclaration().addVariable(new VariableDeclarator(
                                new ClassOrInterfaceType(null, "io.automatiko.engine.service.tracing.TracingAdds"), "tracer"));
                        annotator.withInjection(tracerField);
                        clazz.addMember(tracerField);
                        clazz.findAll(MethodDeclaration.class).stream()
                                .filter(md -> md.getNameAsString().equals("executeWorkItem"))
                                .forEach(md -> {
                                    // add Traced nnotation on method level
                                    md.addAnnotation("org.eclipse.microprofile.opentracing.Traced");
                                    // next update method body to include extra tags
                                    BlockStmt mbody = md.getBody().get();
                                    MethodCallExpr tracer = new MethodCallExpr(new NameExpr("tracer"), "addTags")
                                            .addArgument(new MethodCallExpr(new NameExpr("workItem"), "getProcessInstance"));

                                    BlockStmt updatedBody = new BlockStmt();
                                    updatedBody.addStatement(tracer);

                                    mbody.getStatements().forEach(s -> updatedBody.addStatement(s));
                                    md.setBody(updatedBody);

                                });

                    }
                    annotator.withApplicationComponent(clazz);
                } else {

                    String packageName = handler.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
                    String clazzName = clazz.getName().toString();

                    MethodCallExpr workItemManager = new MethodCallExpr(new NameExpr("services"), "getWorkItemManager");
                    MethodCallExpr registerHandler = new MethodCallExpr(workItemManager, "registerWorkItemHandler")
                            .addArgument(new StringLiteralExpr(name))
                            .addArgument(new ObjectCreationExpr(null,
                                    new ClassOrInterfaceType(null, packageName + "." + clazzName),
                                    NodeList.nodeList()));

                    body.addStatement(registerHandler);
                }
                // annotate for injection or add constructor for initialization
                handler.findAll(FieldDeclaration.class, fd -> !fd.getVariable(0).getNameAsString().equals("tracer"))
                        .forEach(fd -> {
                            if (useInjection()) {
                                annotator.withInjection(fd);
                            }
                            if (descriptor.implementation().equalsIgnoreCase("##webservice") && annotator != null) {
                                annotator.withRestClientInjection(fd);

                            } else if (!descriptor.implementation().equalsIgnoreCase("##webservice")) {
                                BlockStmt constructorBody = new BlockStmt();
                                AssignExpr assignExpr = new AssignExpr(
                                        new FieldAccessExpr(new ThisExpr(), fd.getVariable(0).getNameAsString()),
                                        new ObjectCreationExpr().setType(fd.getVariable(0).getType().toString()),
                                        AssignExpr.Operator.ASSIGN);

                                constructorBody.addStatement(assignExpr);
                                clazz.addConstructor(Keyword.PUBLIC).setBody(constructorBody);
                            }
                        });

                additionalClasses.add(handler);
            });

            if (useInjection()) {

                BlockStmt actionBody = new BlockStmt();
                LambdaExpr forachBody = new LambdaExpr(new Parameter(new UnknownType(), "h"), actionBody);
                MethodCallExpr forachHandler = new MethodCallExpr(new NameExpr("handlers"), "forEach");
                forachHandler.addArgument(forachBody);

                MethodCallExpr workItemManager = new MethodCallExpr(new NameExpr("services"), "getWorkItemManager");
                MethodCallExpr registerHandler = new MethodCallExpr(workItemManager, "registerWorkItemHandler")
                        .addArgument(new MethodCallExpr(new NameExpr("h"), "getName"))
                        .addArgument(new NameExpr("h"));

                actionBody.addStatement(registerHandler);

                body.addStatement(forachHandler);
            }
        }
        if (!processMetaData.getGeneratedListeners().isEmpty()) {

            processMetaData.getGeneratedListeners().forEach(listener -> {

                ClassOrInterfaceDeclaration clazz = listener.findFirst(ClassOrInterfaceDeclaration.class).get();
                String packageName = listener.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
                String clazzName = clazz.getName().toString();

                MethodCallExpr eventSupport = new MethodCallExpr(new NameExpr("services"), "getEventSupport");
                MethodCallExpr registerListener = new MethodCallExpr(eventSupport, "addEventListener")
                        .addArgument(new ObjectCreationExpr(null,
                                new ClassOrInterfaceType(null, packageName + "." + clazzName), NodeList.nodeList()));

                body.addStatement(registerListener);

                additionalClasses.add(listener);
            });
        }

        body.addStatement(new ReturnStmt(new ThisExpr()));

        return internalConfigure;
    }

    private MethodDeclaration internalRegisterListeners(ProcessMetaData processMetaData) {
        BlockStmt body = new BlockStmt();
        MethodDeclaration internalRegisterListeners = new MethodDeclaration().setModifiers(Modifier.Keyword.PROTECTED)
                .setType(void.class).setName("registerListeners").setBody(body);

        if (!processMetaData.getSubProcesses().isEmpty()) {

            for (Entry<String, String> subProcess : processMetaData.getSubProcesses().entrySet()) {
                MethodCallExpr signalManager = new MethodCallExpr(new NameExpr("services"), "getSignalManager");
                MethodCallExpr registerListener = new MethodCallExpr(signalManager, "addEventListener")
                        .addArgument(new StringLiteralExpr(subProcess.getValue()))
                        .addArgument(new NameExpr("completionEventListener"));

                body.addStatement(registerListener);
            }
        }

        return internalRegisterListeners;
    }

    public static ClassOrInterfaceType processType(String canonicalName) {
        return new ClassOrInterfaceType(null, canonicalName + "Process");
    }

    public static ClassOrInterfaceType abstractProcessType(String canonicalName) {
        return new ClassOrInterfaceType(null, AbstractProcess.class.getCanonicalName())
                .setTypeArguments(new ClassOrInterfaceType(null, canonicalName));
    }

    public ClassOrInterfaceDeclaration classDeclaration() {
        ClassOrInterfaceDeclaration cls = new ClassOrInterfaceDeclaration().setName(targetTypeName)
                .setModifiers(Modifier.Keyword.PUBLIC);

        if (useInjection()) {
            annotator.withNamedApplicationComponent(cls, process.getId() + versionSuffix);

            FieldDeclaration handlersInjectFieldDeclaration = new FieldDeclaration().addVariable(new VariableDeclarator(
                    new ClassOrInterfaceType(null, new SimpleName(annotator.multiInstanceInjectionType()),
                            NodeList.nodeList(
                                    new ClassOrInterfaceType(null, WorkItemHandler.class.getCanonicalName()))),
                    "handlers"));

            cls.addMember(handlersInjectFieldDeclaration);
        }

        String processInstanceFQCN = ProcessInstanceGenerator.qualifiedName(packageName, typeName);

        FieldDeclaration fieldDeclaration = new FieldDeclaration()
                .addVariable(new VariableDeclarator(new ClassOrInterfaceType(null, appCanonicalName), "app"));

        ConstructorDeclaration emptyConstructorDeclaration = new ConstructorDeclaration().setName(targetTypeName)
                .addModifier(Modifier.Keyword.PUBLIC);

        ConstructorDeclaration baseConstructorDeclaration = new ConstructorDeclaration().setName(targetTypeName)
                .addModifier(Modifier.Keyword.PUBLIC).addParameter(appCanonicalName, "app")
                .setBody(new BlockStmt()
                        // super(module.config().process())
                        .addStatement(new MethodCallExpr(null, "super").addArgument(
                                new MethodCallExpr(new MethodCallExpr(new NameExpr("app"), "config"), "process")))
                        .addStatement(new AssignExpr(new FieldAccessExpr(new ThisExpr(), "app"), new NameExpr("app"),
                                AssignExpr.Operator.ASSIGN)));

        ConstructorDeclaration constructorDeclaration;
        if (useInjection()) {

            constructorDeclaration = new ConstructorDeclaration().setName(targetTypeName)
                    .addModifier(Modifier.Keyword.PUBLIC).addParameter(appCanonicalName, "app")
                    .addParameter(
                            new ClassOrInterfaceType(null, new SimpleName(annotator.multiInstanceInjectionType()),
                                    NodeList.nodeList(
                                            new ClassOrInterfaceType(null, WorkItemHandler.class.getCanonicalName()))),
                            "handlers")
                    .addParameter(EndOfInstanceStrategy.class.getCanonicalName(), "strategy")

                    .setBody(new BlockStmt()
                            // super(module.config().process())
                            .addStatement(new MethodCallExpr(null, "super").addArgument(
                                    new MethodCallExpr(new MethodCallExpr(new NameExpr("app"), "config"), "process")))
                            .addStatement(new AssignExpr(new FieldAccessExpr(new ThisExpr(), "app"), new NameExpr("app"),
                                    AssignExpr.Operator.ASSIGN))
                            .addStatement(
                                    new AssignExpr(new FieldAccessExpr(new ThisExpr(), "handlers"), new NameExpr("handlers"),
                                            AssignExpr.Operator.ASSIGN))
                            .addStatement(
                                    new AssignExpr(new FieldAccessExpr(new ThisExpr(), "endOfInstanceStrategy"),
                                            new NameExpr("strategy"),
                                            AssignExpr.Operator.ASSIGN)));

        } else {
            constructorDeclaration = new ConstructorDeclaration().setName(targetTypeName)
                    .addModifier(Modifier.Keyword.PUBLIC).addParameter(appCanonicalName, "app")
                    .addParameter(EndOfInstanceStrategy.class.getCanonicalName(), "strategy")

                    .setBody(new BlockStmt()
                            // super(module.config().process())
                            .addStatement(new MethodCallExpr(null, "super").addArgument(
                                    new MethodCallExpr(new MethodCallExpr(new NameExpr("app"), "config"), "process")))
                            .addStatement(new AssignExpr(new FieldAccessExpr(new ThisExpr(), "app"), new NameExpr("app"),
                                    AssignExpr.Operator.ASSIGN))
                            .addStatement(
                                    new AssignExpr(new FieldAccessExpr(new ThisExpr(), "endOfInstanceStrategy"),
                                            new NameExpr("strategy"),
                                            AssignExpr.Operator.ASSIGN)));

        }

        ProcessMetaData processMetaData = processGenerator.generate();

        if (!processMetaData.getSubProcesses().isEmpty()) {

            for (Entry<String, String> subProcess : processMetaData.getSubProcesses().entrySet()) {
                FieldDeclaration subprocessFieldDeclaration = new FieldDeclaration();

                String fieldName = "process" + subProcess.getKey();
                ClassOrInterfaceType modelType = new ClassOrInterfaceType(null,
                        new SimpleName(io.automatiko.engine.api.workflow.Process.class.getCanonicalName()),
                        NodeList.nodeList(
                                new ClassOrInterfaceType(null, StringUtils.capitalize(subProcess.getKey() + "Model"))));
                if (useInjection()) {
                    subprocessFieldDeclaration.addVariable(new VariableDeclarator(modelType, fieldName));

                    constructorDeclaration.addParameter(
                            annotator.withNamed(new Parameter(modelType, fieldName), subProcess.getKey()));

                    constructorDeclaration.getBody().addStatement(
                            new AssignExpr(new FieldAccessExpr(new ThisExpr(), fieldName), new NameExpr(fieldName),
                                    AssignExpr.Operator.ASSIGN));
                } else {
                    // app.processes().processById()
                    MethodCallExpr initSubProcessField = new MethodCallExpr(
                            new MethodCallExpr(new NameExpr("app"), "processes"), "processById")
                                    .addArgument(new StringLiteralExpr(subProcess.getKey()));

                    subprocessFieldDeclaration.addVariable(new VariableDeclarator(modelType, fieldName));

                    baseConstructorDeclaration.getBody()
                            .addStatement(new AssignExpr(new FieldAccessExpr(new ThisExpr(), fieldName),
                                    new CastExpr(modelType, initSubProcessField), Operator.ASSIGN));

                    constructorDeclaration.getBody()
                            .addStatement(new AssignExpr(new FieldAccessExpr(new ThisExpr(), fieldName),
                                    new CastExpr(modelType, initSubProcessField), Operator.ASSIGN));

                }

                cls.addMember(subprocessFieldDeclaration);
                subprocessFieldDeclaration.createGetter();
            }
        }

        if (useInjection()) {
            annotator.withInjection(constructorDeclaration);
        } else {

            emptyConstructorDeclaration.setBody(new BlockStmt().addStatement(
                    new MethodCallExpr(null, "this").addArgument(new ObjectCreationExpr().setType(appCanonicalName))));
        }

        MethodDeclaration createModelMethod = new MethodDeclaration().addModifier(Keyword.PUBLIC).setName("createModel")
                .setType(modelTypeName).setBody(new BlockStmt().addStatement(new ReturnStmt(new ObjectCreationExpr(null,
                        new ClassOrInterfaceType(null, modelTypeName), NodeList.nodeList()))));

        cls.addExtendedType(abstractProcessType(modelTypeName)).addMember(fieldDeclaration)
                .addMember(emptyConstructorDeclaration)
                .addMember(baseConstructorDeclaration)
                .addMember(constructorDeclaration)
                .addMember(createInstanceMethod(processInstanceFQCN))
                .addMember(createInstanceWithBusinessKeyMethod(processInstanceFQCN)).addMember(createModelMethod)
                .addMember(createInstanceGenericMethod(processInstanceFQCN))
                .addMember(createInstanceGenericWithBusinessKeyMethod(processInstanceFQCN))
                .addMember(createInstanceGenericWithWorkflowInstanceMethod(processInstanceFQCN))
                .addMember(createReadOnlyInstanceGenericWithWorkflowInstanceMethod(processInstanceFQCN))
                .addMember(internalConfigure(processMetaData)).addMember(internalRegisterListeners(processMetaData))
                .addMember(userTaskInputModels(processMetaData))
                .addMember(userTaskOutputModels(processMetaData))
                .addMember(process(processMetaData));

        if (isServiceProject()) {
            SvgProcessImageGenerator imageGenerator = new SvgProcessImageGenerator(process);
            String svg = imageGenerator.generate();

            if (svg != null && !svg.isEmpty()) {
                MethodDeclaration processImageMethod = new MethodDeclaration().setName("image").setModifiers(Keyword.PUBLIC)
                        .setType(String.class)
                        .setBody(new BlockStmt().addStatement(new ReturnStmt(new StringLiteralExpr().setString(svg))));

                cls.addMember(processImageMethod);
            }
        }

        if (useInjection()) {

            MethodDeclaration initMethod = annotator.withInitMethod(new MethodCallExpr(new ThisExpr(), "activate"));

            cls.addMember(initMethod);
        }

        if (!processMetaData.getTriggers().isEmpty()) {

            for (TriggerMetaData trigger : processMetaData.getTriggers()) {
                // add message produces as field
                if (trigger.getType().equals(TriggerMetaData.TriggerType.ProduceMessage)) {
                    String producerFieldType = packageName + "." + typeName + "MessageProducer_" + trigger.getOwnerId();
                    String producerFielName = "producer_" + trigger.getOwnerId();

                    FieldDeclaration producerFieldieldDeclaration = new FieldDeclaration()
                            .addVariable(new VariableDeclarator(new ClassOrInterfaceType(null, producerFieldType),
                                    producerFielName));
                    cls.addMember(producerFieldieldDeclaration);

                    if (useInjection()) {
                        annotator.withInjection(producerFieldieldDeclaration);
                    } else {

                        AssignExpr assignExpr = new AssignExpr(new FieldAccessExpr(new ThisExpr(), producerFielName),
                                new ObjectCreationExpr().setType(producerFieldType), AssignExpr.Operator.ASSIGN);

                        cls.getConstructors().forEach(c -> c.getBody().addStatement(assignExpr));

                    }
                }
            }
        }
        cls.getMembers().sort(new BodyDeclarationComparator());
        return cls;
    }

    public String generatedFilePath() {
        return generatedFilePath;
    }

    public boolean isPublic() {
        return WorkflowProcess.PUBLIC_VISIBILITY.equalsIgnoreCase(process.getVisibility());
    }

    public String processId() {
        return process.getId();
    }

    public String version() {
        return process.getVersion() == null ? "" : CodegenUtils.version(process.getVersion());
    }

    public List<CompilationUnit> getAdditionalClasses() {
        return additionalClasses;
    }

    public ProcessGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
        return this;
    }

    public ProcessGenerator withPersistence(boolean persistence) {
        this.persistence = persistence;
        return this;
    }

    protected boolean useInjection() {
        return this.annotator != null;
    }

    public boolean isServiceProject() {
        return context.getBuildContext().hasClassAvailable("javax.ws.rs.Path") || onClasspath("javax.ws.rs.Path");
    }

    protected boolean onClasspath(String clazz) {
        try {
            Class.forName(clazz, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
