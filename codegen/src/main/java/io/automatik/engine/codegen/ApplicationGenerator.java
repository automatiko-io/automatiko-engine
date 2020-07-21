
package io.automatik.engine.codegen;

import static com.github.javaparser.StaticJavaParser.parse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.lang.model.SourceVersion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatik.engine.api.Config;
import io.automatik.engine.api.event.EventPublisher;
import io.automatik.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatik.engine.codegen.metadata.Labeler;
import io.automatik.engine.codegen.metadata.MetaDataWriter;
import io.automatik.engine.codegen.metadata.PrometheusLabeler;

public class ApplicationGenerator {

	public static final Logger logger = LoggerFactory.getLogger(ApplicationGenerator.class);

	private static final String RESOURCE = "/class-templates/ApplicationTemplate.java";

	public static final String DEFAULT_GROUP_ID = "io.automati.application";
	public static final String DEFAULT_PACKAGE_NAME = "io.automati.application.app";
	public static final String APPLICATION_CLASS_NAME = "Application";

	private final String packageName;
	private final File targetDirectory;

	private DependencyInjectionAnnotator annotator;

	private boolean hasRuleUnits;
	private final List<BodyDeclaration<?>> factoryMethods;
	private ConfigGenerator configGenerator;
	private List<Generator> generators = new ArrayList<>();
	private List<Labeler> labelers = new ArrayList<>();

	private GeneratorContext context;
	private boolean persistence;

	public ApplicationGenerator(String packageName, File targetDirectory) {
		if (packageName == null) {
			throw new IllegalArgumentException(
					"Package name cannot be undefined (null), please specify a package name!");
		}
		if (!SourceVersion.isName(packageName)) {
			throw new IllegalArgumentException(MessageFormat
					.format("Package name \"{0}\" is not valid. It should be a valid Java package name.", packageName));
		}
		this.packageName = packageName;
		this.targetDirectory = targetDirectory;
		this.factoryMethods = new ArrayList<>();
		this.configGenerator = new ConfigGenerator(packageName);
	}

	public String targetCanonicalName() {
		return this.packageName + "." + APPLICATION_CLASS_NAME;
	}

	public String generatedFilePath() {
		return getFilePath(APPLICATION_CLASS_NAME);
	}

	private String getFilePath(String className) {
		return (this.packageName + "." + className).replace('.', '/') + ".java";
	}

	public void addFactoryMethods(Collection<MethodDeclaration> decls) {
		factoryMethods.addAll(decls);
	}

	CompilationUnit compilationUnit() {
		CompilationUnit compilationUnit = parse(this.getClass().getResourceAsStream(RESOURCE))
				.setPackageDeclaration(packageName);

		ClassOrInterfaceDeclaration cls = compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow(
				() -> new NoSuchElementException("Compilation unit doesn't contain a class or interface declaration!"));

		VariableDeclarator eventPublishersDeclarator;
		FieldDeclaration eventPublishersFieldDeclaration = new FieldDeclaration();

		FieldDeclaration serviceField = new FieldDeclaration()
				.addVariable(
						new VariableDeclarator()
								.setType(new ClassOrInterfaceType(null,
										new SimpleName(Optional.class.getCanonicalName()),
										NodeList.nodeList(
												new ClassOrInterfaceType(null, String.class.getCanonicalName()))))
								.setName("service"));

		cls.addMember(eventPublishersFieldDeclaration);
		cls.addMember(serviceField);
		if (useInjection()) {
			annotator.withSingletonComponent(cls);

			cls.findFirst(MethodDeclaration.class, md -> md.getNameAsString().equals("setup"))
					.orElseThrow(() -> new RuntimeException("setup method template not found"))
					.addAnnotation("javax.annotation.PostConstruct");

			annotator.withOptionalInjection(eventPublishersFieldDeclaration);
			eventPublishersDeclarator = new VariableDeclarator(
					new ClassOrInterfaceType(null, new SimpleName(annotator.multiInstanceInjectionType()),
							NodeList.nodeList(new ClassOrInterfaceType(null, EventPublisher.class.getCanonicalName()))),
					"eventPublishers");

			annotator.withConfigInjection(serviceField, "automatik.service.url");
		} else {
			eventPublishersDeclarator = new VariableDeclarator(
					new ClassOrInterfaceType(null, new SimpleName(List.class.getCanonicalName()),
							NodeList.nodeList(new ClassOrInterfaceType(null, EventPublisher.class.getCanonicalName()))),
					"eventPublishers");
			serviceField.getVariable(0)
					.setInitializer(new MethodCallExpr(new NameExpr(Optional.class.getCanonicalName()), "empty"));
		}

		eventPublishersFieldDeclaration.addVariable(eventPublishersDeclarator);

		FieldDeclaration configField = null;
		if (useInjection()) {
			configField = new FieldDeclaration()
					.addVariable(new VariableDeclarator().setType(Config.class.getCanonicalName()).setName("config"));
			annotator.withInjection(configField);
		} else {
			configField = new FieldDeclaration().addModifier(Modifier.Keyword.PROTECTED)
					.addVariable(new VariableDeclarator().setType(Config.class.getCanonicalName()).setName("config")
							.setInitializer(configGenerator.newInstance()));
		}
		cls.addMember(configField);

		factoryMethods.forEach(cls::addMember);

		Optional<BlockStmt> optSetupBody = cls
				.findFirst(MethodDeclaration.class, md -> md.getNameAsString().equals("setup"))
				.flatMap(MethodDeclaration::getBody);
		for (Generator generator : generators) {
			ApplicationSection section = generator.section();
			if (section == null) {
				continue;
			}
			cls.addMember(section.fieldDeclaration());
			cls.addMember(section.factoryMethod());
			optSetupBody.ifPresent(b -> section.setupStatements().forEach(b::addStatement));
		}
		cls.getMembers().sort(new BodyDeclarationComparator());
		return compilationUnit;
	}

