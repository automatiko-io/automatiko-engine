
package io.automatiko.engine.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import io.automatiko.engine.codegen.ConfigGenerator;
import io.automatiko.engine.codegen.process.config.ProcessConfigGenerator;

public class ConfigGeneratorTest {

	@Test
	public void withProcessConfig() {
		final ConfigGenerator generator = new ConfigGenerator("org.kie.kogito.test");
		final ProcessConfigGenerator processConfigGenerator = Mockito.mock(ProcessConfigGenerator.class);
		final ConfigGenerator returnedConfigGenerator = generator.withProcessConfig(processConfigGenerator);
		assertThat(returnedConfigGenerator).isNotNull();
		assertThat(returnedConfigGenerator).isSameAs(generator);
	}

	@Test
	public void withProcessConfigNull() {
		final ConfigGenerator generator = new ConfigGenerator("org.kie.kogito.test");
		final ConfigGenerator returnedConfigGenerator = generator.withProcessConfig(null);
		assertThat(returnedConfigGenerator).isNotNull();
		assertThat(returnedConfigGenerator).isSameAs(generator);
	}

	@Test
	public void newInstanceNoProcessConfig() {
		newInstanceTest(null, NullLiteralExpr.class);
	}

	@Test
	public void newInstanceWithProcessConfig() {
		final ProcessConfigGenerator processConfigGenerator = Mockito.mock(ProcessConfigGenerator.class);
		Mockito.when(processConfigGenerator.newInstance()).thenReturn(new ObjectCreationExpr());
		newInstanceTest(processConfigGenerator, ObjectCreationExpr.class);
	}

	private void newInstanceTest(final ProcessConfigGenerator processConfigGenerator,
			final Class<?> expectedArgumentType) {
		ObjectCreationExpr expression = new ConfigGenerator("org.kie.kogito.test")
				.withProcessConfig(processConfigGenerator).newInstance();
		assertThat(expression).isNotNull();

		assertThat(expression.getType()).isNotNull();
		assertThat(expression.getType().asString()).isEqualTo("org.kie.kogito.test.ApplicationConfig");

		assertThat(expression.getArguments()).isNotNull();
		assertThat(expression.getArguments()).hasSize(0);
	}
}
