
package io.automatiko.engine.codegen.process;

import java.nio.file.Path;
import java.util.Optional;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.workflow.compiler.canonical.ProcessMetaData;
import io.automatiko.engine.workflow.compiler.canonical.ProcessToExecModelGenerator;

public class ProcessExecutableModelGenerator {

    private final WorkflowProcess workFlowProcess;
    private final ProcessToExecModelGenerator execModelGenerator;
    private String processFilePath;
    private ProcessMetaData processMetaData;

    public ProcessExecutableModelGenerator(WorkflowProcess workFlowProcess,
            ProcessToExecModelGenerator execModelGenerator) {
        this.workFlowProcess = workFlowProcess;
        this.execModelGenerator = execModelGenerator;
    }

    public boolean isPublic() {
        return WorkflowProcess.PUBLIC_VISIBILITY.equalsIgnoreCase(workFlowProcess.getVisibility());
    }

    public ProcessMetaData generate() {
        if (processMetaData != null)
            return processMetaData;
        processMetaData = execModelGenerator.generate(workFlowProcess);

        // this is ugly, but this class will be refactored
        String processClazzName = processMetaData.getProcessClassName();
        processFilePath = processClazzName.replace('.', '/') + ".java";
        return processMetaData;
    }

    public String description() {
        return Optional.ofNullable(workFlowProcess.getMetaData().get("Description")).map(Object::toString)
                .orElse("Executes " + workFlowProcess.getName());
    }

    public String className() {
        if (processMetaData == null)
            generate();
        return processMetaData.getProcessClassName();
    }

    public String generatedFilePath() {
        return processFilePath;
    }

    private String getCompiledClassName(Path fileNameRelative) {
        return fileNameRelative.toString().replace("/", ".").replace(".java", "");
    }

    public String extractedProcessId() {
        return execModelGenerator.extractProcessId(workFlowProcess.getId(),
                CodegenUtils.version(workFlowProcess.getVersion()));
    }

    public String getProcessId() {
        String version = "";
        if (workFlowProcess.getVersion() != null) {
            version = "_" + workFlowProcess.getVersion();
        }
        return workFlowProcess.getId() + version;
    }

    public WorkflowProcess process() {
        return workFlowProcess;
    }
}
