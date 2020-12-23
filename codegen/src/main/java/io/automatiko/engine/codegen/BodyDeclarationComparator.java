
package io.automatiko.engine.codegen;

import java.util.Comparator;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

public class BodyDeclarationComparator implements Comparator<BodyDeclaration<?>> {

	@Override
	public int compare(BodyDeclaration<?> o1, BodyDeclaration<?> o2) {
		if (o1 instanceof FieldDeclaration && o2 instanceof FieldDeclaration) {
			return 0;
		}
		if (o1 instanceof FieldDeclaration && !(o2 instanceof FieldDeclaration)) {
			return -1;
		}

		if (o1 instanceof ConstructorDeclaration && o2 instanceof ConstructorDeclaration) {
			return 0;
		}
		if (o1 instanceof ConstructorDeclaration && o2 instanceof MethodDeclaration) {
			return -1;
		}
		if (o1 instanceof ConstructorDeclaration && o2 instanceof FieldDeclaration) {
			return 1;
		}
		return 1;
	}

}
