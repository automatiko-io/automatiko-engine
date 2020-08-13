
package io.automatik.engine.codegen.process;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatik.engine.codegen.CodeGenConstants.INCOMING_PROP_PREFIX;
import static io.automatik.engine.codegen.CodeGenConstants.MQTT_CONNECTOR;
import static io.automatik.engine.codegen.CodegenUtils.interpolateTypes;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.codegen.BodyDeclarationComparator;
import io.automatik.engine.codegen.CodegenUtils;
import io.automatik.engine.codegen.GeneratorContext;
import io.automatik.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatik.engine.services.utils.StringUtils;
import io.automatik.engine.workflow.compiler.canonical.TriggerMetaData;

public class MessageProducerGenerator {

	private static final String EVENT_DATA_VAR = "eventData";

	private final String relativePath;

	private GeneratorContext context;

	private WorkflowProcess process;
	private final String packageName;
	private final String resourceClazzName;
	private String processId;
	private final String processName;
	private final String messageDataEventClassName;
	private DependencyInjectionAnnotator annotator;

	private TriggerMetaData trigger;

	public MessageProducerGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn,
			String processfqcn, String messageDataEventClassName, TriggerMetaData trigger) {
		this.context = context;
		this.process = process;
		this.trigger = trigger;
		this.packageName = process.getPackageName();
		this.processId = process.getId();
		this.processName = processId.substring(processId.lastIndexOf('.') + 1);
		String classPrefix = StringUtils.capitalize(processName);
		this.resourceClazzName = classPrefix + "MessageProducer_" + trigger.getOwnerId();
		this.relativePath = packageName.replace(".", "/") + "/" + resourceClazzName + ".java";
		this.messageDataEventClassName = messageDataEventClassName;
	}

	public MessageProducerGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
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

	protected void appendConnectorSpecificProperties(String connector) {
		if (connector.equals(MQTT_CONNECTOR)) {
			String sanitizedName = CodegenUtils.triggerSanitizedName(trigger);
			context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".host", "localhost");
			context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".port", "1883");
			context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".client-id",
					sanitizedName + "-consumer");
			context.setApplicationProperty("quarkus.automatik.messaging.as-cloudevents", "false");
		}
	}

	public String generate() {
		
		String sanitizedName = CodegenUtils.triggerSanitizedName(trigger);
		String connector = CodegenUtils.getConnector(context);
		if (connector != null) {

			context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".connector", connector);
			context.setApplicationProperty(INCOMING_PROP_PREFIX + sanitizedName + ".topic", trigger.getName());
			appendConnectorSpecificProperties(connector);
		}
		
		CompilationUnit clazz = parse(
				this.getClass().getResourceAsStream("/class-templates/MessageProducerTemplate.java"));
		clazz.setPackageDeclaration(process.getPackageName());

		ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class).get();
		template.setName(resourceClazzName);

		template.findAll(ClassOrInterfaceType.class).forEach(cls -> interpolateTypes(cls, trigger.getDataType()));
		template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("produce"))
				.forEach(md -> md.getParameters().stream().filter(p -> p.getNameAsString().equals(EVENT_DATA_VAR))
						.forEach(p -> p.setType(trigger.getDataType())));
		template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("configure"))
				.forEach(md -> md.addAnnotation("javax.annotation.PostConstruct"));
		template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("marshall"))
				.forEach(md -> {
					md.getParameters().stream().filter(p -> p.getNameAsString().equals(EVENT_DATA_VAR))
							.forEach(p -> p.setType(trigger.getDataType()));
					md.findAll(ClassOrInterfaceType.class).forEach(
							t -> t.setName(t.getNameAsString().replace("$DataEventType$", messageDataEventClassName)));
				});

		if (useInjection()) {
			annotator.withApplicationComponent(template);

			FieldDeclaration emitterField = template.findFirst(FieldDeclaration.class)
					.filter(fd -> fd.getVariable(0).getNameAsString().equals("emitter")).get();
			annotator.withInjection(emitterField);
			annotator.withOutgoingMessage(emitterField, sanitizedName);
			emitterField.getVariable(0).setType(annotator.emitterType("Message"));

			MethodDeclaration produceMethod = template.findAll(MethodDeclaration.class).stream()
					.filter(md -> md.getNameAsString().equals("produce")).findFirst().orElseThrow(
							() -> new IllegalStateException("Cannot find produce methos in MessageProducerTemplate"));
			BlockStmt body = new BlockStmt();
			MethodCallExpr sendMethodCall = new MethodCallExpr(new NameExpr("emitter"), "send");
			annotator.withMessageProducer(sendMethodCall, sanitizedName,
					new MethodCallExpr(new ThisExpr(), "marshall").addArgument(new NameExpr("pi"))
							.addArgument(new NameExpr(EVENT_DATA_VAR)));
			body.addStatement(sendMethodCall);
			produceMethod.setBody(body);

			template.findAll(FieldDeclaration.class, fd -> fd.getVariable(0).getNameAsString().equals("useCloudEvents"))
					.forEach(fd -> annotator.withConfigInjection(fd, "quarkus.automatik.messaging.as-cloudevents"));

		}
		template.getMembers().sort(new BodyDeclarationComparator());
		return clazz.toString();
	}

}
