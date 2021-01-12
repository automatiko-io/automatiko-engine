
package io.automatiko.engine.codegen.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.codegen.BodyDeclarationComparator;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.compiler.canonical.ModelMetaData;
import io.automatiko.engine.workflow.compiler.canonical.ProcessMetaData;

public class ProcessInstanceGenerator {

    private static final String PROCESS = "process";
    private static final String VALUE = "value";
    private static final String PROCESS_RUNTIME = "processRuntime";
    private static final String BUSINESS_KEY = "businessKey";
    private static final String WPI = "wpi";

    private final String packageName;
    private final String typeName;
    private final ModelMetaData model;
    private final String canonicalName;
    private final String targetTypeName;
    private final String targetCanonicalName;
    private final String generatedFilePath;
    private final String completePath;

    private final ProcessExecutableModelGenerator processGenerator;

    private final GeneratorContext generatorContext;

    public static String qualifiedName(String packageName, String typeName) {
        return packageName + "." + typeName + "ProcessInstance";
    }

    public ProcessInstanceGenerator(GeneratorContext generatorContext, ProcessExecutableModelGenerator processGenerator,
            String packageName,
            String typeName, ModelMetaData model) {
        this.generatorContext = generatorContext;
        this.processGenerator = processGenerator;
        this.packageName = packageName;
        this.typeName = typeName;
        this.model = model;
        this.canonicalName = packageName + "." + typeName;
        this.targetTypeName = typeName + "ProcessInstance";
        this.targetCanonicalName = packageName + "." + targetTypeName;
        this.generatedFilePath = targetCanonicalName.replace('.', '/') + ".java";
        this.completePath = "src/main/java/" + generatedFilePath;
    }

    public String generate() {
        return compilationUnit().toString();
    }

    public CompilationUnit compilationUnit() {
        CompilationUnit compilationUnit = new CompilationUnit(packageName);
        compilationUnit.getTypes().add(classDeclaration());
        return compilationUnit;
    }

    public ClassOrInterfaceDeclaration classDeclaration() {
        ClassOrInterfaceDeclaration classDecl = new ClassOrInterfaceDeclaration().setName(targetTypeName)
                .addModifier(Modifier.Keyword.PUBLIC);
        classDecl
                .addExtendedType(new ClassOrInterfaceType(null, AbstractProcessInstance.class.getCanonicalName())
                        .setTypeArguments(new ClassOrInterfaceType(null, model.getModelClassSimpleName())))
                .addMember(constructorDecl()).addMember(constructorWithBusinessKeyDecl())
                .addMember(constructorWithWorkflowInstanceAndRuntimeDecl()).addMember(constructorWorkflowInstanceDecl())
                .addMember(bind()).addMember(unbind());

        if (generatorContext.getApplicationProperty("quarkus.automatiko.instance-locking").orElse("true")
                .equalsIgnoreCase("false")) {
            MethodDeclaration configureLock = new MethodDeclaration().setModifiers(Keyword.PROTECTED).setName("configureLock")
                    .addParameter(String.class.getCanonicalName(), "businessKey")
                    .setType(new VoidType());
            classDecl.addMember(configureLock);
        }

        ProcessMetaData processMetaData = processGenerator.generate();
        if (!processMetaData.getSubProcesses().isEmpty()) {
            classDecl.getMembers().add(subprocessesMethod(processMetaData));
        }

        classDecl.getMembers().sort(new BodyDeclarationComparator());
        return classDecl;
    }

    private MethodDeclaration bind() {
        String modelName = model.getModelClassSimpleName();
        BlockStmt body = new BlockStmt().addStatement(new ReturnStmt(model.toMap("variables")));
        return new MethodDeclaration().setModifiers(Modifier.Keyword.PROTECTED).setName("bind")
                .addParameter(modelName, "variables")
                .setType(new ClassOrInterfaceType().setName("java.util.Map").setTypeArguments(
                        new ClassOrInterfaceType().setName("String"), new ClassOrInterfaceType().setName("Object")))
                .setBody(body);

    }

    private MethodDeclaration unbind() {
        String modelName = model.getModelClassSimpleName();
        BlockStmt body = new BlockStmt().addStatement(model.fromMap("variables", "vmap"));

        return new MethodDeclaration().setModifiers(Modifier.Keyword.PROTECTED).setName("unbind")
                .setType(new VoidType()).addParameter(modelName, "variables")
                .addParameter(new ClassOrInterfaceType().setName("java.util.Map").setTypeArguments(
                        new ClassOrInterfaceType().setName("String"), new ClassOrInterfaceType().setName("Object")),
                        "vmap")
                .setBody(body);
    }

    private ConstructorDeclaration constructorDecl() {
        return new ConstructorDeclaration().setName(targetTypeName).addModifier(Modifier.Keyword.PUBLIC)
                .addParameter(ProcessGenerator.processType(canonicalName), PROCESS)
                .addParameter(model.getModelClassSimpleName(), VALUE)
                .addParameter(ProcessRuntime.class.getCanonicalName(), PROCESS_RUNTIME)
                .setBody(new BlockStmt().addStatement(new MethodCallExpr("super", new NameExpr(PROCESS),
                        new NameExpr(VALUE), new NameExpr(PROCESS_RUNTIME))));
    }