	public ApplicationGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
		this.annotator = annotator;
		configGenerator.withDependencyInjection(annotator);
		return this;
	}

	public ApplicationGenerator withGeneratorContext(GeneratorContext context) {
		this.context = context;
		return this;
	}

	public ApplicationGenerator withRuleUnits(boolean hasRuleUnits) {
		this.hasRuleUnits = hasRuleUnits;
		return this;
	}

	public ApplicationGenerator withPersistence(boolean persistence) {
		this.persistence = persistence;
		return this;
	}

	public ApplicationGenerator withMonitoring(boolean monitoring) {
		if (monitoring) {
			this.labelers.add(new PrometheusLabeler());
		}
		return this;
	}

	public Collection<GeneratedFile> generate() {
		List<GeneratedFile> generatedFiles = generateComponents();
		generators.forEach(gen -> gen.updateConfig(configGenerator));
		if (targetDirectory.isDirectory()) {
			generators.forEach(gen -> MetaDataWriter.writeLabelsImageMetadata(targetDirectory, gen.getLabels()));
		}
		generatedFiles.add(generateApplicationDescriptor());
		generatedFiles.addAll(generateApplicationSections());
		generatedFiles.add(generateApplicationConfigDescriptor());
		if (useInjection()) {
			generators.stream().filter(gen -> gen.section() != null)
					.forEach(gen -> generateSectionClass(gen.section(), generatedFiles));
		}
		this.labelers.forEach(l -> MetaDataWriter.writeLabelsImageMetadata(targetDirectory, l.generateLabels()));
		return generatedFiles;
	}

	public List<GeneratedFile> generateComponents() {
		return generators.stream().flatMap(gen -> gen.generate().stream()).collect(Collectors.toList());
	}

	public GeneratedFile generateApplicationDescriptor() {
		return new GeneratedFile(GeneratedFile.Type.APPLICATION, generatedFilePath(),
				log(compilationUnit().toString()).getBytes(StandardCharsets.UTF_8));
	}

	private List<GeneratedFile> generateApplicationSections() {
		ArrayList<GeneratedFile> generatedFiles = new ArrayList<>();

		for (Generator generator : generators) {
			ApplicationSection section = generator.section();
			if (section == null) {
				continue;
			}
			CompilationUnit sectionUnit = new CompilationUnit();
			sectionUnit.setPackageDeclaration(this.packageName);
			sectionUnit.addType(section.classDeclaration());
			generatedFiles.add(new GeneratedFile(GeneratedFile.Type.APPLICATION_SECTION,
					getFilePath(section.sectionClassName()), sectionUnit.toString()));
		}
		return generatedFiles;
	}

	public GeneratedFile generateApplicationConfigDescriptor() {
		return new GeneratedFile(GeneratedFile.Type.APPLICATION_CONFIG, configGenerator.generatedFilePath(),
				log(configGenerator.compilationUnit().toString()).getBytes(StandardCharsets.UTF_8));
	}

	public void generateSectionClass(ApplicationSection section, List<GeneratedFile> generatedFiles) {
		CompilationUnit cp = section.injectableClass();

		if (cp != null) {
			String packageName = cp.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
			String clazzName = packageName + "."
					+ cp.findFirst(ClassOrInterfaceDeclaration.class).map(c -> c.getName().toString())
							.orElseThrow(() -> new NoSuchElementException(
									"Compilation unit doesn't contain a class or interface declaration!"));
			generatedFiles.add(new GeneratedFile(GeneratedFile.Type.CLASS, clazzName.replace('.', '/') + ".java",
					log(cp.toString()).getBytes(StandardCharsets.UTF_8)));
		}
	}

	public <G extends Generator> G withGenerator(G generator) {
		this.generators.add(generator);
		generator.setPackageName(packageName);
		generator.setDependencyInjection(annotator);
		generator.setProjectDirectory(targetDirectory.getParentFile().toPath());
		generator.setContext(context);
		return generator;
	}

	public static String log(String source) {
		if (logger.isDebugEnabled()) {
			logger.debug("=====");
			logger.debug(source);
			logger.debug("=====");
		}
		return source;
	}

	public static void log(byte[] source) {
		if (logger.isDebugEnabled()) {
			logger.debug("=====");
			logger.debug(new String(source));
			logger.debug("=====");
		}
	}

	protected boolean useInjection() {
		return this.annotator != null;
	}

	public ApplicationGenerator withClassLoader(ClassLoader projectClassLoader) {
		this.configGenerator.withClassLoader(projectClassLoader);
		return this;
	}
}
