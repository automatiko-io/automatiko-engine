
package io.automatiko.engine.codegen.process;

import static io.automatiko.engine.api.io.ResourceType.determineResourceType;
import static io.automatiko.engine.codegen.ApplicationGenerator.log;
import static io.automatiko.engine.services.utils.IoUtils.readBytesFromInputStream;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import io.automatiko.engine.api.Functions;
import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.api.io.Resource;
import io.automatiko.engine.api.io.ResourceType;
import io.automatiko.engine.codegen.AbstractGenerator;
import io.automatiko.engine.codegen.ApplicationGenerator;
import io.automatiko.engine.codegen.ApplicationSection;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.codegen.ConfigGenerator;
import io.automatiko.engine.codegen.DefaultResourceGeneratorFactory;
import io.automatiko.engine.codegen.GeneratedFile;
import io.automatiko.engine.codegen.GeneratedFile.Type;
import io.automatiko.engine.codegen.ResourceGeneratorFactory;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.codegen.process.config.ProcessConfigGenerator;
import io.automatiko.engine.services.execution.BaseFunctions;
import io.automatiko.engine.services.io.ByteArrayResource;
import io.automatiko.engine.services.io.FileSystemResource;
import io.automatiko.engine.services.io.InternalResource;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.bpmn2.xml.BPMNDISemanticModule;
import io.automatiko.engine.workflow.bpmn2.xml.BPMNExtensionsSemanticModule;
import io.automatiko.engine.workflow.bpmn2.xml.BPMNSemanticModule;
import io.automatiko.engine.workflow.compiler.canonical.ModelMetaData;
import io.automatiko.engine.workflow.compiler.canonical.OpenAPIMetaData;
import io.automatiko.engine.workflow.compiler.canonical.ProcessMetaData;
import io.automatiko.engine.workflow.compiler.canonical.ProcessToExecModelGenerator;
import io.automatiko.engine.workflow.compiler.canonical.TriggerMetaData;
import io.automatiko.engine.workflow.compiler.canonical.UserTaskModelMetaData;
import io.automatiko.engine.workflow.compiler.xml.SemanticModules;
import io.automatiko.engine.workflow.compiler.xml.XmlProcessReader;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;
import io.automatiko.engine.workflow.serverless.parser.ServerlessWorkflowParser;

/**
 * Entry point to process code generation
 */
