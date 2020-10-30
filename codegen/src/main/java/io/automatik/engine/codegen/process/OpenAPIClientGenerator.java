
package io.automatik.engine.codegen.process;

import static io.automatik.engine.codegen.CodeGenConstants.MP_RESTCLIENT_PROP_ACCESS_TOKEN;
import static io.automatik.engine.codegen.CodeGenConstants.MP_RESTCLIENT_PROP_AUTH_TYPE;
import static io.automatik.engine.codegen.CodeGenConstants.MP_RESTCLIENT_PROP_BASIC;
import static io.automatik.engine.codegen.CodeGenConstants.MP_RESTCLIENT_PROP_CLIENT_ID;
import static io.automatik.engine.codegen.CodeGenConstants.MP_RESTCLIENT_PROP_CLIENT_SECRET;
import static io.automatik.engine.codegen.CodeGenConstants.MP_RESTCLIENT_PROP_PASSWORD;
import static io.automatik.engine.codegen.CodeGenConstants.MP_RESTCLIENT_PROP_REFRESH_TOKEN;
import static io.automatik.engine.codegen.CodeGenConstants.MP_RESTCLIENT_PROP_REFRESH_URL;
import static io.automatik.engine.codegen.CodeGenConstants.MP_RESTCLIENT_PROP_URL;
import static io.automatik.engine.codegen.CodeGenConstants.MP_RESTCLIENT_PROP_USER;
import static org.openapitools.codegen.utils.StringUtils.camelize;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen;
import org.openapitools.codegen.templating.mustache.LowercaseLambda;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.codegen.GeneratorContext;
import io.automatik.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatik.engine.services.utils.StringUtils;
import io.automatik.engine.workflow.compiler.canonical.OpenAPIMetaData;

public class OpenAPIClientGenerator {

    private final String relativePath;

    private GeneratorContext context;

    private WorkflowProcess process;
    private final String packageName;
    private final String resourceClazzName;
    private DependencyInjectionAnnotator annotator;

    private OpenAPIMetaData openApiMetadata;

    private Map<String, String> generatedContent = new HashMap<String, String>();

    private Set<String> usedTypes = new LinkedHashSet<String>();

    private JavaJAXRSSpecServerCodegen codegen = new JavaJAXRSSpecServerCodegen() {

        @Override
        public String toApiName(String name) {
            String computed = sanitizeName(openApiMetadata.name());
            return camelize(computed);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> objs, List<Object> allModels) {

            Map<String, Object> data = super.postProcessOperationsWithModels(objs, allModels);

            Map<String, Object> operations = (Map<String, Object>) data.getOrDefault("operations", Collections.emptyMap());

            Collection<CodegenOperation> operation = (Collection<CodegenOperation>) operations.getOrDefault("operation",
                    Collections.emptyMap());

            Iterator<CodegenOperation> it = operation.iterator();
            while (it.hasNext()) {
                CodegenOperation codegenOperation = (CodegenOperation) it.next();
                if (!openApiMetadata.operations().contains(codegenOperation.operationId)) {
                    it.remove();
                } else {
                    codegenOperation.allParams.forEach(p -> usedTypes.add("io.automatik.engine.app.rest.model." + p.dataType));
                    usedTypes.add("io.automatik.engine.app.rest.model." + codegenOperation.returnType);
                }
            }

            return data;
        }

    };

    public OpenAPIClientGenerator(GeneratorContext context, WorkflowProcess process, OpenAPIMetaData openApiMetadata) {
        this.context = context;
        this.process = process;
        this.openApiMetadata = openApiMetadata;
        this.packageName = "io.automatik.engine.app.rest";
        this.resourceClazzName = StringUtils.capitalize(codegen.toApiName(openApiMetadata.name()));
        this.relativePath = packageName.replace(".", "/") + "/" + resourceClazzName + ".java";

        usedTypes.add(packageName + "." + resourceClazzName);
    }

