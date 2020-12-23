package io.automatiko.engine.quarkus.deployment;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.codegen.GeneratorContext;

public class QuarkusGeneratorContext extends GeneratorContext {

	private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusGeneratorContext.class);

	protected QuarkusGeneratorContext(Properties properties, File resourcePath, File classesPath) {
		super(properties, resourcePath, classesPath);

	}

	@Override
	public void setApplicationProperty(String property, String value) {
		super.setApplicationProperty(property, value);

	}

	public static GeneratorContext ofResourcePath(File resourcePath, File classesPath) {
		Properties applicationProperties = new Properties();

		try (FileReader fileReader = new FileReader(new File(resourcePath, APPLICATION_PROPERTIES_FILE_NAME))) {
			applicationProperties.load(fileReader);
		} catch (IOException ioe) {
			LOGGER.debug("Unable to load '" + APPLICATION_PROPERTIES_FILE_NAME + "'.");
		}

		return new QuarkusGeneratorContext(applicationProperties, resourcePath, classesPath);
	}
}
