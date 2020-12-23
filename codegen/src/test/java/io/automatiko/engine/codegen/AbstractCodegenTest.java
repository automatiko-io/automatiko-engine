
package io.automatiko.engine.codegen;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.config.AutomatikoConfig;
import io.automatiko.engine.codegen.context.QuarkusApplicationBuildContext;
import io.automatiko.engine.codegen.decision.DecisionCodegen;
import io.automatiko.engine.codegen.process.ProcessCodegen;

public class AbstractCodegenTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCodegenTest.class);

    private ClassLoader classloader;
    private Path compilationOutcome;

    protected AutomatikoConfig config = new AutomatikoConfig();

    @AfterEach
    public void cleanup() throws IOException {
        if (compilationOutcome != null) {
            try (Stream<Path> walk = Files.walk(compilationOutcome)) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    protected Application generateCodeProcessesOnly(String... processes) throws Exception {
        return generateCode(Arrays.asList(processes), Collections.emptyList());
    }

    protected Application generateCodeRulesOnly(String... rules) throws Exception {
        return generateCode(Collections.emptyList(), Arrays.asList(rules), Collections.emptyList(),
                Collections.emptyList(), true);
    }

    protected Application generateCode(List<String> processResources, List<String> rulesResources) throws Exception {
        return generateCode(processResources, rulesResources, Collections.emptyList(), Collections.emptyList(), false);
    }

    protected Application generateCode(List<String> processResources, List<String> rulesResources,
            List<String> decisionResources, List<String> javaRulesResources, boolean hasRuleUnit) throws Exception {
        GeneratorContext context = GeneratorContext.ofResourcePath(new File("src/test/resources"),
                new File("target/classes"));

        // Testing based on Quarkus as Default
        context.withBuildContext(
                new QuarkusApplicationBuildContext(config, (className -> false), c -> Collections.emptyList()));

        ApplicationGenerator appGen = new ApplicationGenerator(this.getClass().getPackage().getName(),
                new File("target/codegen-tests")).withGeneratorContext(context).withRuleUnits(hasRuleUnit)
                        .withDependencyInjection(null);

        if (!processResources.isEmpty()) {
            appGen.withGenerator(ProcessCodegen.ofFiles(processResources.stream()
                    .map(resource -> new File("src/test/resources", resource)).collect(Collectors.toList())));
        }

        if (!decisionResources.isEmpty()) {
            appGen.withGenerator(
                    DecisionCodegen.ofFiles(Paths.get("src/test/resources").toAbsolutePath(), decisionResources.stream()
                            .map(resource -> new File("src/test/resources", resource)).collect(Collectors.toList())));
        }

        Collection<GeneratedFile> generatedFiles = appGen.generate();

        List<JavaFileObject> sources = new ArrayList<JavaFileObject>();

        for (GeneratedFile entry : generatedFiles) {
            String fileName = entry.relativePath();
            if (!fileName.endsWith(".java")) {
                continue;
            }
            sources.add(new SourceCode(fileName, new String(entry.contents())));
            logger.debug(new String(entry.contents()));
        }

        if (logger.isDebugEnabled()) {
            Path temp = Files.createTempDirectory("automatik-temp-dir");
            logger.debug("Dumping generated files in " + temp);
            for (GeneratedFile entry : generatedFiles) {
                Path fpath = temp.resolve(entry.relativePath());
                fpath.getParent().toFile().mkdirs();
                Files.write(fpath, entry.contents());
            }
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticsCollector, null, null);
        compilationOutcome = Files.createTempDirectory("compile-test-");
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(compilationOutcome.toFile()));

        CompilationTask task = compiler.getTask(null, fileManager, diagnosticsCollector, null, null, sources);
        boolean result = task.call();

        if (result) {
            classloader = new URLClassLoader(new URL[] { compilationOutcome.toUri().toURL() });

            @SuppressWarnings("unchecked")
            Class<Application> app = (Class<Application>) Class
                    .forName(this.getClass().getPackage().getName() + ".Application", true, classloader);

            Application application = app.getDeclaredConstructor().newInstance();
            app.getMethod("setup").invoke(application);
            return application;
        } else {
            List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticsCollector.getDiagnostics();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
                System.err.println(d);
            }

            throw new RuntimeException("Compilation failed");
        }
    }

    protected ClassLoader testClassLoader() {
        return classloader;
    }

    protected void log(String content) {
        logger.debug(content);
    }

    private static class SourceCode extends SimpleJavaFileObject {

        private String contents = null;

        public SourceCode(String className, String contents) throws Exception {
            super(new URI(className), Kind.SOURCE);
            this.contents = contents;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return contents;
        }

    }
}
