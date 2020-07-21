
package io.automatik.engine.codegen;

import java.nio.file.Path;
import java.util.Collection;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.automatik.engine.codegen.ApplicationSection;
import io.automatik.engine.codegen.ConfigGenerator;
import io.automatik.engine.codegen.GeneratedFile;
import io.automatik.engine.codegen.Generator;
import io.automatik.engine.codegen.GeneratorContext;
import io.automatik.engine.codegen.di.DependencyInjectionAnnotator;

public class GeneratorInterfaceTest {

	@Test
	public void getLabels() {
		Assertions.assertThat(getAnonymousGeneratorInstance().getLabels()).isEmpty();
	}

	private Generator getAnonymousGeneratorInstance() {
		return new Generator() {

			@Override
			public ApplicationSection section() {
				return null;
			}

			@Override
			public Collection<GeneratedFile> generate() {
				return null;
			}

			@Override
			public void updateConfig(ConfigGenerator cfg) {

			}

			@Override
			public void setPackageName(String packageName) {

			}

			@Override
			public void setDependencyInjection(DependencyInjectionAnnotator annotator) {

			}

			@Override
			public void setProjectDirectory(Path projectDirectory) {

			}

			@Override
			public void setContext(GeneratorContext context) {

			}

			@Override
			public GeneratorContext context() {
				return null;
			}
		};
	}
}
