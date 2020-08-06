
package io.automatik.engine.codegen.process;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.codegen.GeneratorContext;
import io.automatik.engine.services.utils.StringUtils;
import io.automatik.engine.workflow.compiler.canonical.ModelMetaData;
import io.automatik.engine.workflow.compiler.canonical.ProcessToExecModelGenerator;

public class ModelClassGenerator {

	private final GeneratorContext context;
	private final WorkflowProcess workFlowProcess;
	private String modelFileName;
	private ModelMetaData modelMetaData;
	private String modelClassName;

	public ModelClassGenerator(GeneratorContext context, WorkflowProcess workFlowProcess) {
		String pid = workFlowProcess.getId();
		String name = ProcessToExecModelGenerator.extractProcessId(pid);
		this.modelClassName = workFlowProcess.getPackageName() + "." + StringUtils.capitalize(name) + "Model";

		this.context = context;
		this.workFlowProcess = workFlowProcess;
	}

	public ModelMetaData generate() {
		// create model class for all variables
		modelMetaData = ProcessToExecModelGenerator.INSTANCE.generateModel(workFlowProcess);
		modelFileName = modelMetaData.getModelClassName().replace('.', '/') + ".java";

		modelMetaData.setSupportsValidation(context.getBuildContext().isValidationSupported());

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