    private ConstructorDeclaration constructorWithBusinessKeyDecl() {
        return new ConstructorDeclaration().setName(targetTypeName).addModifier(Modifier.Keyword.PUBLIC)
                .addParameter(ProcessGenerator.processType(canonicalName), PROCESS)
                .addParameter(model.getModelClassSimpleName(), VALUE)
                .addParameter(String.class.getCanonicalName(), BUSINESS_KEY)
                .addParameter(ProcessRuntime.class.getCanonicalName(), PROCESS_RUNTIME)
                .setBody(new BlockStmt().addStatement(new MethodCallExpr("super", new NameExpr(PROCESS),
                        new NameExpr(VALUE), new NameExpr(BUSINESS_KEY), new NameExpr(PROCESS_RUNTIME))));
    }

    private ConstructorDeclaration constructorWithWorkflowInstanceAndRuntimeDecl() {
        return new ConstructorDeclaration().setName(targetTypeName).addModifier(Modifier.Keyword.PUBLIC)
                .addParameter(ProcessGenerator.processType(canonicalName), PROCESS)
                .addParameter(model.getModelClassSimpleName(), VALUE)
                .addParameter(ProcessRuntime.class.getCanonicalName(), PROCESS_RUNTIME)
                .addParameter(WorkflowProcessInstance.class.getCanonicalName(), WPI)
                .setBody(new BlockStmt().addStatement(new MethodCallExpr("super", new NameExpr(PROCESS),
                        new NameExpr(VALUE), new NameExpr(PROCESS_RUNTIME), new NameExpr(WPI))));
    }

    private ConstructorDeclaration constructorWorkflowInstanceDecl() {
        return new ConstructorDeclaration().setName(targetTypeName).addModifier(Modifier.Keyword.PUBLIC)
                .addParameter(ProcessGenerator.processType(canonicalName), PROCESS)
                .addParameter(model.getModelClassSimpleName(), VALUE)
                .addParameter(WorkflowProcessInstance.class.getCanonicalName(), WPI)
                .setBody(new BlockStmt().addStatement(
                        new MethodCallExpr("super", new NameExpr(PROCESS), new NameExpr(VALUE), new NameExpr(WPI))));
    }

    private MethodDeclaration subprocessesMethod(ProcessMetaData processMetaData) {

        ClassOrInterfaceType collectionOfprocessInstanceType = new ClassOrInterfaceType(null,
                new SimpleName(Collection.class.getCanonicalName()),
                NodeList.nodeList(new ClassOrInterfaceType(null,
                        new SimpleName(ProcessInstance.class.getCanonicalName()), NodeList.nodeList(
                                new WildcardType(new ClassOrInterfaceType(null, Model.class.getCanonicalName()))))));

        ClassOrInterfaceType listOfprocessInstanceType = new ClassOrInterfaceType(null,
                new SimpleName(ArrayList.class.getCanonicalName()),
                NodeList.nodeList(new ClassOrInterfaceType(null,
                        new SimpleName(ProcessInstance.class.getCanonicalName()), NodeList.nodeList(
                                new WildcardType(new ClassOrInterfaceType(null, Model.class.getCanonicalName()))))));

        MethodDeclaration subprocessesMethod = new MethodDeclaration().setName("subprocesses")
                .setModifiers(Keyword.PUBLIC).setType(collectionOfprocessInstanceType);
        BlockStmt body = new BlockStmt();

        VariableDeclarationExpr subprocessFieldDeclaration = new VariableDeclarationExpr(
                new VariableDeclarator().setType(collectionOfprocessInstanceType).setName("subprocesses")
                        .setInitializer(new ObjectCreationExpr(null, listOfprocessInstanceType, NodeList.nodeList())));
        body.addStatement(subprocessFieldDeclaration);
        for (Entry<String, String> subProcess : processMetaData.getSubProcesses().entrySet()) {

            String getterName = "getProcess" + subProcess.getKey();
            // this.process().get....
            MethodCallExpr fetchProcessInstance = new MethodCallExpr(
                    new EnclosedExpr(new CastExpr(ProcessGenerator.processType(canonicalName),
                            new MethodCallExpr(new ThisExpr(), "process"))),
                    getterName);

            body.addStatement(new MethodCallExpr(new ThisExpr(), "populateChildProcesses")
                    .addArgument(fetchProcessInstance).addArgument(new NameExpr("subprocesses")));

        }
        body.addStatement(new ReturnStmt(new NameExpr("subprocesses")));
        subprocessesMethod.setBody(body);
        return subprocessesMethod;
    }

    public String targetTypeName() {
        return targetTypeName;
    }

    public String generatedFilePath() {
        return generatedFilePath;
    }
}
