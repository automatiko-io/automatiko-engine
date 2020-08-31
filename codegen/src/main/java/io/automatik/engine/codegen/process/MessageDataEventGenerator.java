
package io.automatik.engine.codegen.process;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatik.engine.codegen.CodegenUtils.interpolateTypes;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.codegen.BodyDeclarationComparator;
import io.automatik.engine.codegen.CodegenUtils;
import io.automatik.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatik.engine.services.utils.StringUtils;
import io.automatik.engine.workflow.compiler.canonical.TriggerMetaData;

public class MessageDataEventGenerator {
	private final String relativePath;

	private WorkflowProcess process;
	private final String packageName;
	private final String resourceClazzName;
	private String processId;
	private final String processName;
	private DependencyInjectionAnnotator annotator;

	private TriggerMetaData trigger;

	public MessageDataEventGenerator(WorkflowProcess process, TriggerMetaData trigger) {
		this.process = process;
		this.trigger = trigger;
		this.packageName = process.getPackageName();
		this.processId = process.getId();
		this.processName = processId.substring(processId.lastIndexOf('.') + 1);
		String classPrefix = StringUtils.capitalize(processName) + CodegenUtils.version(process.getVersion());
		this.resourceClazzName = classPrefix + "MessageDataEvent_" + trigger.getOwnerId();
		this.relativePath = packageName.replace(".", "/") + "/" + resourceClazzName + ".java";
	}

	public MessageDataEventGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
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
				this.getClass().getResourceAsStream("/class-templates/MessageDataEventTemplate.java"));
		clazz.setPackageDeclaration(process.getPackageName());

		ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class)
				.orElseThrow(() -> new IllegalStateException("Cannot find the class in MessageDataEventTemplate"));
		template.setName(resourceClazzName);

		template.findAll(ClassOrInterfaceType.class).forEach(cls -> interpolateTypes(cls, trigger.getDataType()));
		template.findAll(ConstructorDeclaration.class).stream().forEach(cd -> cd.setName(resourceClazzName));

		template.getMembers().sort(new BodyDeclarationComparator());
		return clazz.toString();
	}

}
