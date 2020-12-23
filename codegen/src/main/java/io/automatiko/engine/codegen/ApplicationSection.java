
package io.automatiko.engine.codegen;

import java.util.Collections;
import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;

/**
 * A descriptor for a "section" of the root Application class. It contains a
 * factory method for the section (which is an object instance) and the
 * corresponding class.
 *
 * This is to allow the pattern: app.$sectionname().$method()
 *
 * e.g.: app.processes().createMyProcess()
 */
public interface ApplicationSection {

	String sectionClassName();

	FieldDeclaration fieldDeclaration();

	MethodDeclaration factoryMethod();

	ClassOrInterfaceDeclaration classDeclaration();

	default CompilationUnit injectableClass() {
		return null;
	}

	default List<Statement> setupStatements() {
		return Collections.emptyList();
	}

}
