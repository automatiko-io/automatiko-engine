
package io.automatiko.engine.codegen;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

/**
 * Base implementation for an {@link ApplicationSection}.
 *
 * It provides a skeleton for a "section" in the Application generated class.
 * Subclasses may extend this base class and decorate the provided simple
 * implementations of the interface methods with custom logic.
 */
public class AbstractApplicationSection implements ApplicationSection {

	private final String sectionClassName;
	private final String methodName;
	private final Class<?> classType;

	public AbstractApplicationSection(String sectionClassName, String methodName, Class<?> classType) {
		this.sectionClassName = sectionClassName;
		this.methodName = methodName;
		this.classType = classType;
	}

	@Override
	public ClassOrInterfaceDeclaration classDeclaration() {
		ClassOrInterfaceDeclaration classDeclaration = new ClassOrInterfaceDeclaration()
				.setModifiers(Modifier.Keyword.PUBLIC).setName(sectionClassName);

		if (classType.isInterface()) {
			classDeclaration.addImplementedType(classType.getCanonicalName());
		} else {
			classDeclaration.addExtendedType(classType.getCanonicalName());
		}

		return classDeclaration;
	}

	@Override
	public String sectionClassName() {
		return sectionClassName;
	}

	@Override
	public FieldDeclaration fieldDeclaration() {
		ObjectCreationExpr objectCreationExpr = new ObjectCreationExpr().setType(sectionClassName);
		if (useApplication()) {
			objectCreationExpr.addArgument("this");
		}
		return new FieldDeclaration().addVariable(new VariableDeclarator().setType(sectionClassName).setName(methodName)
				.setInitializer(objectCreationExpr));
	}

	protected boolean useApplication() {
		return true;
	}

	public MethodDeclaration factoryMethod() {
		return new MethodDeclaration().setModifiers(Modifier.Keyword.PUBLIC).setType(sectionClassName)
				.setName(methodName).setBody(new BlockStmt().addStatement(new ReturnStmt(new NameExpr(methodName))));
	}
}
