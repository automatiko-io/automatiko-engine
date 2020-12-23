
package io.automatiko.engine.codegen;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;

/**
 * A code generator for a part of the platform, e.g. rules, processes, etc.
 */
public interface Generator {

	/**
	 * Returns the "section" of the Application class corresponding to rules. e.g
	 * the processes() method with processes().createMyProcess() etc.
	 *
	 */
	ApplicationSection section();

	/**
	 * Returns the collection of all the files that have been generated/compiled
	 *
	 */
	Collection<GeneratedFile> generate();

	/**
	 * Consumes the given ConfigGenerator so that it can enrich it with further,
	 * Generator-specific details.
	 *
	 * This is automatically called by the ApplicationGenerator.
	 */
	void updateConfig(ConfigGenerator cfg);

	void setPackageName(String packageName);

	void setDependencyInjection(DependencyInjectionAnnotator annotator);

	void setProjectDirectory(Path projectDirectory);

	void setContext(GeneratorContext context);

	GeneratorContext context();

	default Map<String, String> getLabels() {
		return Collections.emptyMap();
	}
}