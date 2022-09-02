package io.automatiko.engine.codegen;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;

import io.automatiko.engine.workflow.builder.BuilderContext;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

public class LambdaParser {

    public static void parseLambdas(String sourceFile) {
        parseLambdas(sourceFile, md -> {
            if (md.getType().toString().equals(WorkflowBuilder.class.getSimpleName())
                    || md.getType().toString().equals(WorkflowBuilder.class.getCanonicalName())) {
                return true;
            }

            return false;
        });
    }

    public static void parseLambdas(String sourceFile, Predicate<MethodDeclaration> filter) {
        try {

            CompilationUnit unit = StaticJavaParser.parse(new File(sourceFile));

            unit.findAll(MethodDeclaration.class, md -> {
                return filter.test(md);
            }).forEach(md -> {
                String methodName = md.getNameAsString();
                List<String> lambdas = new ArrayList<>();

                md.findAll(LambdaExpr.class).forEach(l -> lambdas.add(alterExpression(l.getBody().toString())));
                BuilderContext.addMethodData(methodName, lambdas);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String alterExpression(String expression) {
        if (expression.endsWith(";")) {
            expression = expression.substring(0, expression.length() - 1);
        }

        return expression;
    }
}
