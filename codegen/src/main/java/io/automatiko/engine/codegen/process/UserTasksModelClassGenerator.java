
package io.automatiko.engine.codegen.process;

import java.util.List;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.workflow.compiler.canonical.ProcessToExecModelGenerator;
import io.automatiko.engine.workflow.compiler.canonical.UserTaskModelMetaData;

public class UserTasksModelClassGenerator {

    private final WorkflowProcess workFlowProcess;
    private List<UserTaskModelMetaData> modelMetaData;

    private GeneratorContext context;

    public UserTasksModelClassGenerator(WorkflowProcess workFlowProcess, GeneratorContext context) {
        this.workFlowProcess = workFlowProcess;
        this.context = context;
    }

    public List<UserTaskModelMetaData> generate() {
        // create model class for all variables
        modelMetaData = ProcessToExecModelGenerator.INSTANCE.generateUserTaskModel(workFlowProcess,
                context.getBuildContext().isUserTaskMgmtSupported());
        return modelMetaData;
    }

    public static String generatedFilePath(String classname) {
        return classname.replace('.', '/') + ".java";
    }

}
