package io.automatiko.engine.quarkus.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.automatiko.engine.codegen.ApplicationGenerator;
import io.automatiko.engine.codegen.GeneratedFile;
import io.automatiko.engine.codegen.Generator;
import io.automatiko.engine.codegen.di.CDIDependencyInjectionAnnotator;
import io.quarkus.deployment.dev.JavaCompilationProvider;

public abstract class AutomatikoCompilationProvider extends JavaCompilationProvider {

    @Override
    public Set<String> handledSourcePaths() {
        return Collections.singleton("src" + File.separator + "main" + File.separator + "resources");
    }

    protected Set<File> filterFilesToCompile(Set<File> filesToCompile) {
        return filesToCompile;
    }

    @Override
    public final void compile(Set<File> filesToCompile, Context context) {

        Set<File> allFiles = AutomatikoBuildData.get().getGenerationContext()
                .collectConnectedFiles(filterFilesToCompile(filesToCompile));

        if (allFiles.isEmpty()) {
            return;
        }

        File outputDirectory = context.getOutputDirectory();
        try {

            ApplicationGenerator appGen = new ApplicationGenerator(
                    AutomatikoBuildData.get().getConfig().packageName().orElse(AutomatikoQuarkusProcessor.DEFAULT_PACKAGE_NAME),
                    outputDirectory)
                            .withDependencyInjection(new CDIDependencyInjectionAnnotator())
                            .withGeneratorContext(AutomatikoBuildData.get().getGenerationContext());

            addGenerator(appGen, allFiles, context);

            Collection<GeneratedFile> generatedFiles = appGen.generate();

            Set<File> generatedSourceFiles = new HashSet<>();
            for (GeneratedFile file : generatedFiles) {
                Path path = pathOf(outputDirectory.getPath(), file.relativePath());
                if (file.getType() != GeneratedFile.Type.APPLICATION
                        && file.getType() != GeneratedFile.Type.APPLICATION_CONFIG) {
                    Files.write(path, file.contents());
                    generatedSourceFiles.add(path.toFile());
                }
            }
            super.compile(generatedSourceFiles, context);

        } catch (IOException e) {
            throw new AutomatikoCompilerException(e);
        }
    }

    @Override
    public Path getSourcePath(Path classFilePath, Set<String> sourcePaths, String classesPath) {
        try {
            return AutomatikoBuildData.get().getGenerationContext().getClassSource(classFilePath);
        } catch (IllegalStateException e) {
            return null;
        }

    }

    protected abstract Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
            throws IOException;

    static Path pathOf(String path, String relativePath) {
        Path p = Paths.get(path, relativePath);
        p.getParent().toFile().mkdirs();
        return p;
    }

}
