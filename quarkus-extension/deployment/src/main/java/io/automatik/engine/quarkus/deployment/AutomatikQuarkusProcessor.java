package io.automatik.engine.quarkus.deployment;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.Model;
import io.automatik.engine.api.UserTask;
import io.automatik.engine.api.codegen.Generated;
import io.automatik.engine.api.codegen.VariableInfo;
import io.automatik.engine.codegen.ApplicationGenerator;
import io.automatik.engine.codegen.GeneratedFile;
import io.automatik.engine.codegen.GeneratorContext;
import io.automatik.engine.codegen.JsonSchemaGenerator;
import io.automatik.engine.codegen.context.QuarkusApplicationBuildContext;
import io.automatik.engine.codegen.decision.DecisionCodegen;
import io.automatik.engine.codegen.di.CDIDependencyInjectionAnnotator;
import io.automatik.engine.codegen.process.ProcessCodegen;
import io.automatik.engine.codegen.process.persistence.PersistenceGenerator;
import io.automatik.engine.workflow.util.IoUtils;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.runtime.LaunchMode;

public class AutomatikQuarkusProcessor {

	private static final String generatedResourcesDir = System.getProperty("io.automatik.codegen.resources.directory",
			"target/generated-resources/kogito");
	private static final String generatedSourcesDir = "target/generated-sources/automatik/";
	private static final String generatedCustomizableSourcesDir = System
			.getProperty("io.automatik.codegen.sources.directory", "target/generated-sources/automatik/");
	private static final Logger logger = LoggerFactory.getLogger(AutomatikQuarkusProcessor.class);
	private final transient String generatedClassesDir = System.getProperty("quarkus.debug.generated-classes-dir");
	private final transient String appPackageName = "io.automatik.app";
	private final transient String persistenceFactoryClass = "io.automatik.engine.addons.persistence.AbstractProcessInstancesFactory";
	private final transient String metricsClass = "io.automatik.engine.addons.monitoring.rest.MetricsResource";

	@BuildStep
	CapabilityBuildItem capability() {
		return new CapabilityBuildItem("automatik");
	}

	@BuildStep
	FeatureBuildItem featureBuildItem() {
		return new FeatureBuildItem("automatik");
	}

	private void generatePersistenceInfo(AppPaths appPaths, BuildProducer<GeneratedBeanBuildItem> generatedBeans,
			IndexView index, LaunchModeBuildItem launchMode, BuildProducer<NativeImageResourceBuildItem> resource,
			CurateOutcomeBuildItem curateOutcomeBuildItem) throws Exception, BootstrapDependencyProcessingException {

		ClassInfo persistenceClass = index.getClassByName(createDotName(persistenceFactoryClass));
		boolean usePersistence = persistenceClass != null;
		List<String> parameters = new ArrayList<>();
		if (usePersistence) {
			for (MethodInfo mi : persistenceClass.methods()) {
				if (mi.name().equals("<init>") && !mi.parameters().isEmpty()) {
					parameters = mi.parameters().stream().map(p -> p.name().toString()).collect(Collectors.toList());
					break;
				}
			}
		}

		Collection<GeneratedFile> generatedFiles = getGeneratedPersistenceFiles(appPaths, index, usePersistence,
				parameters);

		if (!generatedFiles.isEmpty()) {

			compile(appPaths, curateOutcomeBuildItem.getEffectiveModel(), generatedFiles, launchMode.getLaunchMode(),
					generatedBeans, GeneratedBeanBuildItem::new);
		}

		if (usePersistence) {
			resource.produce(new NativeImageResourceBuildItem("automatik-types.proto"));
		}
	}

	private Collection<GeneratedFile> getGeneratedPersistenceFiles(AppPaths appPaths, IndexView index,
			boolean usePersistence, List<String> parameters) {
		GeneratorContext context = buildContext(appPaths, index);

		Collection<ClassInfo> modelClasses = index
				.getAllKnownImplementors(createDotName(Model.class.getCanonicalName()));

		Collection<GeneratedFile> generatedFiles = new ArrayList<>();

		for (Path projectPath : appPaths.projectPaths) {
			PersistenceGenerator persistenceGenerator = new PersistenceGenerator(
					new File(projectPath.toFile(), "target"), modelClasses, usePersistence,
					new JandexProtoGenerator(index, createDotName(Generated.class.getCanonicalName()),
							createDotName(VariableInfo.class.getCanonicalName())),
					parameters);
			persistenceGenerator.setDependencyInjection(new CDIDependencyInjectionAnnotator());
			persistenceGenerator.setPackageName(appPackageName);
			persistenceGenerator.setContext(context);

			generatedFiles.addAll(persistenceGenerator.generate());
		}
		return generatedFiles;
	}

