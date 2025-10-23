
package io.automatiko.engine.codegen.decision;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.decision.DecisionModels;
import io.automatiko.engine.codegen.AbstractApplicationSection;
import io.automatiko.engine.services.io.ByteArrayResource;
import io.automatiko.engine.services.utils.IoUtils;

public class DecisionContainerGenerator extends AbstractApplicationSection {

    private static final String TEMPLATE_JAVA = "/class-templates/DMNApplicationClassDeclTemplate.java";

    private String applicationCanonicalName;
    private final List<DMNResource> resources;

    public DecisionContainerGenerator(String applicationCanonicalName, List<DMNResource> resources) {
        super("DecisionModels", "decisionModels", DecisionModels.class);
        this.applicationCanonicalName = applicationCanonicalName;
        this.resources = resources;
    }

    @Override
    public ClassOrInterfaceDeclaration classDeclaration() {

        int index = 0;
        CompilationUnit clazz = StaticJavaParser.parse(this.getClass().getResourceAsStream(TEMPLATE_JAVA));
        ClassOrInterfaceDeclaration typeDeclaration = (ClassOrInterfaceDeclaration) clazz.getTypes().get(0);
        for (DMNResource resource : resources) {
            String source = resource.getDmnModel().getResource().getSourcePath();
            Path relativizedPath = Paths.get(source);
            String decisionContent;
            try {
                decisionContent = Files.readString(relativizedPath);
            } catch (IOException e) {

                try {
                    InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(source);
                    if (input == null) {
                        input = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + source);

                    }
                    decisionContent = new String(IoUtils.readBytesFromInputStream(input));
                } catch (IOException e1) {
                    throw new RuntimeException(e);
                }
            }

            int chunkSize = 1000;
            var dmnContent = new ObjectCreationExpr(null, new ClassOrInterfaceType(null, StringBuilder.class.getCanonicalName()), new NodeList<>());
            var sw = true;
            MethodCallExpr lastCall = null;
            for (int i = 0; i < decisionContent.length(); i += chunkSize) {
                int end = Math.min(decisionContent.length(), i + chunkSize);
                var chunk = decisionContent.substring(i, end);
                
                if(sw) {
                    lastCall = new MethodCallExpr(dmnContent, "append");
                    lastCall.addArgument(new StringLiteralExpr().setString(chunk));
                    sw = false;
                }else {
                    lastCall = new MethodCallExpr(lastCall, "append");
                    lastCall.addArgument(new StringLiteralExpr().setString(chunk));
                }
            }

            String decisionMethodName = "decision_" + index;
            BlockStmt body = new BlockStmt();
            ObjectCreationExpr newbyteResource = new ObjectCreationExpr(null,
                    new ClassOrInterfaceType(null, ByteArrayResource.class.getCanonicalName()),
                    NodeList.nodeList(new MethodCallExpr(new MethodCallExpr(lastCall, "toString"), "getBytes")));
            body.addStatement(new ReturnStmt(newbyteResource));
            MethodDeclaration dmnResourceMethod = new MethodDeclaration().setName(decisionMethodName)
                    .setType(ByteArrayResource.class.getCanonicalName())
                    .setModifiers(Keyword.PRIVATE, Keyword.STATIC)
                    .setBody(body);

            typeDeclaration.addMember(dmnResourceMethod);

            Optional<FieldDeclaration> dmnRuntimeField = typeDeclaration.getFieldByName("dmnRuntime");
            Optional<Expression> initalizer = dmnRuntimeField.flatMap(x -> x.getVariable(0).getInitializer());
            if (initalizer.isPresent()) {
                initalizer.get().asMethodCallExpr().addArgument(new MethodCallExpr(null, decisionMethodName));
            } else {
                throw new RuntimeException("The template " + TEMPLATE_JAVA + " has been modified.");
            }
            index++;
        }
        return typeDeclaration;

    }

    @Override
    protected boolean useApplication() {
        return false;
    }

    @Override
    public List<Statement> setupStatements() {
        return Collections.singletonList(new IfStmt(
                new BinaryExpr(new MethodCallExpr(new MethodCallExpr(null, "config"), "decision"),
                        new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
                new BlockStmt().addStatement(new ExpressionStmt(
                        new MethodCallExpr(new NameExpr("decisionModels"), "init", NodeList.nodeList(new ThisExpr())))),
                null));
    }

}
