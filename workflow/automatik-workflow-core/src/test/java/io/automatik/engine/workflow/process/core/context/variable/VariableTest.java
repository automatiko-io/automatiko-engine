
package io.automatik.engine.workflow.process.core.context.variable;

import java.util.stream.Stream;

import javax.lang.model.SourceVersion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.automatik.engine.workflow.base.core.context.variable.Variable;

import static org.assertj.core.api.Assertions.assertThat;

class VariableTest {

	private Variable tested;

	@BeforeEach
	public void setUp() {
		tested = new Variable();
	}

	@Test
	void testValidIdentifierName() {
		final String name = "valid";
		tested.setName(name);
		assertValidSanitizedName(name);
		assertThat(tested.getSanitizedName()).isEqualTo(name);
	}

	private void assertValidSanitizedName(String name) {
		assertThat(tested.getName()).isEqualTo(name);
		assertThat(SourceVersion.isName(tested.getSanitizedName())).isTrue();
	}

	@Test
	void testInvalidIdentifierName() {
		final String name = "123valid%^á+-)([]?!@";
		tested.setName(name);
		assertValidSanitizedName(name);
		assertThat(tested.getSanitizedName()).isNotEqualTo(name).isEqualTo("valid");
	}

	@Test
	void testInvalidIdentifierWithReservedWordName() {
		final String name = "123class%^á+-)([]?!@";
		tested.setName(name);
		assertValidSanitizedName(name);
		assertThat(tested.getSanitizedName()).isNotEqualTo(name).isEqualTo("v$class");
	}

	@Test
	void testReservedWordsName() {
		Stream.of("abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package",
				"synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements", "protected",
				"throw", "byte", "else", "import", "public", "throws", "case", "enum", "instanceof", "return",
				"transient", "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void",
				"class", "finally", "long", "strictfp", "volatile", "const", "float", "native", "super", "while")
				.forEach(name -> {
					tested.setName(name);
					assertValidSanitizedName(name);
					assertThat(tested.getSanitizedName()).isNotEqualTo(name).isEqualTo("v$" + tested.getName());
				});
	}
}