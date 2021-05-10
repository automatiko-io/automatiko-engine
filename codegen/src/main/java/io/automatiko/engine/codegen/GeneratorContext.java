
package io.automatiko.engine.codegen;

import static com.github.javaparser.StaticJavaParser.parse;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import io.automatiko.engine.codegen.context.ApplicationBuildContext;
import io.automatiko.engine.workflow.compiler.canonical.ProcessMetaData;

public class GeneratorContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorContext.class);

    protected static final String APPLICATION_PROPERTIES_FILE_NAME = "application.properties";

    public static GeneratorContext ofResourcePath(File resourcePath, File classesPath) {
        Properties applicationProperties = new Properties();

        try (FileReader fileReader = new FileReader(new File(resourcePath, APPLICATION_PROPERTIES_FILE_NAME))) {
            applicationProperties.load(fileReader);
        } catch (IOException ioe) {
            LOGGER.debug("Unable to load '" + APPLICATION_PROPERTIES_FILE_NAME + "'.");
        }

        return new GeneratorContext(applicationProperties, resourcePath, classesPath);
    }

    private ApplicationBuildContext buildContext;

    private File resourcePath;
    private File classesPath;

    private Properties applicationProperties = new Properties();
    private Map<String, String> modifiedApplicationProperties = new LinkedHashMap<String, String>();

    private Map<String, ProcessMetaData> processes = new ConcurrentHashMap<String, ProcessMetaData>();

    private Map<Path, Path> classToSource = new HashMap<>();

    private List<String> instructions = new ArrayList<String>();

    protected GeneratorContext(Properties properties, File resourcePath, File classesPath) {
        this.applicationProperties = properties;
        this.resourcePath = resourcePath;
        this.classesPath = classesPath;
    }

    public GeneratorContext withBuildContext(ApplicationBuildContext buildContext) {
        this.buildContext = buildContext;
        return this;
    }

    public ApplicationBuildContext getBuildContext() {
        return this.buildContext;
    }

    public Optional<String> getApplicationProperty(String property) {
        return Optional.ofNullable(
                modifiedApplicationProperties.getOrDefault(property, applicationProperties.getProperty(property)));
    }

    public Collection<String> getApplicationProperties() {
        return applicationProperties.stringPropertyNames();
    }

    public void setApplicationProperty(String property, String value) {
        this.modifiedApplicationProperties.put(property, value);
    }

    public CompilationUnit write(String packageName) {

        CompilationUnit clazz = parse(
                this.getClass().getResourceAsStream("/class-templates/config/ConfigPropertiesTemplate.java"));

        clazz.setPackageDeclaration(packageName);
        ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class).get();

        BlockStmt constructorBody = new BlockStmt();

        // hide automatik api (e.g. process management api) from OpenAPI definition
        String includeAutomatikApi = applicationProperties.getProperty("quarkus.automatiko.include-automatiko-api");
        if (!"true".equalsIgnoreCase(includeAutomatikApi)) {
            modifiedApplicationProperties.put("mp.openapi.scan.exclude.classes",
                    "io.automatiko.engine.addons.process.management.ProcessInstanceManagementResource,io.automatiko.engine.addons.usertasks.management.UserTaskManagementResource");
        }

        for (Entry<String, String> entry : modifiedApplicationProperties.entrySet()) {
            if (!applicationProperties.containsKey(entry.getKey())) { // avoid overriding of defined properties

                MethodCallExpr putItem = new MethodCallExpr(new NameExpr("properties"), "put")
                        .addArgument(new StringLiteralExpr(entry.getKey()))
                        .addArgument(new StringLiteralExpr(entry.getValue()));

                constructorBody.addStatement(putItem);
            }
        }

        ConstructorDeclaration constructor = new ConstructorDeclaration().setName(template.getName().asString())
                .addModifier(Modifier.Keyword.PUBLIC).setBody(constructorBody);

        template.addMember(constructor);

        return clazz;
    }

    public void addClassToSourceMapping(Path clazz, Path source) {
        this.classToSource.put(clazz, source);
    }

    public Path getClassSource(Path classFilePath) {
        if (classToSource.containsKey(classFilePath)) {
            return classToSource.get(classFilePath);
        } else if (classFilePath.toString().contains("$")) {
            Path toplevelClass = Paths.get(classFilePath.toString().replaceAll("\\$[0-9].*\\.", "."));

            if (classToSource.containsKey(toplevelClass)) {
                return classToSource.get(toplevelClass);
            }
        }

        return null;
    }

    public void addProcess(String processId, ProcessMetaData processMetadata) {
        this.processes.put(processId, processMetadata);
    }

    public Set<File> collectConnectedFiles(Set<File> inputs) {

        Set<File> outcome = new LinkedHashSet<File>(inputs);
        for (File input : inputs) {

            Set<ProcessMetaData> relatedProcesses = this.processes.values().stream()
                    .filter(pm -> isTheSameResource(input, pm.getSource())).collect(Collectors.toSet());
            collectRelatedProcesses(outcome, relatedProcesses);

            for (ProcessMetaData metadata : relatedProcesses) {

                // last check if any of the processes is used as subprocess of any other process - find parent processes
                Set<ProcessMetaData> parentProcesses = relatedProcesses.stream()
                        .filter(pm -> pm.getSubProcesses() != null
                                && pm.getSubProcesses().containsKey(metadata.getExtractedProcessId()))
                        .collect(Collectors.toSet());

                collectRelatedProcesses(outcome, parentProcesses);
            }
        }

        return outcome;
    }

    protected void collectRelatedProcesses(Set<File> outcome, Set<ProcessMetaData> relatedProcesses) {

        // add all related process source files
        relatedProcesses.forEach(pm -> outcome.add(new File(pm.getSource())));
        // next check if any of the processes has subprocess      
        for (ProcessMetaData metadata : relatedProcesses) {
            if (metadata.getSubProcesses() != null && !metadata.getSubProcesses().isEmpty()) {

                Set<ProcessMetaData> subpocesses = metadata.getSubProcesses().entrySet().stream()
                        .map(entry -> this.processes.get(entry.getKey())).collect(Collectors.toSet());

                collectRelatedProcesses(outcome, subpocesses);
            }
        }

    }

    protected boolean isTheSameResource(File file, String targetPath) {
        String sourceFolders = "src" + File.separator + "main" + File.separator + "resources" + File.separator;
        String targetFolders = "target" + File.separator + "classes" + File.separator;

        String path = file.getAbsolutePath();
        if (path.contains(sourceFolders)) {
            path = path.split(sourceFolders)[1];
        } else if (path.contains(targetFolders)) {
            path = path.split(targetFolders)[1];
        }

        if (targetPath.contains(sourceFolders)) {
            targetPath = targetPath.split(sourceFolders)[1];
        } else if (targetPath.contains(targetFolders)) {
            targetPath = targetPath.split(targetFolders)[1];
        }

        return path.equals(targetPath);
    }

    public void addInstruction(String instruction) {
        this.instructions.add(instruction);
    }

    public List<String> getInstructions() {
        return this.instructions;
    }

    public void logInstructions() {
        if (!instructions.isEmpty()) {
            LOGGER.info("****************** Automatiko Instructions *********************");
            LOGGER.info("Following are set of information that can be useful down the line...");

            instructions.forEach(instruction -> LOGGER.info(instruction));

            LOGGER.info("***************************************************************");
        }
    }
}
