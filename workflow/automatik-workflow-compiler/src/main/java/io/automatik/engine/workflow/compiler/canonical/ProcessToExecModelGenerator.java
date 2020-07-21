
package io.automatik.engine.workflow.compiler.canonical;

import static com.github.javaparser.StaticJavaParser.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.impl.WorkflowProcessImpl;
import io.automatik.engine.workflow.process.core.node.HumanTaskNode;
import io.automatik.engine.workflow.util.StringUtils;

public class ProcessToExecModelGenerator {

	public static final ProcessToExecModelGenerator INSTANCE = new ProcessToExecModelGenerator(
			ProcessToExecModelGenerator.class.getClassLoader());

	private static final String PROCESS_CLASS_SUFFIX = "Process";
	private static final String MODEL_CLASS_SUFFIX = "Model";
	private static final String PROCESS_TEMPLATE_FILE = "/class-templates/ProcessTemplate.java";

	private final ProcessVisitor processVisitor;

	public ProcessToExecModelGenerator(ClassLoader contextClassLoader) {
		this.processVisitor = new ProcessVisitor(contextClassLoader);
	}

	public ProcessMetaData generate(WorkflowProcess process) {
		CompilationUnit parsedClazzFile = parse(this.getClass().getResourceAsStream(PROCESS_TEMPLATE_FILE));
		parsedClazzFile.setPackageDeclaration(process.getPackageName());
		Optional<ClassOrInterfaceDeclaration> processClazzOptional = parsedClazzFile
				.findFirst(ClassOrInterfaceDeclaration.class, sl -> true);

		String extractedProcessId = extractProcessId(process.getId());

		if (!processClazzOptional.isPresent()) {
			throw new NoSuchElementException("Cannot find class declaration in the template");
		}
		ClassOrInterfaceDeclaration processClazz = processClazzOptional.get();
		processClazz.setName(StringUtils.capitalize(extractedProcessId + PROCESS_CLASS_SUFFIX));
		String packageName = parsedClazzFile.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse(null);
		ProcessMetaData metadata = new ProcessMetaData(process.getId(), extractedProcessId, process.getName(),
				process.getVersion(), packageName, processClazz.getNameAsString());

		Optional<MethodDeclaration> processMethod = parsedClazzFile.findFirst(MethodDeclaration.class,
				sl -> sl.getName().asString().equals("process"));

		processVisitor.visitProcess(process, processMethod.get(), metadata);

		metadata.setGeneratedClassModel(parsedClazzFile);
		return metadata;
	}

	public MethodDeclaration generateMethod(WorkflowProcess process) {

		CompilationUnit clazz = parse(this.getClass().getResourceAsStream("/class-templates/ProcessTemplate.java"));
		clazz.setPackageDeclaration(process.getPackageName());

		String extractedProcessId = extractProcessId(process.getId());

		String packageName = clazz.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse(null);
		ProcessMetaData metadata = new ProcessMetaData(process.getId(), extractedProcessId, process.getName(),
				process.getVersion(), packageName, "process");

		MethodDeclaration processMethod = new MethodDeclaration();
		processVisitor.visitProcess(process, processMethod, metadata);

		return processMethod;
	}

	public ModelMetaData generateModel(WorkflowProcess process) {
		String packageName = process.getPackageName();
		String name = extractModelClassName(process.getId());

		return new ModelMetaData(process.getId(), packageName, name, process.getVisibility(),
				VariableDeclarations.of((VariableScope) ((io.automatik.engine.workflow.base.core.Process) process)
						.getDefaultContext(VariableScope.VARIABLE_SCOPE)),
				false);
	}

	public ModelMetaData generateInputModel(WorkflowProcess process) {
		String packageName = process.getPackageName();
		String name = extractModelClassName(process.getId()) + "Input";

		return new ModelMetaData(process.getId(), packageName, name, process.getVisibility(),
				VariableDeclarations.ofInput((VariableScope) ((io.automatik.engine.workflow.base.core.Process) process)
						.getDefaultContext(VariableScope.VARIABLE_SCOPE)),
				true, "/class-templates/ModelNoIDTemplate.java");
	}

	public ModelMetaData generateOutputModel(WorkflowProcess process) {
		String packageName = process.getPackageName();
		String name = extractModelClassName(process.getId()) + "Output";

		return new ModelMetaData(process.getId(), packageName, name, process.getVisibility(),
				VariableDeclarations.ofOutput((VariableScope) ((io.automatik.engine.workflow.base.core.Process) process)
						.getDefaultContext(VariableScope.VARIABLE_SCOPE)),
				true);
	}

	public static String extractModelClassName(String processId) {
		return StringUtils.capitalize(extractProcessId(processId) + MODEL_CLASS_SUFFIX);
	}

	public List<UserTaskModelMetaData> generateUserTaskModel(WorkflowProcess process) {
		String packageName = process.getPackageName();
		List<UserTaskModelMetaData> usertaskModels = new ArrayList<>();

		VariableScope variableScope = (VariableScope) ((io.automatik.engine.workflow.base.core.Process) process)
				.getDefaultContext(VariableScope.VARIABLE_SCOPE);

		for (Node node : ((WorkflowProcessImpl) process).getNodesRecursively()) {
			if (node instanceof HumanTaskNode) {
				HumanTaskNode humanTaskNode = (HumanTaskNode) node;
				VariableScope nodeVariableScope = (VariableScope) ((ContextContainer) humanTaskNode
						.getParentContainer()).getDefaultContext(VariableScope.VARIABLE_SCOPE);
				if (nodeVariableScope == null) {
					nodeVariableScope = variableScope;
				}
				usertaskModels.add(new UserTaskModelMetaData(packageName, variableScope, nodeVariableScope,
						humanTaskNode, process.getId()));
			}
		}

		return usertaskModels;
	}

	public static String extractProcessId(String processId) {
		if (processId.contains(".")) {
			return processId.substring(processId.lastIndexOf('.') + 1);
		}

		return processId;
	}
}