public class ProcessCodegen extends AbstractGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessCodegen.class);

    private static final SemanticModules BPMN_SEMANTIC_MODULES = new SemanticModules();
    public static final Set<String> SUPPORTED_BPMN_EXTENSIONS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(".bpmn", ".bpmn2")));
    private static final String YAML_PARSER = "yml";
    private static final String JSON_PARSER = "json";
    public static final Map<String, String> SUPPORTED_SW_EXTENSIONS;

    static {
        BPMN_SEMANTIC_MODULES.addSemanticModule(new BPMNSemanticModule());
        BPMN_SEMANTIC_MODULES.addSemanticModule(new BPMNExtensionsSemanticModule());
        BPMN_SEMANTIC_MODULES.addSemanticModule(new BPMNDISemanticModule());

        Map<String, String> extMap = new HashMap<>();
        extMap.put(".sw.yml", YAML_PARSER);
        extMap.put(".sw.yaml", YAML_PARSER);
        extMap.put(".sw.json", JSON_PARSER);
        SUPPORTED_SW_EXTENSIONS = Collections.unmodifiableMap(extMap);
    }

    private ClassLoader contextClassLoader;
    private ResourceGeneratorFactory resourceGeneratorFactory;

    public static ProcessCodegen ofJar(Path... jarPaths) {
        return ofJar(Collections.emptyList(), Collections.emptyList(), jarPaths);
    }

    public static ProcessCodegen ofJar(List<Process> inprocesses, List<String> dependencies, Path... jarPaths) {
        List<Process> processes = new ArrayList<>(inprocesses);

        for (Path jarPath : jarPaths) {
            try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    ResourceType resourceType = determineResourceType(entry.getName());
                    if (SUPPORTED_BPMN_EXTENSIONS.stream().anyMatch(entry.getName()::endsWith)) {
                        InternalResource resource = new ByteArrayResource(
                                readBytesFromInputStream(zipFile.getInputStream(entry)));
                        resource.setResourceType(resourceType);
                        resource.setSourcePath(entry.getName());
                        processes.addAll(parseProcessFile(resource));
                    } else if (SUPPORTED_SW_EXTENSIONS.keySet().stream().anyMatch(entry.getName()::endsWith)) {
                        InternalResource resource = new ByteArrayResource(
                                readBytesFromInputStream(zipFile.getInputStream(entry)));
                        resource.setResourceType(resourceType);
                        resource.setSourcePath(entry.getName());

                        SUPPORTED_SW_EXTENSIONS.entrySet().stream()
                                .filter(e -> entry.getName().endsWith(e.getKey()))
                                .forEach(e -> processes.add(parseWorkflowFile(resource, e.getValue())));

                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        for (String dependency : dependencies) {
            try (ZipFile zipFile = new ZipFile(dependency)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    ResourceType resourceType = determineResourceType(entry.getName());
                    if (SUPPORTED_BPMN_EXTENSIONS.stream().anyMatch(entry.getName()::endsWith)) {
                        InternalResource resource = new ByteArrayResource(
                                readBytesFromInputStream(zipFile.getInputStream(entry)));
                        resource.setResourceType(resourceType);
                        resource.setSourcePath(entry.getName());
                        processes.addAll(parseProcessFile(resource));
                    }
                }
            } catch (IOException e) {

            }
        }

        return ofProcesses(processes);
    }

    public static ProcessCodegen ofPath(Path... paths) throws IOException {

        return ofPath(Collections.emptyList(), Collections.emptyList(), paths);
    }

    public static ProcessCodegen ofPath(List<Process> inprocesses, List<String> dependencies, Path... paths)
            throws IOException {

        List<Process> allProcesses = new ArrayList<>(inprocesses);

        for (String dependency : dependencies) {
            File file = new File(dependency);

            if (file.isDirectory()) {

                try (Stream<Path> filesStream = Files.walk(file.toPath())) {
                    List<File> files = filesStream
                            .filter(p -> SUPPORTED_BPMN_EXTENSIONS.stream().anyMatch(p.toString()::endsWith)
                                    || SUPPORTED_SW_EXTENSIONS.keySet().stream().anyMatch(p.toString()::endsWith))
                            .map(Path::toFile).collect(Collectors.toList());
                    allProcesses.addAll(parseProcesses(files, true));
                }
            } else {

                try (ZipFile zipFile = new ZipFile(dependency)) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        ResourceType resourceType = determineResourceType(entry.getName());
                        if (SUPPORTED_BPMN_EXTENSIONS.stream().anyMatch(entry.getName()::endsWith)) {
                            InternalResource resource = new ByteArrayResource(
                                    readBytesFromInputStream(zipFile.getInputStream(entry)));
                            resource.setResourceType(resourceType);
                            resource.setSourcePath(entry.getName());
                            allProcesses.addAll(parseProcessFile(resource));
                        }
                    }
                } catch (IOException e) {

                }
            }
        }

        for (Path path : paths) {
            Path srcPath = Paths.get(path.toString());
            try (Stream<Path> filesStream = Files.walk(srcPath)) {
                List<File> files = filesStream
                        .filter(p -> SUPPORTED_BPMN_EXTENSIONS.stream().anyMatch(p.toString()::endsWith)
                                || SUPPORTED_SW_EXTENSIONS.keySet().stream().anyMatch(p.toString()::endsWith))
                        .map(Path::toFile).collect(Collectors.toList());
                allProcesses.addAll(parseProcesses(files, false));
            }
        }
        return ofProcesses(allProcesses);
    }

    public static ProcessCodegen ofFiles(Collection<File> processFiles) {
        List<Process> allProcesses = parseProcesses(processFiles, false);
        return ofProcesses(allProcesses);
    }

    public static ProcessCodegen ofFilesAndProcesses(Collection<File> processFiles, List<Process> processes) {
        List<Process> allProcesses = parseProcesses(processFiles, false);
        allProcesses.addAll(processes);
        return ofProcesses(allProcesses);
    }

    public static ProcessCodegen ofProcesses(List<Process> processes) {
        return new ProcessCodegen(processes);
    }

    static List<Process> parseProcesses(Collection<File> processFiles, boolean fromDeps) {
        List<Process> processes = new ArrayList<>();
        for (File processSourceFile : processFiles) {
            if (!fromDeps && processSourceFile.getAbsolutePath().contains("target" + File.separator + "classes")) {
                // exclude any resources files that come from target folder especially in dev mode which can cause overrides
                continue;
            }
            try {
                FileSystemResource r = new FileSystemResource(processSourceFile);
                if (SUPPORTED_BPMN_EXTENSIONS.stream().anyMatch(processSourceFile.getPath()::endsWith)) {
                    processes.addAll(parseProcessFile(r));
                } else {
                    SUPPORTED_SW_EXTENSIONS.entrySet().stream()
                            .filter(e -> processSourceFile.getPath().endsWith(e.getKey()))
                            .forEach(e -> processes.add(parseWorkflowFile(r, e.getValue())));
                }
                if (processes.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Unable to process file with unsupported extension: " + processSourceFile);
                }
            } catch (RuntimeException e) {
                throw new ProcessCodegenException(processSourceFile.getAbsolutePath(), e);
            }
        }
        return processes;
    }

    private static Process parseWorkflowFile(Resource r, String parser) {
        try {
            ServerlessWorkflowParser workflowParser = new ServerlessWorkflowParser();
            Process p = workflowParser.parse(r.getReader());
            ((WorkflowProcess) p).getMetaData().put("IsServerlessWorkflow", true);
            p.setResource(r);
            return p;
        } catch (IOException e) {
            throw new ProcessParsingException("Could not parse file " + r.getSourcePath(), e);
        }
    }

    private static Collection<? extends Process> parseProcessFile(Resource r) {
        try {
            XmlProcessReader xmlReader = new XmlProcessReader(BPMN_SEMANTIC_MODULES,
                    Thread.currentThread().getContextClassLoader());
            Collection<? extends Process> parsed = xmlReader.read(r.getReader());
            parsed.forEach(p -> p.setResource(r));
            return parsed;
        } catch (SAXException | IOException e) {
            throw new ProcessParsingException("Could not parse file " + r.getSourcePath(), e);
        }
    }

    private String applicationCanonicalName;
    private DependencyInjectionAnnotator annotator;

    private ProcessesContainerGenerator moduleGenerator;

    private final Map<String, WorkflowProcess> processes;
    private final List<GeneratedFile> generatedFiles = new ArrayList<>();

    private boolean persistence;

    public ProcessCodegen(Collection<? extends Process> processes) {
        this.processes = new HashMap<>();
        for (Process process : processes) {
            String version = "";
            if (process.getVersion() != null) {
                version = "_" + process.getVersion();
            }
            this.processes.put(process.getId() + version, (WorkflowProcess) process);
        }

        // set default package name
        setPackageName(ApplicationGenerator.DEFAULT_PACKAGE_NAME);
        contextClassLoader = Thread.currentThread().getContextClassLoader();

        resourceGeneratorFactory = new DefaultResourceGeneratorFactory();
    }

    public static String defaultWorkItemHandlerConfigClass(String packageName) {
        return packageName + ".WorkItemHandlerConfig";
    }

    public static String defaultProcessListenerConfigClass(String packageName) {
        return packageName + ".ProcessEventListenerConfig";
    }

    public void setPackageName(String packageName) {
        this.moduleGenerator = new ProcessesContainerGenerator(packageName);
        this.applicationCanonicalName = packageName + ".Application";
    }

    public void setDependencyInjection(DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
        this.moduleGenerator.withDependencyInjection(annotator);
    }

    public ProcessesContainerGenerator moduleGenerator() {
        return moduleGenerator;
    }

    public ProcessCodegen withPersistence(boolean persistence) {
        this.persistence = persistence;
        return this;
    }

    public ProcessCodegen withClassLoader(ClassLoader projectClassLoader) {
        this.contextClassLoader = projectClassLoader;
        return this;
    }

    public List<GeneratedFile> generate() {
        if (processes.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProcessGenerator> ps = new ArrayList<>();
        List<ProcessInstanceGenerator> pis = new ArrayList<>();
        List<ProcessExecutableModelGenerator> processExecutableModelGenerators = new ArrayList<>();
        List<AbstractResourceGenerator> rgs = new ArrayList<>(); // REST resources
        List<AbstractResourceGenerator> grapggs = new ArrayList<>(); // GraphQL resources
        List<FunctionGenerator> fgs = new ArrayList<>(); // Function resources
        List<FunctionFlowGenerator> ffgs = new ArrayList<>(); // Function flow resources
        List<MessageDataEventGenerator> mdegs = new ArrayList<>(); // message data events
        Set<MessageConsumerGenerator> megs = new LinkedHashSet<>(); // message endpoints/consumers
        List<MessageProducerGenerator> mpgs = new ArrayList<>(); // message producers
        Set<OpenAPIClientGenerator> opgs = new LinkedHashSet<>(); // OpenAPI clients

        List<String> publicProcesses = new ArrayList<>();

        Map<String, ModelMetaData> processIdToModel = new HashMap<>();

        Map<String, ModelClassGenerator> processIdToModelGenerator = new HashMap<>();
        Map<String, InputModelClassGenerator> processIdToInputModelGenerator = new HashMap<>();
        Map<String, OutputModelClassGenerator> processIdToOutputModelGenerator = new HashMap<>();

        Map<String, List<UserTaskModelMetaData>> processIdToUserTaskModel = new HashMap<>();
        Map<String, ProcessMetaData> processIdToMetadata = new HashMap<>();

        String workflowType = Process.WORKFLOW_TYPE;
        if (isFunctionFlowProject()) {
            workflowType = Process.FUNCTION_FLOW_TYPE;
        } else if (isFunctionProject()) {
            workflowType = Process.FUNCTION_TYPE;
        }

        // then we can instantiate the exec model generator
        // with the data classes that we have already resolved
        ProcessToExecModelGenerator execModelGenerator = new ProcessToExecModelGenerator(contextClassLoader, workflowType);

        // first we generate all the data classes from variable declarations
        for (Entry<String, WorkflowProcess> entry : processes.entrySet()) {
            ModelClassGenerator mcg = new ModelClassGenerator(execModelGenerator, context(), entry.getValue());
            processIdToModelGenerator.put(entry.getKey(), mcg);
            processIdToModel.put(entry.getKey(), mcg.generate());

            InputModelClassGenerator imcg = new InputModelClassGenerator(context(), entry.getValue(), workflowType);
            processIdToInputModelGenerator.put(entry.getKey(), imcg);

            OutputModelClassGenerator omcg = new OutputModelClassGenerator(context(), entry.getValue(), workflowType);
            processIdToOutputModelGenerator.put(entry.getKey(), omcg);

            context.addGenerator("ModelClassGenerator", entry.getKey(), mcg);
            context.addGenerator("InputModelClassGenerator", entry.getKey(), imcg);
            context.addGenerator("OutputModelClassGenerator", entry.getKey(), omcg);
        }

        // then we generate user task inputs and outputs if any
        for (Entry<String, WorkflowProcess> entry : processes.entrySet()) {
            UserTasksModelClassGenerator utcg = new UserTasksModelClassGenerator(entry.getValue(), context);
            processIdToUserTaskModel.put(entry.getKey(), utcg.generate());
        }

        List<String> functions = context.getBuildContext().classThatImplement(Functions.class.getCanonicalName());

        // collect all process descriptors (exec model)
        for (Entry<String, WorkflowProcess> entry : processes.entrySet()) {

            entry.getValue().getNodesRecursively().stream().filter(node -> node instanceof SubProcessNode).forEach(sp -> {
                String processId = ((SubProcessNode) sp).getProcessId();
                if (((SubProcessNode) sp).getProcessVersion() != null) {
                    processId += "_" + ((SubProcessNode) sp).getProcessVersion();
                }

                WorkflowProcess sprocess = processes.get(processId);

                ((SubProcessNode) sp).setMetaData("serverless", ProcessToExecModelGenerator.isServerlessWorkflow(sprocess));
            });

            ProcessExecutableModelGenerator execModelGen = new ProcessExecutableModelGenerator(entry.getValue(),
                    execModelGenerator);
            String packageName = entry.getValue().getPackageName();
            String id = entry.getKey();

            // add extra meta data to indicate if user task mgmt is available
            if (context.getBuildContext().isUserTaskMgmtSupported()) {
                entry.getValue().getMetaData().put("UserTaskMgmt", "true");
            }
            entry.getValue().getMetaData().putIfAbsent("referencePrefix",
                    context.getApplicationProperty("quarkus.automatiko.resource-path-prefix").orElse(""));

            String resourcePathFormat = context.getApplicationProperty("quarkus.automatiko.resource-path-format").orElse(null);

            if (resourcePathFormat != null) {
                entry.getValue().getMetaData().put("referenceFormat", resourcePathFormat);

                if ("dash".equalsIgnoreCase(resourcePathFormat)) {
                    entry.getValue().getNodesRecursively().stream().filter(node -> node instanceof WorkItemNode)
                            .map(WorkItemNode.class::cast).forEach(wiNode -> {
                                String taskName = (String) wiNode.getWork().getParameter("TaskName");
                                if (taskName != null) {
                                    wiNode.getWork().setParameter("TaskName", StringUtils.toDashCase(taskName));
                                }
                            });
                } else if ("camel".equalsIgnoreCase(resourcePathFormat)) {
                    entry.getValue().getNodesRecursively().stream().filter(node -> node instanceof WorkItemNode)
                            .map(WorkItemNode.class::cast).forEach(wiNode -> {
                                String taskName = (String) wiNode.getWork().getParameter("TaskName");
                                if (taskName != null) {
                                    wiNode.getWork().setParameter("TaskName", StringUtils.toCamelCase(taskName));
                                }
                            });
                }
            }

            Set<String> classImports = ((io.automatiko.engine.workflow.process.core.WorkflowProcess) entry.getValue())
                    .getImports();
            if (classImports != null) {
                classImports = new HashSet<>();
                ((io.automatiko.engine.workflow.process.core.WorkflowProcess) entry.getValue()).setImports(classImports);
            }
            classImports.add(BaseFunctions.class.getCanonicalName());
            classImports.addAll(functions);

            try {
                ProcessMetaData generate = execModelGen.generate();
                processIdToMetadata.put(id, generate);
                processExecutableModelGenerators.add(execModelGen);

                context.addProcess(id, generate);
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage());
                throw new ProcessCodegenException(id, packageName, e);
            }
        }

        // generate Process, ProcessInstance classes and the REST resource
        for (ProcessExecutableModelGenerator execModelGen : processExecutableModelGenerators) {
            String classPrefix = StringUtils.capitalize(execModelGen.extractedProcessId());
            WorkflowProcess workFlowProcess = execModelGen.process();
            ModelClassGenerator modelClassGenerator = processIdToModelGenerator.get(execModelGen.getProcessId());

            ProcessGenerator p = new ProcessGenerator(context, workFlowProcess, execModelGen, classPrefix,
                    modelClassGenerator.className(), applicationCanonicalName,
                    processIdToUserTaskModel.get(execModelGen.getProcessId()), processIdToMetadata)
                            .withDependencyInjection(annotator)
                            .withPersistence(persistence);

            ProcessInstanceGenerator pi = new ProcessInstanceGenerator(workflowType, context(), execModelGen,
                    workFlowProcess.getPackageName(),
                    classPrefix, modelClassGenerator.generate());

            ProcessMetaData metaData = processIdToMetadata.get(execModelGen.getProcessId());
            if (isFunctionFlowProject()) {
                ffgs.add(new FunctionFlowGenerator(context(), workFlowProcess, modelClassGenerator.className(),
                        execModelGen.className(),
                        applicationCanonicalName).withDependencyInjection(annotator).withSignals(metaData.getSignals(),
                                metaData.getSignalNodes()).withTriggers(metaData.getTriggers()));

                if (metaData.getTriggers() != null) {

                    for (TriggerMetaData trigger : metaData.getTriggers()) {

                        if (trigger.getType().equals(TriggerMetaData.TriggerType.ProduceMessage)) {

                            MessageDataEventGenerator msgDataEventGenerator = new MessageDataEventGenerator(workFlowProcess,
                                    trigger).withDependencyInjection(annotator);
                            mdegs.add(msgDataEventGenerator);

                            mpgs.add(new MessageProducerGenerator(workflowType, context(), workFlowProcess,
                                    modelClassGenerator.className(), execModelGen.className(),
                                    msgDataEventGenerator.className(), trigger).withDependencyInjection(annotator));
                        }
                    }
                }
            } else if (isFunctionProject()) {
                fgs.add(new FunctionGenerator(context(), workFlowProcess, modelClassGenerator.className(),
                        execModelGen.className(),
                        applicationCanonicalName).withDependencyInjection(annotator));
            } else if (isServiceProject()) {
                if (isPublic(workFlowProcess)) {

                    // Creating and adding the ResourceGenerator
                    resourceGeneratorFactory
                            .create(context(), workFlowProcess, modelClassGenerator.className(), execModelGen.className(),
                                    applicationCanonicalName)
                            .map(r -> r.withDependencyInjection(annotator).withParentProcess(null).withPersistence(persistence)
                                    .withUserTasks(processIdToUserTaskModel.get(execModelGen.getProcessId()))
                                    .withPathPrefix("{id}").withSignals(metaData.getSignals(),
                                            metaData.getSignalNodes())
                                    .withTriggers(metaData.isStartable(), metaData.isDynamic())
                                    .withSubProcesses(populateSubprocesses(workFlowProcess,
                                            processIdToMetadata.get(execModelGen.getProcessId()), processIdToMetadata,
                                            processIdToModelGenerator, processExecutableModelGenerators,
                                            processIdToUserTaskModel)))
                            .ifPresent(rgs::add);

                    if (context.getBuildContext().isGraphQLSupported()) {
                        GraphQLResourceGenerator graphqlGenerator = new GraphQLResourceGenerator(context(), workFlowProcess,
                                modelClassGenerator.className(), execModelGen.className(),
                                applicationCanonicalName);

                        graphqlGenerator.withDependencyInjection(annotator).withParentProcess(null).withPersistence(persistence)
                                .withUserTasks(processIdToUserTaskModel.get(execModelGen.getProcessId()))
                                .withPathPrefix(CodegenUtils.version(workFlowProcess.getVersion()))
                                .withSignals(metaData.getSignals(),
                                        metaData.getSignalNodes())
                                .withTriggers(metaData.isStartable(), metaData.isDynamic())
                                .withSubProcesses(populateSubprocessesGraphQL(workFlowProcess,
                                        processIdToMetadata.get(execModelGen.getProcessId()), processIdToMetadata,
                                        processIdToModelGenerator, processExecutableModelGenerators,
                                        processIdToUserTaskModel));

                        grapggs.add(graphqlGenerator);
                    }
                }
                if (metaData.getTriggers() != null) {

                    for (TriggerMetaData trigger : metaData.getTriggers()) {

                        // generate message consumers for processes with message events
                        if (isPublic(workFlowProcess)
                                && trigger.getType().equals(TriggerMetaData.TriggerType.ConsumeMessage)) {

                            MessageDataEventGenerator msgDataEventGenerator = new MessageDataEventGenerator(workFlowProcess,
                                    trigger).withDependencyInjection(annotator);
                            mdegs.add(msgDataEventGenerator);

                            megs.add(new MessageConsumerGenerator(context(), workFlowProcess,
                                    modelClassGenerator.className(), execModelGen.className(), applicationCanonicalName,
                                    msgDataEventGenerator.className(), trigger).withDependencyInjection(annotator)
                                            .withPersistence(persistence));
                        } else if (trigger.getType().equals(TriggerMetaData.TriggerType.ProduceMessage)) {

                            MessageDataEventGenerator msgDataEventGenerator = new MessageDataEventGenerator(workFlowProcess,
                                    trigger).withDependencyInjection(annotator);
                            mdegs.add(msgDataEventGenerator);

                            mpgs.add(new MessageProducerGenerator(workflowType, context(), workFlowProcess,
                                    modelClassGenerator.className(), execModelGen.className(),
                                    msgDataEventGenerator.className(), trigger).withDependencyInjection(annotator));
                        }
                    }
                }
            }

            if (metaData.getOpenAPIs() != null) {

                for (OpenAPIMetaData api : metaData.getOpenAPIs()) {
                    OpenAPIClientGenerator oagenerator = new OpenAPIClientGenerator(context, workFlowProcess, api)
                            .withDependencyInjection(annotator);

                    opgs.add(oagenerator);
                }
            }
            moduleGenerator.addProcess(p);

            ps.add(p);
            pis.add(pi);
        }

        for (ModelClassGenerator modelClassGenerator : processIdToModelGenerator.values()) {
            ModelMetaData mmd = modelClassGenerator.generate();

            storeFile(Type.MODEL, modelClassGenerator.generatedFilePath(), mmd.generate(
                    annotator != null ? new String[] { "io.quarkus.runtime.annotations.RegisterForReflection" }
                            : new String[0]));
        }

        for (InputModelClassGenerator modelClassGenerator : processIdToInputModelGenerator.values()) {
            ModelMetaData mmd = modelClassGenerator.generate();
            storeFile(Type.MODEL, modelClassGenerator.generatedFilePath(), mmd.generate(
                    annotator != null ? new String[] { "io.quarkus.runtime.annotations.RegisterForReflection" }
                            : new String[0]));
        }

        for (OutputModelClassGenerator modelClassGenerator : processIdToOutputModelGenerator.values()) {
            ModelMetaData mmd = modelClassGenerator.generate();
            storeFile(Type.MODEL, modelClassGenerator.generatedFilePath(), mmd.generate(
                    annotator != null ? new String[] { "io.quarkus.runtime.annotations.RegisterForReflection" }
                            : new String[0]));
        }

        for (List<UserTaskModelMetaData> utmd : processIdToUserTaskModel.values()) {

            for (UserTaskModelMetaData ut : utmd) {
                storeFile(Type.MODEL, UserTasksModelClassGenerator.generatedFilePath(ut.getInputModelClassName()),
                        ut.generateInput());

                storeFile(Type.MODEL, UserTasksModelClassGenerator.generatedFilePath(ut.getOutputModelClassName()),
                        ut.generateOutput());
            }
        }

        for (AbstractResourceGenerator resourceGenerator : rgs) {
            storeFile(Type.REST, resourceGenerator.generatedFilePath(), resourceGenerator.generate());
        }

        for (AbstractResourceGenerator resourceGenerator : grapggs) {
            storeFile(Type.GRAPHQL, resourceGenerator.generatedFilePath(), resourceGenerator.generate());
        }

        for (FunctionGenerator functionGenerator : fgs) {
            storeFile(Type.FUNCTION, functionGenerator.generatedFilePath(), functionGenerator.generate());
        }

        for (FunctionFlowGenerator functionFlowGenerator : ffgs) {
            storeFile(Type.FUNCTION_FLOW, functionFlowGenerator.generatedFilePath(), functionFlowGenerator.generate());
        }

        for (MessageDataEventGenerator messageDataEventGenerator : mdegs) {
            storeFile(Type.CLASS, messageDataEventGenerator.generatedFilePath(), messageDataEventGenerator.generate());
        }

        for (MessageConsumerGenerator messageConsumerGenerator : megs) {
            storeFile(Type.MESSAGE_CONSUMER, messageConsumerGenerator.generatedFilePath(),
                    messageConsumerGenerator.generate());
        }

        for (MessageProducerGenerator messageProducerGenerator : mpgs) {
            storeFile(Type.MESSAGE_PRODUCER, messageProducerGenerator.generatedFilePath(),
                    messageProducerGenerator.generate());
        }

        for (OpenAPIClientGenerator openApiClientGenerator : opgs) {
            openApiClientGenerator.generate();

            Map<String, String> contents = openApiClientGenerator.generatedClasses();

            for (Entry<String, String> entry : contents.entrySet()) {

                storeFile(Type.CLASS, entry.getKey().replace('.', '/') + ".java", entry.getValue());
            }
        }

        for (ProcessGenerator p : ps) {
            storeFile(Type.PROCESS, p.generatedFilePath(), p.generate());

            p.getAdditionalClasses().forEach(cp -> {
                String packageName = cp.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
                String clazzName = cp.findFirst(ClassOrInterfaceDeclaration.class).map(cls -> cls.getName().toString())
                        .get();
                String path = (packageName + "." + clazzName).replace('.', '/') + ".java";
                storeFile(Type.CLASS, path, cp.toString());
            });
        }

        for (ProcessInstanceGenerator pi : pis) {
            storeFile(Type.PROCESS_INSTANCE, pi.generatedFilePath(), pi.generate());
        }

        for (ProcessExecutableModelGenerator processGenerator : processExecutableModelGenerators) {
            if (processGenerator.isPublic()) {
                publicProcesses.add(processGenerator.extractedProcessId());
            }
        }

        return generatedFiles;
    }

    @Override
    public void updateConfig(ConfigGenerator cfg) {
        if (!processes.isEmpty()) {
            cfg.withProcessConfig(new ProcessConfigGenerator());
        }
    }

    private void storeFile(Type type, String path, String source) {
        if (source != null) {
            generatedFiles.add(new GeneratedFile(type, path, log(source).getBytes(StandardCharsets.UTF_8)));
        }
    }

    public List<GeneratedFile> getGeneratedFiles() {
        return generatedFiles;
    }

    @Override
    public ApplicationSection section() {
        return moduleGenerator;
    }

    protected boolean isPublic(WorkflowProcess process) {
        return WorkflowProcess.PUBLIC_VISIBILITY.equalsIgnoreCase(process.getVisibility());
    }

    protected List<AbstractResourceGenerator> populateSubprocesses(WorkflowProcess parentProcess,
            ProcessMetaData metaData, Map<String, ProcessMetaData> processIdToMetadata,
            Map<String, ModelClassGenerator> processIdToModelGenerator,
            List<ProcessExecutableModelGenerator> processExecutableModelGenerators,
            Map<String, List<UserTaskModelMetaData>> processIdToUserTaskModel) {
        List<AbstractResourceGenerator> subprocesses = new ArrayList<AbstractResourceGenerator>();

        for (Entry<String, String> entry : metaData.getSubProcesses().entrySet()) {

            ProcessExecutableModelGenerator execModelGen = processExecutableModelGenerators.stream()
                    .filter(p -> p.getProcessId().equals(entry.getValue())).findFirst().orElse(null);

            if (execModelGen != null) {
                WorkflowProcess workFlowProcess = execModelGen.process();
                ModelClassGenerator modelClassGenerator = processIdToModelGenerator.get(entry.getValue());

                Optional.of(new SubprocessResourceGenerator(context(), workFlowProcess, modelClassGenerator.className(),
                        execModelGen.className(), applicationCanonicalName))
                        .map(r -> r.withDependencyInjection(annotator).withParentProcess(parentProcess)
                                .withUserTasks(processIdToUserTaskModel.get(execModelGen.getProcessId()))
                                .withSignals(processIdToMetadata.get(execModelGen.getProcessId()).getSignals(),
                                        metaData.getSignalNodes())
                                .withTriggers(processIdToMetadata.get(execModelGen.getProcessId()).isStartable(),
                                        processIdToMetadata.get(execModelGen.getProcessId()).isDynamic())
                                .withSubProcesses(populateSubprocesses(workFlowProcess,
                                        processIdToMetadata.get(execModelGen.getProcessId()), processIdToMetadata,
                                        processIdToModelGenerator, processExecutableModelGenerators,
                                        processIdToUserTaskModel)))
                        .ifPresent(subprocesses::add);
            }
        }

        return subprocesses;
    }

    protected List<AbstractResourceGenerator> populateSubprocessesGraphQL(WorkflowProcess parentProcess,
            ProcessMetaData metaData, Map<String, ProcessMetaData> processIdToMetadata,
            Map<String, ModelClassGenerator> processIdToModelGenerator,
            List<ProcessExecutableModelGenerator> processExecutableModelGenerators,
            Map<String, List<UserTaskModelMetaData>> processIdToUserTaskModel) {
        List<AbstractResourceGenerator> subprocesses = new ArrayList<AbstractResourceGenerator>();

        for (Entry<String, String> entry : metaData.getSubProcesses().entrySet()) {

            ProcessExecutableModelGenerator execModelGen = processExecutableModelGenerators.stream()
                    .filter(p -> p.getProcessId().equals(entry.getValue())).findFirst().orElse(null);

            if (execModelGen != null) {
                WorkflowProcess workFlowProcess = execModelGen.process();
                ModelClassGenerator modelClassGenerator = processIdToModelGenerator.get(entry.getValue());

                Optional.of(new SubprocessGraphQLResourceGenerator(context(), workFlowProcess, modelClassGenerator.className(),
                        execModelGen.className(), applicationCanonicalName))
                        .map(r -> r.withDependencyInjection(annotator).withParentProcess(parentProcess)
                                .withUserTasks(processIdToUserTaskModel.get(execModelGen.getProcessId()))
                                .withSignals(processIdToMetadata.get(execModelGen.getProcessId()).getSignals(),
                                        metaData.getSignalNodes())
                                .withTriggers(processIdToMetadata.get(execModelGen.getProcessId()).isStartable(),
                                        processIdToMetadata.get(execModelGen.getProcessId()).isDynamic())
                                .withSubProcesses(populateSubprocessesGraphQL(workFlowProcess,
                                        processIdToMetadata.get(execModelGen.getProcessId()), processIdToMetadata,
                                        processIdToModelGenerator, processExecutableModelGenerators,
                                        processIdToUserTaskModel)))
                        .ifPresent(subprocesses::add);
            }
        }

        return subprocesses;
    }
}
