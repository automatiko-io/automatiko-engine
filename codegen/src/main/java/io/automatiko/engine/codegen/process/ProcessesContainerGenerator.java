
package io.automatiko.engine.codegen.process;

import static com.github.javaparser.StaticJavaParser.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.WildcardType;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Processes;
import io.automatiko.engine.codegen.AbstractApplicationSection;
import io.automatiko.engine.codegen.BodyDeclarationComparator;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;

public class ProcessesContainerGenerator extends AbstractApplicationSection {

    private static final String RESOURCE = "/class-templates/ProcessesTemplate.java";
    private final String packageName;
    private final List<ProcessGenerator> processes;
    private final List<BodyDeclaration<?>> factoryMethods;

    private DependencyInjectionAnnotator annotator;

    private NodeList<BodyDeclaration<?>> applicationDeclarations;
    private MethodDeclaration byProcessIdMethodDeclaration;
    private MethodDeclaration processesMethodDeclaration;

    public ProcessesContainerGenerator(String packageName) {
        super("Processes", "processes", Processes.class);
        this.packageName = packageName;
        this.processes = new ArrayList<>();
        this.factoryMethods = new ArrayList<>();
        this.applicationDeclarations = new NodeList<>();

        byProcessIdMethodDeclaration = new MethodDeclaration().addModifier(Modifier.Keyword.PUBLIC)
                .setName("processById")
                .setType(new ClassOrInterfaceType(null,
                        io.automatiko.engine.api.workflow.Process.class.getCanonicalName()).setTypeArguments(
                                new WildcardType(new ClassOrInterfaceType(null, Model.class.getCanonicalName()))))
                .setBody(new BlockStmt()).addParameter("String", "processId");

        processesMethodDeclaration = new MethodDeclaration().addModifier(Modifier.Keyword.PUBLIC).setName("processIds")
                .setType(new ClassOrInterfaceType(null, Collection.class.getCanonicalName())
                        .setTypeArguments(new ClassOrInterfaceType(null, "String")))
                .setBody(new BlockStmt());

        applicationDeclarations.add(byProcessIdMethodDeclaration);
        applicationDeclarations.add(processesMethodDeclaration);
    }

    public List<BodyDeclaration<?>> factoryMethods() {
        return factoryMethods;
    }

    public void addProcess(ProcessGenerator p) {
        processes.add(p);
        addProcessToApplication(p);
    }

    public void addProcessToApplication(ProcessGenerator r) {
        ObjectCreationExpr newProcess = new ObjectCreationExpr().setType(r.targetCanonicalName())
                .addArgument("application");
        IfStmt byProcessId = new IfStmt(
                new MethodCallExpr(new StringLiteralExpr(r.processId() + r.version()), "equals",
                        NodeList.nodeList(new NameExpr("processId"))),
                new ReturnStmt(new MethodCallExpr(newProcess, "configure")), null);

        byProcessIdMethodDeclaration.getBody()
                .orElseThrow(() -> new NoSuchElementException("A method declaration doesn't contain a body!"))
                .addStatement(byProcessId);
    }

    public ProcessesContainerGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
        return this;
    }

    @Override
    public ClassOrInterfaceDeclaration classDeclaration() {
        byProcessIdMethodDeclaration.getBody()
                .orElseThrow(() -> new NoSuchElementException("A method declaration doesn't contain a body!"))
                .addStatement(new ReturnStmt(new NullLiteralExpr()));

        NodeList<Expression> processIds = NodeList.nodeList(
                processes.stream().map(p -> new StringLiteralExpr(p.processId())).collect(Collectors.toList()));
        processesMethodDeclaration.getBody()
                .orElseThrow(() -> new NoSuchElementException("A method declaration doesn't contain a body!"))
                .addStatement(new ReturnStmt(
                        new MethodCallExpr(new NameExpr(Arrays.class.getCanonicalName()), "asList", processIds)));

        FieldDeclaration applicationFieldDeclaration = new FieldDeclaration();
        applicationFieldDeclaration
                .addVariable(new VariableDeclarator(new ClassOrInterfaceType(null, "Application"), "application"))
                .setModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
        applicationDeclarations.add(applicationFieldDeclaration);

        ConstructorDeclaration constructorDeclaration = new ConstructorDeclaration("Processes")
                .addModifier(Modifier.Keyword.PUBLIC).addParameter("Application", "application")
                .setBody(new BlockStmt().addStatement("this.application = application;"));
        applicationDeclarations.add(constructorDeclaration);

        ClassOrInterfaceDeclaration cls = super.classDeclaration().setMembers(applicationDeclarations);
        cls.getMembers().sort(new BodyDeclarationComparator());

        return cls;
    }

    @Override
    public CompilationUnit injectableClass() {
        CompilationUnit compilationUnit = parse(this.getClass().getResourceAsStream(RESOURCE))
                .setPackageDeclaration(packageName);
        ClassOrInterfaceDeclaration cls = compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow(
                () -> new NoSuchElementException("Compilation unit doesn't contain a class or interface declaration!"));

        cls.findAll(FieldDeclaration.class, fd -> fd.getVariable(0).getNameAsString().equals("processes"))
                .forEach(fd -> {
                    annotator.withInjection(fd);
                    fd.getVariable(0).setType(new ClassOrInterfaceType(null,
                            new SimpleName(annotator.multiInstanceInjectionType()),
                            NodeList.nodeList(new ClassOrInterfaceType(null,
                                    new SimpleName(io.automatiko.engine.api.workflow.Process.class.getCanonicalName()),
                                    NodeList.nodeList(new WildcardType())))));
                });

        annotator.withApplicationComponent(cls);

        return compilationUnit;
    }
}
