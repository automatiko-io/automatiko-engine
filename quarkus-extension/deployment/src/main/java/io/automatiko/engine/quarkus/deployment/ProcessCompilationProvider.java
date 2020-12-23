package io.automatiko.engine.quarkus.deployment;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.automatiko.engine.codegen.ApplicationGenerator;
import io.automatiko.engine.codegen.Generator;
import io.automatiko.engine.codegen.process.ProcessCodegen;

public class ProcessCompilationProvider extends AutomatikoCompilationProvider {

    @Override
    public Set<String> handledExtensions() {
        return new HashSet<>(asList(".bpmn", ".bpmn2", ".sw.json", ".sw.yml", ".sw.yaml", ".json", ".yml", ".yaml"));
    }

    @Override
    protected Set<File> filterFilesToCompile(Set<File> filesToCompile) {
        return filesToCompile.stream().filter(f -> f.getName().endsWith(".bpmn") ||
                f.getName().endsWith("bpmn2") ||
                f.getName().endsWith(".sw.json") ||
                f.getName().endsWith(".sw.yml") ||
                f.getName().endsWith(".sw.yaml")).collect(Collectors.toSet());
    }

    @Override
    protected Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
            throws IOException {
        return appGen.withGenerator(ProcessCodegen.ofFiles(new ArrayList<>(filesToCompile)))
                .withClassLoader(Thread.currentThread().getContextClassLoader());
    }
}
