
package io.automatik.engine.codegen.process;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.codegen.GeneratorContext;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.compiler.canonical.ModelMetaData;
import io.automatik.engine.workflow.compiler.canonical.ProcessToExecModelGenerator;
import io.automatik.engine.workflow.compiler.canonical.VariableDeclarations;
import io.automatik.engine.workflow.util.StringUtils;

public class InputModelClassGenerator {

	private final GeneratorContext context;
	private final WorkflowProcess workFlowProcess;
	private String className;
	private String modelFileName;
	private ModelMetaData modelMetaData;
	private String modelClassName;

	public InputModelClassGenerator(GeneratorContext context, WorkflowProcess workFlowProcess) {
		String pid = workFlowProcess.getId();
		className = StringUtils.capitalize(ProcessToExecModelGenerator.extractProcessId(pid) + "ModelInput");
		this.modelClassName = workFlowProcess.getPackageName() + "." + className;

		this.context = context;
		this.workFlowProcess = workFlowProcess;
	}

	public ModelMetaData generate() {
		// create model class for all variables
		String packageName = workFlowProcess.getPackageName();

		modelMetaData = new ModelMetaData(workFlowProcess.getId(), packageName, className,
				workFlowProcess.getVisibility(),
				VariableDeclarations
						.ofInput((VariableScope) ((io.automatik.engine.workflow.base.core.Process) workFlowProcess)
								.getDefaultContext(VariableScope.VARIABLE_SCOPE)),
				true, "/class-templates/ModelNoIDTemplate.java");
		modelMetaData.setSupportsValidation(context.getBuildContext().isValidationSupported());

		modelFileName = modelMetaData.getModelClassName().replace('.', '/') + ".java";
		return modelMetaData;
	}

	public String generatedFilePath() {
		return modelFileName;
	}

	public String simpleName() {
		return modelMetaData.getModelClassSimpleName();
	}

	public String className() {
		return modelClassName;
	}
}