	private Collection<GeneratedFile> getJsonSchemaFiles(Index index, Path classesPath) throws IOException {
		try (URLClassLoader cl = new URLClassLoader(new URL[] { classesPath.toUri().toURL() },
				Thread.currentThread().getContextClassLoader())) {
			return new JsonSchemaGenerator.Builder(
					index.getAnnotations(createDotName(UserTask.class.getCanonicalName())).stream().map(instance -> {
						try {
							return cl.loadClass(instance.target().toString());
						} catch (ClassNotFoundException e) {
							throw new IllegalStateException(e);
						}
					})).withSchemaVersion(System.getProperty("automatik.jsonSchema.version")).build().generate();
		}

	}

	@BuildStep
	public List<ReflectiveHierarchyIgnoreWarningBuildItem> reflectiveDMNREST() {
		List<ReflectiveHierarchyIgnoreWarningBuildItem> result = new ArrayList<>();
		result.add(new ReflectiveHierarchyIgnoreWarningBuildItem(createDotName("org.kie.api.builder.Message$Level")));
		result.add(new ReflectiveHierarchyIgnoreWarningBuildItem(createDotName("org.kie.dmn.api.core.DMNContext")));
		result.add(
				new ReflectiveHierarchyIgnoreWarningBuildItem(createDotName("org.kie.dmn.api.core.DMNDecisionResult")));
		result.add(new ReflectiveHierarchyIgnoreWarningBuildItem(
				createDotName("org.kie.dmn.api.core.DMNDecisionResult$DecisionEvaluationStatus")));
		result.add(new ReflectiveHierarchyIgnoreWarningBuildItem(createDotName("org.kie.dmn.api.core.DMNMessage")));
		result.add(new ReflectiveHierarchyIgnoreWarningBuildItem(
				createDotName("org.kie.dmn.api.core.DMNMessage$Severity")));
		result.add(new ReflectiveHierarchyIgnoreWarningBuildItem(createDotName("org.kie.dmn.api.core.DMNMessageType")));
		result.add(new ReflectiveHierarchyIgnoreWarningBuildItem(
				createDotName("org.kie.dmn.api.feel.runtime.events.FEELEvent")));
		return result;
	}

