
package io.automatiko.engine.workflow.compiler.canonical;

import static com.github.javaparser.StaticJavaParser.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.process.core.impl.WorkflowProcessImpl;
import io.automatiko.engine.workflow.process.core.node.HumanTaskNode;

public class ProcessToExecModelGenerator {

    public static final ProcessToExecModelGenerator INSTANCE = new ProcessToExecModelGenerator(
            ProcessToExecModelGenerator.class.getClassLoader(), Process.WORKFLOW_TYPE);

    private static final String PROCESS_CLASS_SUFFIX = "Process";
    private static final String MODEL_CLASS_SUFFIX = "Model";
    private static final String PROCESS_TEMPLATE_FILE = "/class-templates/ProcessTemplate.java";

    private static List<String> serverlessExtensions = Arrays.asList(".sw.yml", ".sw.yaml", ".sw.json");

    private final ProcessVisitor processVisitor;
    private String workflowType;

    public ProcessToExecModelGenerator(ClassLoader contextClassLoader, String workflowType) {
        this.processVisitor = new ProcessVisitor(contextClassLoader);
        this.workflowType = workflowType;
    }

    public ProcessMetaData generate(WorkflowProcess process) {
        CompilationUnit parsedClazzFile = parse(this.getClass().getResourceAsStream(PROCESS_TEMPLATE_FILE));
        parsedClazzFile.setPackageDeclaration(process.getPackageName());
        Optional<ClassOrInterfaceDeclaration> processClazzOptional = parsedClazzFile
                .findFirst(ClassOrInterfaceDeclaration.class, sl -> true);

        String extractedProcessId = extractProcessId(process.getId(), ModelMetaData.version(process.getVersion()));

        if (!processClazzOptional.isPresent()) {
            throw new NoSuchElementException("Cannot find class declaration in the template");
        }
        ClassOrInterfaceDeclaration processClazz = processClazzOptional.get();
        processClazz.setName(StringUtils.capitalize(extractedProcessId + PROCESS_CLASS_SUFFIX));

        String packageName = parsedClazzFile.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse(null);
        ProcessMetaData metadata = new ProcessMetaData(process.getId(), extractedProcessId, process.getName(),
                ModelMetaData.version(process.getVersion()), packageName, processClazz.getNameAsString(),
                extractSourcePath(process), isServerlessWorkflow(process));

        Optional<MethodDeclaration> processMethod = parsedClazzFile.findFirst(MethodDeclaration.class,
                sl -> sl.getName().asString().equals("buildProcess"));

        processVisitor.visitProcess(process, processMethod.get(), metadata, workflowType);

        metadata.setGeneratedClassModel(parsedClazzFile);
        return metadata;
    }

    public MethodDeclaration generateMethod(WorkflowProcess process) {

        CompilationUnit clazz = parse(this.getClass().getResourceAsStream("/class-templates/ProcessTemplate.java"));
        clazz.setPackageDeclaration(process.getPackageName());

        String extractedProcessId = extractProcessId(process.getId(), process.getVersion());

        String packageName = clazz.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse(null);
        ProcessMetaData metadata = new ProcessMetaData(process.getId(), extractedProcessId, process.getName(),
                ModelMetaData.version(process.getVersion()), packageName, "process", extractSourcePath(process),
                isServerlessWorkflow(process));

        MethodDeclaration processMethod = new MethodDeclaration();
        processVisitor.visitProcess(process, processMethod, metadata, workflowType);

        return processMethod;
    }

    public ModelMetaData generateModel(WorkflowProcess process) {
        String packageName = process.getPackageName();
        String name = extractModelClassName(process.getId(), ModelMetaData.version(process.getVersion()));

        return new ModelMetaData(workflowType, process.getId(), ModelMetaData.version(process.getVersion()), packageName, name,
                process.getVisibility(),
                VariableDeclarations.of((VariableScope) ((io.automatiko.engine.workflow.base.core.Process) process)
                        .getDefaultContext(VariableScope.VARIABLE_SCOPE)),
                false,
                isServerlessWorkflow(process) ? "/class-templates/JsonModelTemplate.java"
                        : "/class-templates/ModelTemplate.java",
                "Complete data model for " + process.getName(),
                "Describes complete data model expected by " + process.getName());
    }

    public ModelMetaData generateInputModel(WorkflowProcess process) {
        String packageName = process.getPackageName();
        String name = extractModelClassName(process.getId(), process.getVersion()) + "Input";

        return new ModelMetaData(workflowType, process.getId(), ModelMetaData.version(process.getVersion()), packageName, name,
                process.getVisibility(),
                VariableDeclarations.ofInput((VariableScope) ((io.automatiko.engine.workflow.base.core.Process) process)
                        .getDefaultContext(VariableScope.VARIABLE_SCOPE)),
                true,
                isServerlessWorkflow(process) ? "/class-templates/JsonModelTemplate.java"
                        : "/class-templates/ModelNoIDTemplate.java",
                "Input data model for " + process.getName(),
                "Describes input data model expected by " + process.getName());
    }

    public ModelMetaData generateOutputModel(WorkflowProcess process) {
        String packageName = process.getPackageName();
        String name = extractModelClassName(process.getId(), process.getVersion()) + "Output";

        return new ModelMetaData(workflowType, process.getId(), ModelMetaData.version(process.getVersion()), packageName, name,
                process.getVisibility(),
                VariableDeclarations.ofOutput((VariableScope) ((io.automatiko.engine.workflow.base.core.Process) process)
                        .getDefaultContext(VariableScope.VARIABLE_SCOPE)),
                true,
                isServerlessWorkflow(process) ? "/class-templates/JsonOutputModelTemplate.java"
                        : "/class-templates/ModelTemplate.java",
                "Output data model for " + process.getName(),
                "Describes output data model expected by " + process.getName());
    }

    public static String extractModelClassName(String processId, String version) {
        return StringUtils.capitalize(extractProcessId(processId, version) + MODEL_CLASS_SUFFIX);
    }

    public List<UserTaskModelMetaData> generateUserTaskModel(WorkflowProcess process, boolean templateSupported) {
        String packageName = process.getPackageName();
        List<UserTaskModelMetaData> usertaskModels = new ArrayList<>();

        VariableScope variableScope = (VariableScope) ((io.automatiko.engine.workflow.base.core.Process) process)
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
                        humanTaskNode, process.getId(), ModelMetaData.version(process.getVersion()), templateSupported));
            }
        }

        return usertaskModels;
    }

    public static String extractProcessId(String processId, String version) {
        String id = processId;

        if (processId.contains(".")) {
            id = processId.substring(processId.lastIndexOf('.') + 1);
        }
        if (version != null && !version.trim().isEmpty()) {
            id += version;
        }

        return id;
    }

    public static boolean isServerlessWorkflow(WorkflowProcess process) {

        return serverlessExtensions.stream()
                .filter(ext -> extractSourcePath(process).endsWith(ext))
                .findAny()
                .isPresent();

    }

    protected static String extractSourcePath(WorkflowProcess process) {
        if (process.getResource() != null) {
            return process.getResource().getSourcePath();
        }

        return "";
    }
}
