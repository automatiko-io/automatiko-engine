package io.automatiko.engine.codegen.process.augmentors;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

public class GraphQLModelAugmentor implements Consumer<CompilationUnit> {

    @Override
    public void accept(CompilationUnit unit) {

        Optional<ClassOrInterfaceDeclaration> mainClass = unit.findFirst(ClassOrInterfaceDeclaration.class);

        if (!mainClass.isPresent()) {
            throw new NoSuchElementException("Cannot find class declaration in the template");
        }
        ClassOrInterfaceDeclaration modelClass = mainClass.get();

        modelClass.addAnnotation("org.eclipse.microprofile.graphql.Input");
        modelClass.getImplementedTypes().remove(0);
    }
}
