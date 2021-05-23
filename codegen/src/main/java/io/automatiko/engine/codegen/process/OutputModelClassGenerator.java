
package io.automatiko.engine.codegen.process;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.compiler.canonical.ModelMetaData;
import io.automatiko.engine.workflow.compiler.canonical.ProcessToExecModelGenerator;
import io.automatiko.engine.workflow.compiler.canonical.VariableDeclarations;

public class OutputModelClassGenerator {

    private final GeneratorContext context;
    private final WorkflowProcess workFlowProcess;
    private String className;
    private String modelFileName;
    private ModelMetaData modelMetaData;
    private String modelClassName;

    public OutputModelClassGenerator(GeneratorContext context, WorkflowProcess workFlowProcess) {
        String pid = workFlowProcess.getId();
        className = StringUtils.capitalize(
                ProcessToExecModelGenerator.extractProcessId(pid, CodegenUtils.version(workFlowProcess.getVersion()))
                        + "ModelOutput");
        this.modelClassName = workFlowProcess.getPackageName() + "." + className;

        this.context = context;
        this.workFlowProcess = workFlowProcess;
    }

    public ModelMetaData generate() {
        // create model class for all variables
        String packageName = workFlowProcess.getPackageName();

        modelMetaData = new ModelMetaData(workFlowProcess.getId(), CodegenUtils.version(workFlowProcess.getVersion()),
                packageName, className, workFlowProcess.getVisibility(),
                VariableDeclarations
                        .ofOutput((VariableScope) ((io.automatiko.engine.workflow.base.core.Process) workFlowProcess)
                                .getDefaultContext(VariableScope.VARIABLE_SCOPE)),
                true,
                ProcessToExecModelGenerator.isServerlessWorkflow(workFlowProcess) ? "/class-templates/JsonModelTemplate.java"
                        : "/class-templates/ModelTemplate.java",
                "Output data model for " + workFlowProcess.getName(),
                "Describes output data model expected by " + workFlowProcess.getName());
        modelFileName = modelMetaData.getModelClassName().replace('.', '/') + ".java";
        modelMetaData.setSupportsValidation(context.getBuildContext().isValidationSupported());
        modelMetaData.setSupportsOpenApi(context.getBuildContext().isOpenApiSupported());

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