	@BuildStep(loadsApplicationClasses = true)
	public void generateModel(ArchiveRootBuildItem root, BuildProducer<GeneratedBeanBuildItem> generatedBeans,
			CombinedIndexBuildItem combinedIndexBuildItem, LaunchModeBuildItem launchMode,
			LiveReloadBuildItem liveReload, BuildProducer<NativeImageResourceBuildItem> resource,
			BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CurateOutcomeBuildItem curateOutcomeBuildItem)
			throws Exception, BootstrapDependencyProcessingException {

		if (liveReload.isLiveReload()) {
			return;
		}

		AppPaths appPaths = new AppPaths(root.getPaths());

		ApplicationGenerator appGen = createApplicationGenerator(appPaths, combinedIndexBuildItem);
		Collection<GeneratedFile> generatedFiles = appGen.generate();

		Collection<GeneratedFile> javaFiles = generatedFiles.stream().filter(f -> f.relativePath().endsWith(".java"))
				.collect(Collectors.toCollection(ArrayList::new));
		writeGeneratedFiles(appPaths, generatedFiles);

		if (!javaFiles.isEmpty()) {

			Indexer automatikIndexer = new Indexer();
			Set<DotName> automatikIndex = new HashSet<>();

			compile(appPaths, curateOutcomeBuildItem.getEffectiveModel(), javaFiles, launchMode.getLaunchMode(),
					generatedBeans, (className, data) -> generateBeanBuildItem(combinedIndexBuildItem, automatikIndexer,
							automatikIndex, className, data));

			Index index = automatikIndexer.complete();

			generatePersistenceInfo(appPaths, generatedBeans,
					CompositeIndex.create(combinedIndexBuildItem.getIndex(), index), launchMode, resource,
					curateOutcomeBuildItem);

			reflectiveClass.produce(
					new ReflectiveClassBuildItem(true, true, "io.automatik.engine.api.event.AbstractDataEvent"));
			reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
					"io.automatik.engine.services.event.AbstractProcessDataEvent"));
			reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
					"io.automatik.engine.services.event.ProcessInstanceDataEvent"));
			reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
					"io.automatik.engine.services.event.UserTaskInstanceDataEvent"));
			reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
					"io.automatik.engine.services.event.VariableInstanceDataEvent"));
			reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
					"io.automatik.engine.services.event.impl.ProcessInstanceEventBody"));
			reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
					"io.automatik.engine.services.event.impl.NodeInstanceEventBody"));
			reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
					"io.automatik.engine.services.event.impl.ProcessErrorEventBody"));
			reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
					"io.automatik.engine.services.event.impl.VariableInstanceEventBody"));
			reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
					"io.automatik.engine.services.event.impl.UserTaskInstanceEventBody"));

			Collection<ClassInfo> dataEvents = index
					.getAllKnownSubclasses(createDotName("io.automatik.engine.api.event.AbstractDataEvent"));

			dataEvents.forEach(
					c -> reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, c.name().toString())));

			writeGeneratedFiles(appPaths, getJsonSchemaFiles(index, appPaths.getFirstClassesPath()));
		}
	}

	private void writeGeneratedFiles(AppPaths appPaths, Collection<GeneratedFile> resourceFiles) {
		for (Path projectPath : appPaths.projectPaths) {
			String restResourcePath = projectPath.resolve(generatedCustomizableSourcesDir).toString();
			String resourcePath = projectPath.resolve(generatedResourcesDir).toString();
			String jsonSchemaPath = projectPath.resolve(generatedResourcesDir).resolve("jsonSchema").toString();
			String sourcePath = projectPath.resolve(generatedSourcesDir).toString();

			for (GeneratedFile f : resourceFiles) {
				try {
					if (f.getType() == GeneratedFile.Type.RESOURCE) {
						writeGeneratedFile(f, resourcePath);
					} else if (f.getType() == GeneratedFile.Type.JSON_SCHEMA) {
						writeGeneratedFile(f, jsonSchemaPath);
					} else if (f.getType().isCustomizable()) {
						writeGeneratedFile(f, restResourcePath);
					} else {
						writeGeneratedFile(f, sourcePath);
					}
				} catch (IOException e) {
					logger.warn(String.format("Could not write file '%s'", f.toString()), e);
				}
			}
		}
	}

	private GeneratedBeanBuildItem generateBeanBuildItem(CombinedIndexBuildItem combinedIndexBuildItem,
			Indexer automatikIndexer, Set<DotName> automatikIndex, String className, byte[] data) {
		IndexingUtil.indexClass(className, automatikIndexer, combinedIndexBuildItem.getIndex(), automatikIndex,
				Thread.currentThread().getContextClassLoader(), data);
		return new GeneratedBeanBuildItem(className, data);
	}

	private void compile(AppPaths appPaths, AppModel appModel, Collection<GeneratedFile> generatedFiles,
			LaunchMode launchMode, BuildProducer<GeneratedBeanBuildItem> generatedBeans,
			BiFunction<String, byte[], GeneratedBeanBuildItem> bif) throws Exception {
		List<JavaFileObject> sources = new ArrayList<JavaFileObject>();
		List<String> classpaths = new ArrayList<String>();

		List<String> options = new ArrayList<String>();
		options.add("-proc:none"); // force disable annotation processing
		for (Path classPath : appPaths.classesPaths) {
			classpaths.add(classPath.toString());
		}
		if (appModel != null) {
			for (AppDependency i : appModel.getUserDependencies()) {
				classpaths.add(i.getArtifact().getPaths().getSinglePath().toAbsolutePath().toString());
			}
		}
		if (!classpaths.isEmpty()) {
			options.add("-classpath");
			options.add(classpaths.stream().collect(Collectors.joining(File.pathSeparator)));
		}

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticsCollector, null, null);

		File buildDir = appPaths.getFirstClassesPath().toFile();
		if (buildDir.isFile()) {
			buildDir = new File(buildDir.getParentFile(), "classes");
		}

		fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(buildDir));

		for (GeneratedFile entry : generatedFiles) {
			String generatedClassFile = entry.relativePath().replace("src/main/java/", "");
			String fileName = toRuntimeSource(toClassName(generatedClassFile));

			sources.add(new SourceCode(fileName, new String(entry.contents())));

			String location = generatedClassesDir;
			if (launchMode == LaunchMode.DEVELOPMENT) {
				location = Paths.get(buildDir.toString()).toString();
			}

			writeGeneratedFile(entry, location);
		}

		CompilationTask task = compiler.getTask(null, fileManager, diagnosticsCollector, options, null, sources);
		boolean result = task.call();

		if (result) {

			Iterable<JavaFileObject> compiledClasses = fileManager.list(StandardLocation.CLASS_OUTPUT, "",
					Collections.singleton(JavaFileObject.Kind.CLASS), true);
			for (JavaFileObject jfo : compiledClasses) {

				String clazz = jfo.getName().replaceFirst(buildDir.toString() + "/", "");
				clazz = toClassName(clazz);
				generatedBeans.produce(bif.apply(clazz, IoUtils.readBytesFromInputStream(jfo.openInputStream())));
			}

		} else {
			List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticsCollector.getDiagnostics();
			String errorMessage = diagnostics.stream().map(d -> d.toString()).collect(Collectors.joining(","));

			throw new IllegalStateException(errorMessage);
		}
	}

	private ApplicationGenerator createApplicationGenerator(AppPaths appPaths,
			CombinedIndexBuildItem combinedIndexBuildItem) throws IOException {

		boolean usePersistence = combinedIndexBuildItem.getIndex()
				.getClassByName(createDotName(persistenceFactoryClass)) != null;
		boolean useMonitoring = combinedIndexBuildItem.getIndex().getClassByName(createDotName(metricsClass)) != null;

		GeneratorContext context = buildContext(appPaths, combinedIndexBuildItem.getIndex());

		ApplicationGenerator appGen = new ApplicationGenerator(appPackageName,
				new File(appPaths.getFirstProjectPath().toFile(), "target"))
						.withDependencyInjection(new CDIDependencyInjectionAnnotator()).withPersistence(usePersistence)
						.withMonitoring(useMonitoring).withGeneratorContext(context);

		addProcessGenerator(appPaths, usePersistence, appGen);
		// addDecisionGenerator(appPaths, appGen, useMonitoring);

		return appGen;
	}

	private void addProcessGenerator(AppPaths appPaths, boolean usePersistence, ApplicationGenerator appGen)
			throws IOException {
		ProcessCodegen generator = appPaths.isJar ? ProcessCodegen.ofJar(appPaths.getJarPath())
				: ProcessCodegen.ofPath(appPaths.getProjectPaths());

		appGen.withGenerator(generator).withPersistence(usePersistence)
				.withClassLoader(Thread.currentThread().getContextClassLoader());
	}

	private void addDecisionGenerator(AppPaths appPaths, ApplicationGenerator appGen, boolean useMonitoring)
			throws IOException {
		DecisionCodegen generator = appPaths.isJar ? DecisionCodegen.ofJar(appPaths.getJarPath())
				: DecisionCodegen.ofPath(appPaths.getResourcePaths());

		appGen.withGenerator(generator).withMonitoring(useMonitoring);
	}

	private String toRuntimeSource(String className) {
		return "src/main/java/" + className.replace('.', '/') + ".java";
	}

	private String toClassName(String sourceName) {
		if (sourceName.startsWith("./")) {
			sourceName = sourceName.substring(2);
		}
		if (sourceName.endsWith(".java")) {
			sourceName = sourceName.substring(0, sourceName.length() - 5);
		} else if (sourceName.endsWith(".class")) {
			sourceName = sourceName.substring(0, sourceName.length() - 6);
		}
		return sourceName.replace('/', '.');
	}

	private void writeGeneratedFile(GeneratedFile f, String location) throws IOException {
		if (location == null) {
			return;
		}

		String generatedClassFile = f.relativePath().replace("src/main/java", "");
		Files.write(pathOf(location, generatedClassFile), f.contents());
	}

	private Path pathOf(String location, String end) {
		Path path = Paths.get(location, end);
		path.getParent().toFile().mkdirs();
		return path;
	}

	private DotName createDotName(String name) {
		int lastDot = name.indexOf('.');
		if (lastDot < 0) {
			return DotName.createComponentized(null, name);
		}

		DotName lastDotName = null;
		while (lastDot > 0) {
			String local = name.substring(0, lastDot);
			name = name.substring(lastDot + 1);
			lastDot = name.indexOf('.');
			lastDotName = DotName.createComponentized(lastDotName, local);
		}

		int lastDollar = name.indexOf('$');
		if (lastDollar < 0) {
			return DotName.createComponentized(lastDotName, name);
		}
		DotName lastDollarName = null;
		while (lastDollar > 0) {
			String local = name.substring(0, lastDollar);
			name = name.substring(lastDollar + 1);
			lastDollar = name.indexOf('$');
			if (lastDollarName == null) {
				lastDollarName = DotName.createComponentized(lastDotName, local);
			} else {
				lastDollarName = DotName.createComponentized(lastDollarName, local, true);
			}
		}
		return DotName.createComponentized(lastDollarName, name, true);
	}

	private GeneratorContext buildContext(AppPaths appPaths, IndexView index) {
		GeneratorContext context = GeneratorContext.ofResourcePath(appPaths.getResourceFiles());
		context.withBuildContext(new QuarkusApplicationBuildContext(className -> {
			DotName classDotName = createDotName(className);
			return !index.getAnnotations(classDotName).isEmpty() || index.getClassByName(classDotName) != null;

		}));

		return context;
	}

	private static class AppPaths {

		private final Set<Path> projectPaths = new LinkedHashSet<>();
		private final List<Path> classesPaths = new ArrayList<>();

		private boolean isJar = false;

		private AppPaths(PathsCollection paths) {
			for (Path path : paths) {
				PathType pathType = getPathType(path);
				switch (pathType) {
				case CLASSES: {
					classesPaths.add(path);
					projectPaths.add(path.getParent().getParent());
					break;
				}
				case TEST_CLASSES: {
					projectPaths.add(path.getParent().getParent());
					break;
				}
				case JAR: {
					isJar = true;
					classesPaths.add(path);
					projectPaths.add(path.getParent().getParent());
					break;
				}
				case UNKNOWN: {
					classesPaths.add(path);
					projectPaths.add(path);
					break;
				}
				}
			}
		}

		public Path getFirstProjectPath() {
			return projectPaths.iterator().next();
		}

		public Path getFirstClassesPath() {
			return classesPaths.get(0);
		}

		public Path[] getJarPath() {
			if (!isJar) {
				throw new IllegalStateException("Not a jar");
			}
			return classesPaths.toArray(new Path[classesPaths.size()]);
		}

		public File[] getResourceFiles() {
			return projectPaths.stream().map(p -> p.resolve("src/main/resources").toFile()).toArray(File[]::new);
		}

		public Path[] getResourcePaths() {
			return transformPaths(projectPaths, p -> p.resolve("src/main/resources"));
		}

		public Path[] getSourcePaths() {
			return transformPaths(projectPaths, p -> p.resolve("src"));
		}

		public Path[] getProjectPaths() {
			return transformPaths(projectPaths, Function.identity());
		}

		private Path[] transformPaths(Collection<Path> paths, Function<Path, Path> f) {
			return paths.stream().map(f).toArray(Path[]::new);
		}

		private PathType getPathType(Path archiveLocation) {
			String path = archiveLocation.toString();
			if (path.endsWith("target" + File.separator + "classes")) {
				return PathType.CLASSES;
			}
			if (path.endsWith("target" + File.separator + "test-classes")) {
				return PathType.TEST_CLASSES;
			}
			// Quarkus generates a file with extension .jar.original when doing a native
			// compilation of a uberjar
			// TODO replace ".jar.original" with constant
			// JarResultBuildStep.RENAMED_JAR_EXTENSION when it will be avialable in Quakrus
			// 1.7
			if (path.endsWith(".jar") || path.endsWith(".jar.original")) {
				return PathType.JAR;
			}
			return PathType.UNKNOWN;
		}

		private enum PathType {
			CLASSES, TEST_CLASSES, JAR, UNKNOWN
		}
	}

	private static class SourceCode extends SimpleJavaFileObject {

		private String contents = null;

		public SourceCode(String className, String contents) throws Exception {
			super(new URI(className), Kind.SOURCE);
			this.contents = contents;
		}

		public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
			return contents;
		}

	}
}
