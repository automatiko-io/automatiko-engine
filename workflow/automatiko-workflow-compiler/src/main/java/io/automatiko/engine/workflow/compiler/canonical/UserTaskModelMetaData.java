package io.automatiko.engine.workflow.compiler.canonical;

import static com.github.javaparser.StaticJavaParser.parse;
import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static io.automatiko.engine.workflow.compiler.util.ClassUtils.constructClass;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.CUSTOM_AUTO_START;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.DATA_INPUTS;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.DATA_OUTPUTS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.UserTask;
import io.automatiko.engine.api.UserTaskParam;
import io.automatiko.engine.api.UserTaskParam.ParamType;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.process.core.node.HumanTaskNode;
import io.automatiko.engine.workflow.util.PatternConstants;

public class UserTaskModelMetaData {

    private static final String TASK_INTPUT_CLASS_SUFFIX = "TaskInput";
    private static final String TASK_OUTTPUT_CLASS_SUFFIX = "TaskOutput";
    private static final String TASK_NAME = "TaskName";

    protected static final List<String> INTERNAL_FIELDS = Arrays.asList(TASK_NAME, "Description", "NodeName", "ActorId",
            "GroupId", "EmailSubject", "FormName",
            "Priority", "Comment", "Skippable", "Content", "Locale", "Groups", "ExcludedUsers", "ExcludedOwnerId");

    private final String packageName;

    private final VariableScope processVariableScope;
    private final VariableScope variableScope;
    private final HumanTaskNode humanTaskNode;
    private final String processId;
    private final String version;

    private String inputModelClassName;
    private String inputModelClassSimpleName;

    private String outputModelClassName;
    private String outputModelClassSimpleName;

    private boolean templateSupported;

    public UserTaskModelMetaData(String packageName, VariableScope processVariableScope, VariableScope variableScope,
            HumanTaskNode humanTaskNode, String processId, String version, boolean templateSupported) {
        this.packageName = packageName;
        this.processVariableScope = processVariableScope;
        this.variableScope = variableScope;
        this.humanTaskNode = humanTaskNode;
        this.processId = processId;
        this.version = version;
        this.templateSupported = templateSupported;

        this.inputModelClassSimpleName = StringUtils
                .capitalize(ProcessToExecModelGenerator.extractProcessId(processId, version) + "_"
                        + humanTaskNode.getId() + "_" + TASK_INTPUT_CLASS_SUFFIX);
        this.inputModelClassName = packageName + '.' + inputModelClassSimpleName;

        this.outputModelClassSimpleName = StringUtils
                .capitalize(ProcessToExecModelGenerator.extractProcessId(processId, version) + "_"
                        + humanTaskNode.getId() + "_" + TASK_OUTTPUT_CLASS_SUFFIX);
        this.outputModelClassName = packageName + '.' + outputModelClassSimpleName;

    }

    public String generateInput() {
        CompilationUnit modelClass = compilationUnitInput();
        return modelClass.toString();
    }

    public String generateOutput() {
        CompilationUnit modelClass = compilationUnitOutput();
        return modelClass.toString();
    }

    public String getInputModelClassName() {
        return inputModelClassName;
    }

    public void setInputModelClassName(String inputModelClassName) {
        this.inputModelClassName = inputModelClassName;
    }

    public String getInputModelClassSimpleName() {
        return inputModelClassSimpleName;
    }

    public void setInputModelClassSimpleName(String inputModelClassSimpleName) {
        this.inputModelClassSimpleName = inputModelClassSimpleName;
    }

    public String getOutputModelClassName() {
        return outputModelClassName;
    }

    public void setOutputModelClassName(String outputModelClassName) {
        this.outputModelClassName = outputModelClassName;
    }

    public String getOutputModelClassSimpleName() {
        return outputModelClassSimpleName;
    }

    public void setOutputModelClassSimpleName(String outputModelClassSimpleName) {
        this.outputModelClassSimpleName = outputModelClassSimpleName;
    }

    public String getName() {
        return (String) humanTaskNode.getWork().getParameters().getOrDefault(TASK_NAME, humanTaskNode.getName());
    }