    public OpenAPIClientGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
        return this;
    }

    public String className() {
        return resourceClazzName;
    }

    public String generatedFilePath() {
        return relativePath;
    }

    protected boolean useInjection() {
        return this.annotator != null;
    }

    public String generate() {

        if (openApiMetadata.api().getServers() != null && !openApiMetadata.api().getServers().isEmpty()) {

            context.setApplicationProperty(resourceClazzName.toLowerCase() + MP_RESTCLIENT_PROP_URL,
                    openApiMetadata.api().getServers().get(0).getUrl());

            context.addInstruction("Set '" + resourceClazzName.toLowerCase() + MP_RESTCLIENT_PROP_URL
                    + "' property to change defaut location (" + openApiMetadata.api().getServers().get(0).getUrl()
                    + ") of the service");

            context.addInstruction("In case authorization is required use following:");
            context.addInstruction("For basic auth:");
            context.addInstruction("    Set auth type via property '" + resourceClazzName.toLowerCase()
                    + MP_RESTCLIENT_PROP_AUTH_TYPE + "'  to 'basic'");
            context.addInstruction("    Then one of the following:");
            context.addInstruction("    Set user name and password with properties '" + resourceClazzName.toLowerCase()
                    + MP_RESTCLIENT_PROP_USER + "', '" + resourceClazzName.toLowerCase() + MP_RESTCLIENT_PROP_PASSWORD + "'");
            context.addInstruction("    Set base64 encoded username and password with property '"
                    + resourceClazzName.toLowerCase() + MP_RESTCLIENT_PROP_BASIC + "'");
            context.addInstruction("For OAuth2 auth:");
            context.addInstruction("    Set auth type via property '" + resourceClazzName.toLowerCase()
                    + MP_RESTCLIENT_PROP_AUTH_TYPE + "'  to 'oauth'");
            context.addInstruction("    Then depending on your OAuth configuration:");
            context.addInstruction("    Set access token type via property '" + resourceClazzName.toLowerCase()
                    + MP_RESTCLIENT_PROP_ACCESS_TOKEN);
            context.addInstruction(
                    "    Set client id type via property '" + resourceClazzName.toLowerCase() + MP_RESTCLIENT_PROP_CLIENT_ID);
            context.addInstruction("    Set client secret type via property '" + resourceClazzName.toLowerCase()
                    + MP_RESTCLIENT_PROP_CLIENT_SECRET);
            context.addInstruction("    Set refresh token type via property '" + resourceClazzName.toLowerCase()
                    + MP_RESTCLIENT_PROP_REFRESH_TOKEN);
            context.addInstruction("    Set refresh url type via property '" + resourceClazzName.toLowerCase()
                    + MP_RESTCLIENT_PROP_REFRESH_URL);
        }

        codegen.setOutputDir("");
        codegen.additionalProperties().put(JavaJAXRSSpecServerCodegen.INTERFACE_ONLY, true);
        codegen.additionalProperties().put(JavaJAXRSSpecServerCodegen.USE_BEANVALIDATION, false);
        codegen.additionalProperties().put(JavaJAXRSSpecServerCodegen.USE_SWAGGER_ANNOTATIONS, false);
        codegen.additionalProperties().put(JavaJAXRSSpecServerCodegen.GENERATE_POM, false);
        codegen.additionalProperties().put("lowercase", new LowercaseLambda());
        codegen.setTemplateDir("open-api-templates");
        codegen.setApiPackage("io.automatik.engine.app.rest");
        codegen.setModelPackage("io.automatik.engine.app.rest.model");
        codegen.setSourceFolder("");

        ClientOptInput input = new ClientOptInput().openAPI(openApiMetadata.api()).config(codegen);

        DefaultGenerator generator = new DefaultGenerator(false) {

            @Override
            public File writeToFile(String filename, String contents) throws IOException {
                if (filename.endsWith(".java")) {
                    String name = filename.substring(1, filename.lastIndexOf(".")).replaceAll("/", ".");

                    generatedContent.compute(name, (n, c) -> {
                        if (c == null) {

                            CompilationUnit unit = com.github.javaparser.StaticJavaParser.parse(contents);
                            ClassOrInterfaceDeclaration template = unit.findFirst(ClassOrInterfaceDeclaration.class)
                                    .get();
                            if (unit.getPackageDeclaration().get().getNameAsString().equals("io.automatik.engine.app.rest")) {
                                // add wildcard import to all model classes generated
                                unit.addImport("io.automatik.engine.app.rest.model.*");
                            } else if (unit.getPackageDeclaration().get().getNameAsString()
                                    .equals("io.automatik.engine.app.rest.model")) {
                                // find all import definitions that reference other model classes and add them to used types
                                unit.getImports().stream()
                                        .filter(i -> i.getNameAsString().startsWith("io.automatik.engine.app.rest.model"))
                                        .forEach(i -> usedTypes.add(i.getNameAsString()));
                            }

                            Optional<AnnotationExpr> p = template.getAnnotationByName("Path");

                            if (p.isPresent()) {
                                SingleMemberAnnotationExpr pathannotation = ((SingleMemberAnnotationExpr) p.get());
                                final String path = pathannotation.getMemberValue().asStringLiteralExpr().getValue();
                                template.findAll(MethodDeclaration.class).stream()
                                        .filter(md -> !md.getNameAsString().equals("authorizationHeader"))
                                        .forEach(md -> {

                                            Optional<AnnotationExpr> pathAnotation = md.getAnnotationByName("Path");
                                            if (pathAnotation.isPresent()) {

                                                String mpath = ((SingleMemberAnnotationExpr) pathAnotation.get())
                                                        .getMemberValue().asStringLiteralExpr().getValue();
                                                ((SingleMemberAnnotationExpr) pathAnotation.get())
                                                        .setMemberValue(new StringLiteralExpr(path + mpath));
                                            } else {
                                                md.addAnnotation(new SingleMemberAnnotationExpr(new Name("Path"),
                                                        new StringLiteralExpr(path)));
                                            }

                                        });
                                pathannotation.setMemberValue(new StringLiteralExpr("/"));
                            }

                            return unit.toString();
                        }

                        CompilationUnit unit = com.github.javaparser.StaticJavaParser.parse(c);
                        ClassOrInterfaceDeclaration template = unit.findFirst(ClassOrInterfaceDeclaration.class).get();

                        CompilationUnit newunit = com.github.javaparser.StaticJavaParser.parse(contents);
                        ClassOrInterfaceDeclaration newtemplate = newunit.findFirst(ClassOrInterfaceDeclaration.class)
                                .get();
                        final String path = ((SingleMemberAnnotationExpr) newtemplate.getAnnotationByName("Path").get())
                                .getMemberValue().asStringLiteralExpr().getValue();

                        List<MethodDeclaration> declarations = newtemplate.findAll(MethodDeclaration.class,
                                md -> !md.getNameAsString().equals("authorizationHeader"));
                        declarations.stream().forEach(md -> {
                            MethodDeclaration cloned = md.clone();
                            Optional<AnnotationExpr> pathAnotation = cloned.getAnnotationByName("Path");
                            if (pathAnotation.isPresent()) {

                                String mpath = ((SingleMemberAnnotationExpr) pathAnotation.get())
                                        .getMemberValue().asStringLiteralExpr().getValue();
                                ((SingleMemberAnnotationExpr) pathAnotation.get())
                                        .setMemberValue(new StringLiteralExpr(path + mpath));
                            } else {
                                cloned.addAnnotation(new SingleMemberAnnotationExpr(new Name("Path"),
                                        new StringLiteralExpr(path)));
                            }

                            template.addMember(cloned);
                        });

                        return unit.toString();

                    });
                }

                return new File(filename);
            }

        };
        generator.opts(input).generate();

        return null;
    }

    public Map<String, String> generatedClasses() {

        return generatedContent.entrySet().stream().filter(e -> usedTypes.contains(e.getKey()))
                .collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((resourceClazzName == null) ? 0 : resourceClazzName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OpenAPIClientGenerator other = (OpenAPIClientGenerator) obj;
        if (resourceClazzName == null) {
            if (other.resourceClazzName != null)
                return false;
        } else if (!resourceClazzName.equals(other.resourceClazzName))
            return false;
        return true;
    }

}
