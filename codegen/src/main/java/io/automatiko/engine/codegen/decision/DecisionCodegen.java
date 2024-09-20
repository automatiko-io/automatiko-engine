
package io.automatiko.engine.codegen.decision;

import static io.automatiko.engine.codegen.ApplicationGenerator.log;
import static io.automatiko.engine.services.utils.IoUtils.readBytesFromInputStream;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.kie.dmn.api.core.DMNRuntime;

import io.automatiko.engine.api.io.Resource;
import io.automatiko.engine.codegen.AbstractGenerator;
import io.automatiko.engine.codegen.ApplicationGenerator;
import io.automatiko.engine.codegen.ApplicationSection;
import io.automatiko.engine.codegen.ConfigGenerator;
import io.automatiko.engine.codegen.GeneratedFile;
import io.automatiko.engine.codegen.decision.config.DecisionConfigGenerator;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.decision.dmn.DmnRuntimeProvider;
import io.automatiko.engine.services.io.ByteArrayResource;
import io.automatiko.engine.services.io.FileSystemResource;
import io.automatiko.engine.services.io.InternalResource;

public class DecisionCodegen extends AbstractGenerator {

    public static DecisionCodegen ofJar(Path... jarPaths) throws IOException {
        return ofJar(Collections.emptyList(), jarPaths);
    }

    public static DecisionCodegen ofJar(List<String> dependencies, Path... jarPaths) throws IOException {
        List<DMNResource> dmnResources = new ArrayList<>();

        for (Path jarPath : jarPaths) {
            List<Resource> resources = new ArrayList<>();
            try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();

                    if (entry.getName().endsWith(".dmn")) {
                        InternalResource resource = new ByteArrayResource(
                                readBytesFromInputStream(zipFile.getInputStream(entry)));
                        resource.setSourcePath(entry.getName());
                        resources.add(resource);
                    }
                }
            }
            dmnResources.addAll(parseDecisions(jarPath, resources));
        }

        for (String dependency : dependencies) {
            List<Resource> resources = new ArrayList<>();
            try (ZipFile zipFile = new ZipFile(dependency)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();

                    if (entry.getName().endsWith(".dmn")) {
                        InternalResource resource = new ByteArrayResource(
                                readBytesFromInputStream(zipFile.getInputStream(entry)));
                        resource.setSourcePath(entry.getName());
                        resources.add(resource);
                    }
                }
            } catch (IOException e) {

            }
            dmnResources.addAll(parseDecisions(Paths.get(dependency), resources));
        }

        return ofDecisions(dmnResources);
    }

    public static DecisionCodegen ofPath(Path... paths) throws IOException {
        return ofPath(Collections.emptyList(), paths);
    }

    public static DecisionCodegen ofPath(List<String> dependencies, Path... paths) throws IOException {
        List<DMNResource> resources = new ArrayList<>();

        for (String dependency : dependencies) {
            List<Resource> dmnresources = new ArrayList<>();
            File file = new File(dependency);

            if (file.isDirectory()) {
                Path srcPath = file.toPath();
                if (Files.exists(srcPath)) {
                    try (Stream<Path> filesStream = Files.walk(srcPath)) {
                        List<File> files = filesStream.filter(p -> p.toString().endsWith(".dmn")).map(Path::toFile)
                                .collect(Collectors.toList());
                        resources.addAll(parseFiles(srcPath, files));
                    }
                }
            } else {

                try (ZipFile zipFile = new ZipFile(dependency)) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();

                        if (entry.getName().endsWith(".dmn")) {
                            InternalResource resource = new ByteArrayResource(
                                    readBytesFromInputStream(zipFile.getInputStream(entry)));
                            resource.setSourcePath(entry.getName());
                            dmnresources.add(resource);
                        }
                    }
                } catch (IOException e) {

                }
                resources.addAll(parseDecisions(Paths.get(dependency), dmnresources));
            }
        }

        for (Path path : paths) {
            Path srcPath = Paths.get(path.toString());
            if (Files.exists(srcPath)) {
                try (Stream<Path> filesStream = Files.walk(srcPath)) {
                    List<File> files = filesStream.filter(p -> p.toString().endsWith(".dmn")).map(Path::toFile)
                            .collect(Collectors.toList());
                    resources.addAll(parseFiles(srcPath, files));
                }
            }
        }
        return ofDecisions(resources);
    }

    public static DecisionCodegen ofFiles(Path basePath, List<File> files) throws IOException {
        return ofDecisions(parseFiles(basePath, files));
    }

    private static DecisionCodegen ofDecisions(List<DMNResource> resources) {
        return new DecisionCodegen(resources);
    }

    private static List<DMNResource> parseFiles(Path path, List<File> files) throws IOException {
        return parseDecisions(path, files.stream().map(FileSystemResource::new).collect(toList()));
    }

    private static List<DMNResource> parseDecisions(Path path, List<Resource> resources) throws IOException {
        DMNRuntime dmnRuntime = DmnRuntimeProvider.from(resources);
        return dmnRuntime.getModels().stream().map(model -> new DMNResource(model, path)).collect(toList());
    }

    private String packageName;
    private String applicationCanonicalName;
    private DependencyInjectionAnnotator annotator;

    private DecisionContainerGenerator moduleGenerator;

    private final List<DMNResource> resources;
    private final List<GeneratedFile> generatedFiles = new ArrayList<>();
    private boolean useMonitoring = false;

    public DecisionCodegen(List<DMNResource> resources) {
        this.resources = resources;

        // set default package name
        setPackageName(context == null ? ApplicationGenerator.DEFAULT_PACKAGE_NAME
                : context.getPackageName().orElse(ApplicationGenerator.DEFAULT_PACKAGE_NAME));
        this.moduleGenerator = new DecisionContainerGenerator(applicationCanonicalName, resources);
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
        this.applicationCanonicalName = packageName + ".Application";
    }

    public void setDependencyInjection(DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
    }

    public DecisionContainerGenerator moduleGenerator() {
        return moduleGenerator;
    }

    public List<GeneratedFile> generate() {
        if (resources.isEmpty()) {
            return Collections.emptyList();
        }

        return generatedFiles;
    }

    @Override
    public void updateConfig(ConfigGenerator cfg) {
        if (!resources.isEmpty()) {
            cfg.withDecisionConfig(new DecisionConfigGenerator());
        }
    }

    private void storeFile(GeneratedFile.Type type, String path, String source) {
        generatedFiles.add(new GeneratedFile(type, path, log(source).getBytes(StandardCharsets.UTF_8)));
    }

    public List<GeneratedFile> getGeneratedFiles() {
        return generatedFiles;
    }

    @Override
    public ApplicationSection section() {
        return moduleGenerator;
    }

    public DecisionCodegen withMonitoring(boolean useMonitoring) {
        this.useMonitoring = useMonitoring;
        return this;
    }
}