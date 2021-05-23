package io.automatiko.engine.codegen.process.augmentors;

import static com.github.javaparser.StaticJavaParser.parse;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

public class GcpPubSubModelAugmentor implements Consumer<CompilationUnit> {

    @Override
    public void accept(CompilationUnit unit) {

        Optional<ClassOrInterfaceDeclaration> mainClass = unit.findFirst(ClassOrInterfaceDeclaration.class);

        if (!mainClass.isPresent()) {
            throw new NoSuchElementException("Cannot find class declaration in the template");
        }
        ClassOrInterfaceDeclaration modelClass = mainClass.get();

        CompilationUnit compilationUnit = parse(
                this.getClass().getResourceAsStream("/class-templates/GcpPubSubDeserializerTemplate.java"));

        ClassOrInterfaceDeclaration deserializerClass = compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).get();
        deserializerClass.setName(modelClass.getName() + "Deserializer");

        modelClass.addAnnotation(new NormalAnnotationExpr(new Name("com.fasterxml.jackson.databind.annotation.JsonDeserialize"),
                NodeList.nodeList(new MemberValuePair("using",
                        new NameExpr(deserializerClass.getNameAsString() + ".class")))));

        deserializerClass.findFirst(MethodDeclaration.class, md -> md.getNameAsString().equals("type")).ifPresent(md -> {
            md.setBody(new BlockStmt().addStatement(new ReturnStmt(new NameExpr(modelClass.getNameAsString() + ".class"))));
        });

        unit.addType(deserializerClass);

    }

}
