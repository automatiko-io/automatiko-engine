
package io.automatik.engine.codegen;

import static com.github.javaparser.StaticJavaParser.parse;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import io.automatik.engine.codegen.context.ApplicationBuildContext;

public class GeneratorContext {

	private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorContext.class);

	protected static final String APPLICATION_PROPERTIES_FILE_NAME = "application.properties";

	public static GeneratorContext ofResourcePath(File resourcePath, File classesPath) {
		Properties applicationProperties = new Properties();

		try (FileReader fileReader = new FileReader(new File(resourcePath, APPLICATION_PROPERTIES_FILE_NAME))) {
			applicationProperties.load(fileReader);
		} catch (IOException ioe) {
			LOGGER.debug("Unable to load '" + APPLICATION_PROPERTIES_FILE_NAME + "'.");
		}

		return new GeneratorContext(applicationProperties, resourcePath, classesPath);
	}

	private ApplicationBuildContext buildContext;

	private File resourcePath;
	private File classesPath;

	private Properties applicationProperties = new Properties();
	private Map<String, String> modifiedApplicationProperties = new LinkedHashMap<String, String>();

	protected GeneratorContext(Properties properties, File resourcePath, File classesPath) {
		this.applicationProperties = properties;
		this.resourcePath = resourcePath;
		this.classesPath = classesPath;
	}

	public GeneratorContext withBuildContext(ApplicationBuildContext buildContext) {
		this.buildContext = buildContext;
		return this;
	}

	public ApplicationBuildContext getBuildContext() {
		return this.buildContext;
	}

	public Optional<String> getApplicationProperty(String property) {
		return Optional.ofNullable(
				modifiedApplicationProperties.getOrDefault(property, applicationProperties.getProperty(property)));
	}

	public Collection<String> getApplicationProperties() {
		return applicationProperties.stringPropertyNames();
	}

	public void setApplicationProperty(String property, String value) {
		this.modifiedApplicationProperties.put(property, value);
	}

	public CompilationUnit write(String packageName) {

		CompilationUnit clazz = parse(
				this.getClass().getResourceAsStream("/class-templates/config/ConfigPropertiesTemplate.java"));

		clazz.setPackageDeclaration(packageName);
		ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class).get();

		BlockStmt constructorBody = new BlockStmt();

		for (Entry<String, String> entry : modifiedApplicationProperties.entrySet()) {
			if (!applicationProperties.containsKey(entry.getKey())) { // avoid overriding of defined properties

				MethodCallExpr putItem = new MethodCallExpr(new NameExpr("properties"), "put")
						.addArgument(new StringLiteralExpr(entry.getKey()))
						.addArgument(new StringLiteralExpr(entry.getValue()));

				constructorBody.addStatement(putItem);
			}
		}

		ConstructorDeclaration constructor = new ConstructorDeclaration().setName(template.getName().asString())
				.addModifier(Modifier.Keyword.PUBLIC).setBody(constructorBody);

		template.addMember(constructor);

		return clazz;
	}
}