    public String getNodeName() {
        return humanTaskNode.getName();
    }

    public String getTaskName() {
        return (String) humanTaskNode.getWork().getParameters().getOrDefault("TaskName",
                humanTaskNode.getName());
    }

    public long getId() {
        return humanTaskNode.getId();
    }

    public HumanTaskNode getHumanTaskNode() {
        return humanTaskNode;
    }

    private void addUserTaskAnnotation(ClassOrInterfaceDeclaration modelClass) {
        String taskName = (String) humanTaskNode.getWork().getParameter(TASK_NAME);
        if (taskName == null)
            taskName = humanTaskNode.getName();
        modelClass.addAndGetAnnotation(UserTask.class).addPair("taskName", new StringLiteralExpr(taskName))
                .addPair("processName", new StringLiteralExpr(
                        StringUtils.capitalize(ProcessToExecModelGenerator.extractProcessId(processId, version))));
    }

    private void addUserTaskParamAnnotation(FieldDeclaration fd, UserTaskParam.ParamType paramType) {
        fd.tryAddImportToParentCompilationUnit(ParamType.class);
        fd.addAndGetAnnotation(UserTaskParam.class).addPair("value", ParamType.class.getSimpleName() + '.' + paramType);
    }

    private CompilationUnit compilationUnitInput() {
        // task input handling
        CompilationUnit compilationUnit = parse(
                this.getClass().getResourceAsStream("/class-templates/TaskInputTemplate.java"));
        compilationUnit.setPackageDeclaration(packageName);
        Optional<ClassOrInterfaceDeclaration> processMethod = compilationUnit
                .findFirst(ClassOrInterfaceDeclaration.class, sl1 -> true);

        if (!processMethod.isPresent()) {
            throw new RuntimeException("Cannot find class declaration in the template");
        }
        ClassOrInterfaceDeclaration modelClass = processMethod.get();
        compilationUnit.addOrphanComment(new LineComment(
                "Task input model for user task '" + humanTaskNode.getName() + "' in process '" + processId + "'"));

        addUserTaskAnnotation(modelClass);

        modelClass.setName(inputModelClassSimpleName);

        if (templateSupported) {
            modelClass.addAnnotation("io.quarkus.qute.TemplateData");
        }

        modelClass.addAnnotation(new NormalAnnotationExpr(new Name("org.eclipse.microprofile.openapi.annotations.media.Schema"),
                NodeList.nodeList(
                        new MemberValuePair("name",
                                new StringLiteralExpr(("Input model for " + humanTaskNode.getName()).replaceAll("\\s", ""))),
                        new MemberValuePair("description", new StringLiteralExpr(
                                "Task input model for user task '" + humanTaskNode.getName() + "' in '" + processId
                                        + "'")))));

        // setup of static fromMap method body
        ClassOrInterfaceType modelType = new ClassOrInterfaceType(null, modelClass.getNameAsString());
        BlockStmt staticFromMap = new BlockStmt();
        VariableDeclarationExpr itemField = new VariableDeclarationExpr(modelType, "item");
        staticFromMap.addStatement(new AssignExpr(itemField,
                new ObjectCreationExpr(null, modelType, NodeList.nodeList()), AssignExpr.Operator.ASSIGN));
        NameExpr item = new NameExpr("item");
        for (Entry<String, String> entry : humanTaskNode.getInMappings().entrySet()) {

            Variable variable = Optional.ofNullable(variableScope.findVariable(entry.getValue()))
                    .orElse(processVariableScope.findVariable(entry.getValue()));

            if (variable == null) {
                throw new IllegalStateException("Task " + humanTaskNode.getName() + " (input) " + entry.getKey()
                        + " reference not existing variable " + entry.getValue());
            }

            FieldDeclaration fd = new FieldDeclaration().addVariable(
                    new VariableDeclarator().setType(variable.getType().getStringType()).setName(entry.getKey()))
                    .addModifier(Modifier.Keyword.PRIVATE);

            modelClass.addMember(fd);

            addUserTaskParamAnnotation(fd, UserTaskParam.ParamType.INPUT);

            fd.createGetter();
            fd.createSetter();

            // fromMap static method body
            FieldAccessExpr field = new FieldAccessExpr(item, entry.getKey());

            ClassOrInterfaceType type = parseClassOrInterfaceType(variable.getType().getStringType());
            staticFromMap
                    .addStatement(new AssignExpr(field,
                            new CastExpr(type,
                                    new MethodCallExpr(new NameExpr("params"), "get")
                                            .addArgument(new StringLiteralExpr(entry.getKey()))),
                            AssignExpr.Operator.ASSIGN));
        }

        for (Entry<String, Object> entry : humanTaskNode.getWork().getParameters().entrySet()) {

            if (entry.getValue() == null || INTERNAL_FIELDS.contains(entry.getKey())) {
                continue;
            }

            Map<String, String> dataInputs = (Map<String, String>) humanTaskNode.getMetaData(DATA_INPUTS);
            FieldDeclaration fd = new FieldDeclaration().addVariable(new VariableDeclarator()
                    .setType(dataInputs.get(entry.getKey())).setName(entry.getKey()))
                    .addModifier(Modifier.Keyword.PUBLIC);
            modelClass.addMember(fd);
            addUserTaskParamAnnotation(fd, UserTaskParam.ParamType.INPUT);

            fd.createGetter();
            fd.createSetter();

            // fromMap static method body
            FieldAccessExpr field = new FieldAccessExpr(item, entry.getKey());

            ClassOrInterfaceType type = parseClassOrInterfaceType(fd.getVariable(0).getType().asString());
            staticFromMap
                    .addStatement(new AssignExpr(field,
                            new CastExpr(type,
                                    new MethodCallExpr(new NameExpr("params"), "get")
                                            .addArgument(new StringLiteralExpr(entry.getKey()))),
                            AssignExpr.Operator.ASSIGN));
        }
        Optional<MethodDeclaration> staticFromMapMethod = modelClass.findFirst(MethodDeclaration.class,
                sl -> sl.getName().asString().equals("fromMap") && sl.isStatic());
        if (staticFromMapMethod.isPresent()) {
            MethodDeclaration fromMap = staticFromMapMethod.get();
            fromMap.setType(modelClass.getNameAsString());
            staticFromMap.addStatement(new ReturnStmt(new NameExpr("item")));
            fromMap.setBody(staticFromMap);
        }

        // setup of the toMap method body
        BlockStmt toMapBody = new BlockStmt();
        ClassOrInterfaceType toMap = new ClassOrInterfaceType(null, new SimpleName(Map.class.getSimpleName()),
                NodeList.nodeList(new ClassOrInterfaceType(null, String.class.getSimpleName()),
                        new ClassOrInterfaceType(null, Object.class.getSimpleName())));
        VariableDeclarationExpr paramsField = new VariableDeclarationExpr(toMap, "params");
        toMapBody
                .addStatement(new AssignExpr(
                        paramsField, new ObjectCreationExpr(null,
                                new ClassOrInterfaceType(null, HashMap.class.getSimpleName()), NodeList.nodeList()),
                        AssignExpr.Operator.ASSIGN));

        for (Entry<String, String> entry : humanTaskNode.getInMappings().entrySet()) {
            if (entry.getValue() == null || INTERNAL_FIELDS.contains(entry.getKey())) {
                continue;
            }

            Variable variable = Optional.ofNullable(variableScope.findVariable(entry.getValue()))
                    .orElse(processVariableScope.findVariable(entry.getValue()));

            if (variable == null) {
                // check if given mapping is an expression
                Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(entry.getValue());
                if (matcher.find()) {
                    Map<String, String> dataOutputs = (Map<String, String>) humanTaskNode.getMetaData(DATA_INPUTS);
                    variable = new Variable();
                    variable.setName(entry.getKey());
                    variable.setType(new ObjectDataType(constructClass(dataOutputs.get(entry.getKey())),
                            dataOutputs.get(entry.getKey())));
                } else {
                    throw new IllegalStateException("Task " + humanTaskNode.getName() + " (output) " + entry.getKey()
                            + " reference not existing variable " + entry.getValue());
                }
            }

            // toMap method body
            MethodCallExpr putVariable = new MethodCallExpr(new NameExpr("params"), "put");
            putVariable.addArgument(new StringLiteralExpr(entry.getKey()));
            putVariable.addArgument(new FieldAccessExpr(new ThisExpr(), entry.getKey()));
            toMapBody.addStatement(putVariable);
        }
        Optional<MethodDeclaration> toMapMethod = modelClass.findFirst(MethodDeclaration.class,
                sl -> sl.getName().asString().equals("toMap"));

        toMapBody.addStatement(new ReturnStmt(new NameExpr("params")));
        toMapMethod.ifPresent(methodDeclaration -> methodDeclaration.setBody(toMapBody));
        return compilationUnit;
    }

