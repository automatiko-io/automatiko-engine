
package io.automatiko.engine.codegen.process;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatiko.engine.codegen.CodegenUtils.interpolateTypes;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.BodyDeclarationComparator;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.compiler.canonical.ProcessToExecModelGenerator;

public class FunctionGenerator {
    private final String relativePath;

    private final GeneratorContext context;
    private WorkflowProcess process;
    private final String functionClazzName;
    private String processId;
    private final String processName;
    private String version = "";
    private String dataClazzName;
    private String modelfqcn;
    private final String appCanonicalName;
    private final String processClazzName;
    private DependencyInjectionAnnotator annotator;

    public FunctionGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn,
            String processfqcn, String appCanonicalName) {
        this.context = context;
        this.process = process;
        this.processId = ProcessToExecModelGenerator.extractProcessId(process.getId(), null);
        this.processName = processId.substring(processId.lastIndexOf('.') + 1);
        if (process.getVersion() != null && !process.getVersion().trim().isEmpty()) {
            this.version = CodegenUtils.version(process.getVersion());
        }
        this.appCanonicalName = appCanonicalName;
        String classPrefix = StringUtils.capitalize(processName);
        this.functionClazzName = classPrefix + "Function" + version;
        this.relativePath = process.getPackageName().replace(".", "/") + "/" + functionClazzName + ".java";
        this.modelfqcn = modelfqcn + "Output";
        this.dataClazzName = modelfqcn.substring(modelfqcn.lastIndexOf('.') + 1);
        this.processClazzName = processfqcn;
    }

    public FunctionGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
        return this;
    }

    public String className() {
        return functionClazzName;
    }

    public String generatedFilePath() {
        return relativePath;
    }

    protected boolean useInjection() {
        return this.annotator != null;
    }

    public String generate() {
        CompilationUnit clazz = parse(
                this.getClass().getResourceAsStream("/class-templates/FunctionTemplate.java"));
        clazz.setPackageDeclaration(process.getPackageName());
        clazz.addImport(modelfqcn);

        ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new IllegalStateException("Cannot find the class in FunctionTemplate"));
        template.setName(functionClazzName);

        Map<String, String> typeInterpolations = new HashMap<>();
        typeInterpolations.put("$Clazz$", functionClazzName);
        typeInterpolations.put("$Type$", dataClazzName);
        template.findAll(ClassOrInterfaceType.class).forEach(cls -> interpolateTypes(cls, typeInterpolations));

        if (useInjection()) {
            template.findAll(FieldDeclaration.class, CodegenUtils::isProcessField)
                    .forEach(fd -> annotator.withNamedInjection(fd, processId + version));

            template.findAll(FieldDeclaration.class, CodegenUtils::isApplicationField)
                    .forEach(fd -> annotator.withInjection(fd));

            template.findAll(FieldDeclaration.class, CodegenUtils::isIdentitySupplierField)
                    .forEach(fd -> annotator.withInjection(fd));

            template.findFirst(MethodDeclaration.class, md -> md.isPublic()).ifPresent(md -> {
                annotator.withFunction(md);
                md.setName(processId);
            });
        }

        template.getMembers().sort(new BodyDeclarationComparator());
        return clazz.toString();
    }

}
