
package io.automatik.engine.codegen.decision;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import io.automatik.engine.api.decision.DecisionModels;
import io.automatik.engine.codegen.AbstractApplicationSection;

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

		CompilationUnit clazz = StaticJavaParser.parse(this.getClass().getResourceAsStream(TEMPLATE_JAVA));
		ClassOrInterfaceDeclaration typeDeclaration = (ClassOrInterfaceDeclaration) clazz.getTypes().get(0);
		for (DMNResource resource : resources) {
			String source = resource.getDmnModel().getResource().getSourcePath();
			Path relativizedPath = Paths.get(source);

			String resourcePath = relativizedPath.toString().replace(File.separatorChar, '/');
			Optional<FieldDeclaration> dmnRuntimeField = typeDeclaration.getFieldByName("dmnRuntime");
			Optional<Expression> initalizer = dmnRuntimeField.flatMap(x -> x.getVariable(0).getInitializer());
			if (initalizer.isPresent()) {
				initalizer.get().asMethodCallExpr().addArgument(new StringLiteralExpr(resourcePath));
			} else {
				throw new RuntimeException("The template " + TEMPLATE_JAVA + " has been modified.");
			}
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
