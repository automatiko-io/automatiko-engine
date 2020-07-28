
package io.automatik.engine.codegen.process;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatik.engine.codegen.CodegenUtils.interpolateTypes;
import static io.automatik.engine.codegen.CodegenUtils.isApplicationField;
import static io.automatik.engine.codegen.CodegenUtils.isProcessField;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.codegen.BodyDeclarationComparator;
import io.automatik.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatik.engine.workflow.compiler.canonical.TriggerMetaData;
import io.automatik.engine.workflow.util.StringUtils;

public class MessageConsumerGenerator {
	private final String relativePath;

	private WorkflowProcess process;
	private final String packageName;
	private final String resourceClazzName;
	private final String processClazzName;
	private String processId;
	private String dataClazzName;
	private String modelfqcn;
	private final String processName;
	private final String appCanonicalName;
	private final String messageDataEventClassName;
	private DependencyInjectionAnnotator annotator;

	private TriggerMetaData trigger;

	public MessageConsumerGenerator(WorkflowProcess process, String modelfqcn, String processfqcn,
			String appCanonicalName, String messageDataEventClassName, TriggerMetaData trigger) {
		this.process = process;
		this.trigger = trigger;
		this.packageName = process.getPackageName();
		this.processId = process.getId();
		this.processName = processId.substring(processId.lastIndexOf('.') + 1);
		String classPrefix = StringUtils.capitalize(processName);
		this.resourceClazzName = classPrefix + "MessageConsumer_" + trigger.getOwnerId();
		this.relativePath = packageName.replace(".", "/") + "/" + resourceClazzName + ".java";
		this.modelfqcn = modelfqcn;
		this.dataClazzName = modelfqcn.substring(modelfqcn.lastIndexOf('.') + 1);
		this.processClazzName = processfqcn;
		this.appCanonicalName = appCanonicalName;
		this.messageDataEventClassName = messageDataEventClassName;
	}

	public MessageConsumerGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
		this.annotator = annotator;
		return this;
	}

	public String className() {
		return resourceClazzName;
	}

	public String generatedFilePath() {
		return relativePath;
	}

	protected boolean useInjection() {
		return this.annotator != null;
	}

	public String generate() {
		CompilationUnit clazz = parse(
				this.getClass().getResourceAsStream("/class-templates/MessageConsumerTemplate.java"));
		clazz.setPackageDeclaration(process.getPackageName());
		clazz.addImport(modelfqcn);

		ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class).get();
		template.setName(resourceClazzName);

		template.findAll(ClassOrInterfaceType.class).forEach(cls -> interpolateTypes(cls, dataClazzName));
		template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("configure"))
				.forEach(md -> md.addAnnotation("javax.annotation.PostConstruct"));
		template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("consume"))
				.forEach(md -> {
					//interpolateArguments(md, "String");
					md.findAll(StringLiteralExpr.class)
							.forEach(str -> str.setString(str.asString().replace("$Trigger$", trigger.getName())));
					md.findAll(ClassOrInterfaceType.class).forEach(
							t -> t.setName(t.getNameAsString().replace("$DataEventType$", messageDataEventClassName)));
					md.findAll(ClassOrInterfaceType.class)
							.forEach(t -> t.setName(t.getNameAsString().replace("$DataType$", trigger.getDataType())));
				});
		template.findAll(MethodCallExpr.class).forEach(this::interpolateStrings);

		if (useInjection()) {
			annotator.withApplicationComponent(template);

			template.findAll(FieldDeclaration.class, fd -> isProcessField(fd))
					.forEach(fd -> annotator.withNamedInjection(fd, processId));
			template.findAll(FieldDeclaration.class, fd -> isApplicationField(fd))
					.forEach(fd -> annotator.withInjection(fd));

			template.findAll(FieldDeclaration.class, fd -> fd.getVariable(0).getNameAsString().equals("useCloudEvents"))
					.forEach(fd -> annotator.withConfigInjection(fd, "automatik.messaging.as-cloudevents"));

			template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("consume"))
					.forEach(md -> annotator.withIncomingMessage(md, trigger.getName()));
		} else {
			template.findAll(FieldDeclaration.class, fd -> isProcessField(fd))
					.forEach(fd -> initializeProcessField(fd, template));

			template.findAll(FieldDeclaration.class, fd -> isApplicationField(fd))
					.forEach(fd -> initializeApplicationField(fd, template));
		}
		template.getMembers().sort(new BodyDeclarationComparator());
		return clazz.toString();
	}

	private void initializeProcessField(FieldDeclaration fd, ClassOrInterfaceDeclaration template) {
		fd.getVariable(0).setInitializer(new ObjectCreationExpr().setType(processClazzName));
	}

	private void initializeApplicationField(FieldDeclaration fd, ClassOrInterfaceDeclaration template) {
		fd.getVariable(0).setInitializer(new ObjectCreationExpr().setType(appCanonicalName));
	}

	private void interpolateStrings(MethodCallExpr vv) {
		String s = vv.getNameAsString();
		String interpolated = s.replace("$ModelRef$", StringUtils.capitalize(trigger.getModelRef()));
		vv.setName(interpolated);
	}
}
