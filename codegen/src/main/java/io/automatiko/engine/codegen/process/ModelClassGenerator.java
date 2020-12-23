
package io.automatiko.engine.codegen.process;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.compiler.canonical.ModelMetaData;
import io.automatiko.engine.workflow.compiler.canonical.ProcessToExecModelGenerator;

public class ModelClassGenerator {

    private final GeneratorContext context;
    private final WorkflowProcess workFlowProcess;
    private String modelFileName;
    private ModelMetaData modelMetaData;
    private String modelClassName;

    public ModelClassGenerator(GeneratorContext context, WorkflowProcess workFlowProcess) {
        String pid = workFlowProcess.getId();
        String name = ProcessToExecModelGenerator.extractProcessId(pid,
                CodegenUtils.version(workFlowProcess.getVersion()));
        this.modelClassName = workFlowProcess.getPackageName() + "." + StringUtils.capitalize(name) + "Model";

        this.context = context;
        this.workFlowProcess = workFlowProcess;
    }

    public ModelMetaData generate() {
        // create model class for all variables
        modelMetaData = ProcessToExecModelGenerator.INSTANCE.generateModel(workFlowProcess);
        modelFileName = modelMetaData.getModelClassName().replace('.', '/') + ".java";

        modelMetaData.setSupportsValidation(context.getBuildContext().isValidationSupported());

        modelMetaData.setAsEntity(context.getBuildContext().isEntitiesSupported(),
                context.getBuildContext().config().persistence().database().removeAtCompletion().orElse(false));

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