    @SuppressWarnings({ "unchecked" })
    private CompilationUnit compilationUnitOutput() {
        CompilationUnit compilationUnit = parse(
                this.getClass().getResourceAsStream("/class-templates/TaskOutputTemplate.java"));
        compilationUnit.setPackageDeclaration(packageName);
        Optional<ClassOrInterfaceDeclaration> processMethod = compilationUnit
                .findFirst(ClassOrInterfaceDeclaration.class, sl1 -> true);

        if (!processMethod.isPresent()) {
            throw new RuntimeException("Cannot find class declaration in the template");
        }
        ClassOrInterfaceDeclaration modelClass = processMethod.get();
        compilationUnit.addOrphanComment(new LineComment(
                "Task output model for user task '" + humanTaskNode.getName() + "' in process '" + processId + "'"));
        addUserTaskAnnotation(modelClass);
        modelClass.setName(outputModelClassSimpleName);
        if (templateSupported) {
            modelClass.addAnnotation("io.quarkus.qute.TemplateData");
        }
        modelClass.addAnnotation(new NormalAnnotationExpr(new Name("org.eclipse.microprofile.openapi.annotations.media.Schema"),
                NodeList.nodeList(
                        new MemberValuePair("name",
                                new StringLiteralExpr(("Output model for " + humanTaskNode.getName()).replaceAll("\\s", ""))),
                        new MemberValuePair("description", new StringLiteralExpr(
                                "Task output model for user task '" + humanTaskNode.getName() + "' in '" + processId
                                        + "'")))));

        // setup of static fromMap method body
        ClassOrInterfaceType modelType = new ClassOrInterfaceType(null, modelClass.getNameAsString());
        BlockStmt staticFromMap = new BlockStmt();
        VariableDeclarationExpr itemField = new VariableDeclarationExpr(modelType, "item");
        staticFromMap.addStatement(new AssignExpr(itemField,
                new ObjectCreationExpr(null, modelType, NodeList.nodeList()), AssignExpr.Operator.ASSIGN));
        NameExpr item = new NameExpr("item");
        for (Entry<String, String> entry : humanTaskNode.getOutMappings().entrySet()) {
            if (entry.getValue() == null || INTERNAL_FIELDS.contains(entry.getKey())) {
                continue;
            }
            Variable variable = Optional.ofNullable(variableScope.findVariable(entry.getValue()))
                    .orElse(processVariableScope.findVariable(entry.getValue()));

            if (variable == null) {
                // check if given mapping is an expression
                Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(entry.getValue());
                if (matcher.find()) {
                    Map<String, String> dataOutputs = (Map<String, String>) humanTaskNode.getMetaData(DATA_OUTPUTS);
                    variable = new Variable();
                    variable.setName(entry.getKey());
                    variable.setType(new ObjectDataType(constructClass(dataOutputs.get(entry.getKey())),
                            dataOutputs.get(entry.getKey())));
                } else {

                    throw new IllegalStateException("Task " + humanTaskNode.getName() + " (output) " + entry.getKey()
                            + " reference not existing variable " + entry.getValue());
                }
            }

            // fromMap static method body
            FieldAccessExpr field = new FieldAccessExpr(item, entry.getKey());

            ClassOrInterfaceType type = parseClassOrInterfaceType(variable.getType().getStringType());
            staticFromMap
                    .addStatement(new AssignExpr(field,
                            new CastExpr(type,
                                    new MethodCallExpr(new NameExpr("params"), "get")
                                            .addArgument(new StringLiteralExpr(entry.getKey()))),
                            AssignExpr.Operator.ASSIGN));
        }
        Optional<MethodDeclaration> staticFromMapMethod = modelClass.findFirst(MethodDeclaration.class,
                sl -> sl.getName().asString().equals("fromMap") && sl.isStatic());
        if (staticFromMapMethod.isPresent()) {
            MethodDeclaration fromMap = staticFromMapMethod.get();
            fromMap.setType(modelClass.getNameAsString());
            staticFromMap.addStatement(new ReturnStmt(new NameExpr("item")));
            fromMap.setBody(staticFromMap);
        }

        // setup of the toMap method body
        BlockStmt toMapBody = new BlockStmt();
        ClassOrInterfaceType toMap = new ClassOrInterfaceType(null, new SimpleName(Map.class.getSimpleName()),
                NodeList.nodeList(new ClassOrInterfaceType(null, String.class.getSimpleName()),
                        new ClassOrInterfaceType(null, Object.class.getSimpleName())));
        VariableDeclarationExpr paramsField = new VariableDeclarationExpr(toMap, "params");
        toMapBody
                .addStatement(new AssignExpr(
                        paramsField, new ObjectCreationExpr(null,
                                new ClassOrInterfaceType(null, HashMap.class.getSimpleName()), NodeList.nodeList()),
                        AssignExpr.Operator.ASSIGN));

        for (Entry<String, String> entry : humanTaskNode.getOutMappings().entrySet()) {
            if (entry.getValue() == null || INTERNAL_FIELDS.contains(entry.getKey())) {
                continue;
            }

            Variable variable = Optional.ofNullable(variableScope.findVariable(entry.getValue()))
                    .orElse(processVariableScope.findVariable(entry.getValue()));

            if (variable == null) {
                // check if given mapping is an expression
                Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(entry.getValue());
                if (matcher.find()) {
                    Map<String, String> dataOutputs = (Map<String, String>) humanTaskNode.getMetaData(DATA_OUTPUTS);
                    variable = new Variable();
                    variable.setName(entry.getKey());
                    variable.setType(new ObjectDataType(constructClass(dataOutputs.get(entry.getKey())),
                            dataOutputs.get(entry.getKey())));
                } else {
                    throw new IllegalStateException("Task " + humanTaskNode.getName() + " (output) " + entry.getKey()
                            + " reference not existing variable " + entry.getValue());
                }
            }

            FieldDeclaration fd = new FieldDeclaration().addVariable(
                    new VariableDeclarator().setType(variable.getType().getStringType()).setName(entry.getKey()))
                    .addModifier(Modifier.Keyword.PRIVATE);
            modelClass.addMember(fd);
            addUserTaskParamAnnotation(fd, UserTaskParam.ParamType.OUTPUT);

            fd.createGetter();
            fd.createSetter();

            // toMap method body
            MethodCallExpr putVariable = new MethodCallExpr(new NameExpr("params"), "put");
            putVariable.addArgument(new StringLiteralExpr(entry.getKey()));
            putVariable.addArgument(new FieldAccessExpr(new ThisExpr(), entry.getKey()));
            toMapBody.addStatement(putVariable);
        }

        Optional<MethodDeclaration> toMapMethod = modelClass.findFirst(MethodDeclaration.class,
                sl -> sl.getName().asString().equals("toMap"));

        toMapBody.addStatement(new ReturnStmt(new NameExpr("params")));
        toMapMethod.ifPresent(methodDeclaration -> methodDeclaration.setBody(toMapBody));
        return compilationUnit;
    }

    public boolean isAdHoc() {
        return !Boolean.parseBoolean((String) humanTaskNode.getMetaData(CUSTOM_AUTO_START))
                && (humanTaskNode.getIncomingConnections() == null || humanTaskNode.getIncomingConnections().isEmpty());
    }

}
