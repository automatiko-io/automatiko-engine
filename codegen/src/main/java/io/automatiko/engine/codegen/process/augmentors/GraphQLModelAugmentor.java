package io.automatiko.engine.codegen.process.augmentors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.codegen.process.OutputModelClassGenerator;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.compiler.canonical.ProcessMetaData;

public class GraphQLModelAugmentor implements Consumer<CompilationUnit> {

    private boolean input;
    private ProcessMetaData metadata;

    private GeneratorContext context;

    public GraphQLModelAugmentor(boolean input, ProcessMetaData metadata, GeneratorContext context) {
        this.input = input;
        this.metadata = metadata;

        this.context = context;
    }

    @Override
    public void accept(CompilationUnit unit) {

        Optional<ClassOrInterfaceDeclaration> mainClass = unit.findFirst(ClassOrInterfaceDeclaration.class);

        if (!mainClass.isPresent()) {
            throw new NoSuchElementException("Cannot find class declaration in the template");
        }
        ClassOrInterfaceDeclaration modelClass = mainClass.get();

        if (input) {
            modelClass.addAnnotation("org.eclipse.microprofile.graphql.Input");
        }
        modelClass.getImplementedTypes().remove(0);

        if (!input) {

            if (metadata.getSubProcesses() != null) {

                for (Entry<String, String> entry : metadata.getSubProcesses().entrySet()) {

                    OutputModelClassGenerator outputModelClassGenerator = (OutputModelClassGenerator) context
                            .getGenerator("OutputModelClassGenerator", entry.getValue());

                    if (outputModelClassGenerator == null) {
                        throw new IllegalStateException("Unable to find model class for process '" + entry.getKey()
                                + "', most likely does not match on process id and version");
                    }

                    FieldDeclaration subprocessModelField = new FieldDeclaration().addVariable(new VariableDeclarator(
                            new ClassOrInterfaceType(null, new SimpleName(List.class.getCanonicalName()),
                                    NodeList.nodeList(new ClassOrInterfaceType(null, outputModelClassGenerator.className()))),
                            entry.getKey(),
                            new ObjectCreationExpr(
                                    null, new ClassOrInterfaceType(null, new SimpleName(ArrayList.class.getCanonicalName()),
                                            NodeList.nodeList(
                                                    new ClassOrInterfaceType(null, outputModelClassGenerator.className()))),
                                    NodeList.nodeList())));

                    modelClass.addMember(subprocessModelField);

                    subprocessModelField.createGetter();
                    MethodDeclaration addSubInstance = new MethodDeclaration()
                            .setName("add" + StringUtils.capitalize(entry.getKey())).setModifiers(Keyword.PUBLIC)
                            .setType(void.class)
                            .addParameter(outputModelClassGenerator.className(), "model");
                    addSubInstance.setBody(new BlockStmt().addStatement(
                            new MethodCallExpr(new NameExpr(entry.getKey()), "add").addArgument(new NameExpr("model"))));

                    modelClass.addMember(addSubInstance);
                }
            }
        }
    }
}
