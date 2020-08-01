package io.automatik.engine.quarkus.deployment;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.automatik.engine.codegen.ApplicationGenerator;
import io.automatik.engine.codegen.GeneratedFile;
import io.automatik.engine.codegen.Generator;
import io.automatik.engine.codegen.GeneratorContext;
import io.automatik.engine.codegen.context.QuarkusApplicationBuildContext;
import io.automatik.engine.codegen.di.CDIDependencyInjectionAnnotator;
import io.automatik.engine.quarkus.AutomatikBuildTimeConfig;
import io.quarkus.deployment.dev.JavaCompilationProvider;

public abstract class AutomatikCompilationProvider extends JavaCompilationProvider {

	protected static Map<Path, Path> classToSource = new HashMap<>();
	protected static AutomatikBuildTimeConfig config;

	@Override
	public Set<String> handledSourcePaths() {
		return Collections.singleton("src" + File.separator + "main" + File.separator + "resources");
	}

	@Override
	public final void compile(Set<File> filesToCompile, Context context) {

		File outputDirectory = context.getOutputDirectory();
		try {
			GeneratorContext generationContext = GeneratorContext
					.ofResourcePath(context.getProjectDirectory().toPath().resolve("src/main/resources").toFile());
			generationContext.withBuildContext(
					new QuarkusApplicationBuildContext(config, className -> hasClassOnClasspath(context, className)));

			ApplicationGenerator appGen = new ApplicationGenerator(
					config.packageName().orElse(AutomatikQuarkusProcessor.DEFAULT_PACKAGE_NAME), outputDirectory)
							.withDependencyInjection(new CDIDependencyInjectionAnnotator())
							.withGeneratorContext(generationContext);

			addGenerator(appGen, filesToCompile, context);

			Collection<GeneratedFile> generatedFiles = appGen.generate();

			Set<File> generatedSourceFiles = new HashSet<>();
			for (GeneratedFile file : generatedFiles) {
				Path path = pathOf(outputDirectory.getPath(), file.relativePath());
				if (file.getType() != GeneratedFile.Type.APPLICATION
						&& file.getType() != GeneratedFile.Type.APPLICATION_CONFIG) {
					Files.write(path, file.contents());
					generatedSourceFiles.add(path.toFile());
				}
			}
			super.compile(generatedSourceFiles, context);
		} catch (IOException e) {
			throw new AutomatikCompilerException(e);
		}
	}

	@Override
	public Path getSourcePath(Path classFilePath, Set<String> sourcePaths, String classesPath) {
		if (classToSource.containsKey(classFilePath)) {
			return classToSource.get(classFilePath);
		}

		return null;
	}

	protected abstract Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
			throws IOException;

	static Path pathOf(String path, String relativePath) {
		Path p = Paths.get(path, relativePath);
		p.getParent().toFile().mkdirs();
		return p;
	}

	protected boolean hasClassOnClasspath(Context context, String className) {
		try {
			Set<File> elements = context.getClasspath();
			URL[] urls = new URL[elements.size()];

			int i = 0;

			for (File artifact : elements) {

				urls[i] = artifact.toURI().toURL();
				i++;
			}
			try (URLClassLoader cl = new URLClassLoader(urls)) {
				cl.loadClass(className);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
