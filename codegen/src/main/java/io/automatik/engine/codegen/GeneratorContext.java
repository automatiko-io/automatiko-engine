
package io.automatik.engine.codegen;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.codegen.context.ApplicationBuildContext;

public class GeneratorContext {

	private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorContext.class);

	private static final String APPLICATION_PROPERTIES_FILE_NAME = "application.properties";

	public static GeneratorContext ofResourcePath(File... resourcePaths) {
		Properties applicationProperties = new Properties();

		for (File resourcePath : resourcePaths) {
			try (FileReader fileReader = new FileReader(new File(resourcePath, APPLICATION_PROPERTIES_FILE_NAME))) {
				applicationProperties.load(fileReader);
			} catch (IOException ioe) {
				LOGGER.debug("Unable to load '" + APPLICATION_PROPERTIES_FILE_NAME + "'.");
			}
		}

		return new GeneratorContext(applicationProperties);
	}

	public static GeneratorContext ofProperties(Properties props) {
		return new GeneratorContext(props);
	}

	private ApplicationBuildContext buildContext;

	private Properties applicationProperties = new Properties();

	private GeneratorContext(Properties properties) {
		this.applicationProperties = properties;
	}

	public GeneratorContext withBuildContext(ApplicationBuildContext buildContext) {
		this.buildContext = buildContext;
		return this;
	}

	public ApplicationBuildContext getBuildContext() {
		return this.buildContext;
	}

	public Optional<String> getApplicationProperty(String property) {
		return Optional.ofNullable(applicationProperties.getProperty(property));
	}

	public Collection<String> getApplicationProperties() {
		return applicationProperties.stringPropertyNames();
	}
}
