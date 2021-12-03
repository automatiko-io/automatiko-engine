
package io.automatiko.engine.workflow.compiler.canonical;

import static com.github.javaparser.StaticJavaParser.parse;
import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import io.automatiko.engine.api.codegen.Generated;
import io.automatiko.engine.api.codegen.VariableInfo;
import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.api.workflow.datatype.DataType;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;

public class ModelMetaData {

    private final String workflowType;
    private final String processId;
    private final String version;
    private final String packageName;
    private final String modelClassSimpleName;
    private final VariableDeclarations variableScope;
    private String modelClassName;
    private String visibility;
    private boolean hidden;
    private String templateName;

    private boolean supportsValidation;

    private boolean asEntity;
    private boolean removeAtCompletion;

    private boolean supportsOpenApi;

    private String name;
    private String description;

    private List<Consumer<CompilationUnit>> augmentors = new ArrayList<>();

    public ModelMetaData(String workflowType, String processId, String version, String packageName, String modelClassSimpleName,
            String visibility, VariableDeclarations variableScope, boolean hidden, String name, String description) {
        this(workflowType, processId, version, packageName, modelClassSimpleName, visibility, variableScope, hidden,
                "/class-templates/ModelTemplate.java", name, description);
    }

    public ModelMetaData(String workflowType, String processId, String version, String packageName, String modelClassSimpleName,
            String visibility, VariableDeclarations variableScope, boolean hidden, String templateName,
            String name, String description) {
        this.workflowType = workflowType;
        this.processId = processId;
        this.version = version;
        this.packageName = packageName;
        this.modelClassSimpleName = modelClassSimpleName;
        this.variableScope = variableScope;
        this.modelClassName = packageName + '.' + modelClassSimpleName;
        this.visibility = visibility;
        this.hidden = hidden;
        this.templateName = templateName;
        this.name = name;
        this.description = description;
    }

    public String generate(String... annotations) {
        CompilationUnit modelClass = compilationUnit(annotations);
        return modelClass.toString();
    }

    public AssignExpr newInstance(String assignVarName) {
        ClassOrInterfaceType type = new ClassOrInterfaceType(null, modelClassSimpleName);
        return new AssignExpr(new VariableDeclarationExpr(type, assignVarName), new ObjectCreationExpr().setType(type),
                AssignExpr.Operator.ASSIGN);
    }

    public MethodCallExpr fromMap(String variableName, String mapVarName) {

        return new MethodCallExpr(new NameExpr(variableName), "fromMap")
                .addArgument(new MethodCallExpr(new ThisExpr(), "id")).addArgument(mapVarName);
    }

    public MethodCallExpr toMap(String varName) {
        return new MethodCallExpr(new NameExpr(varName), "toMap");
    }

    public BlockStmt copyInto(String sourceVarName, String destVarName, ModelMetaData dest,
            Map<String, String> mapping) {
        BlockStmt blockStmt = new BlockStmt();

        for (Map.Entry<String, String> e : mapping.entrySet()) {
            String destField = variableScope.getTypes().get(e.getKey()).getSanitizedName();
            String sourceField = e.getValue();
            blockStmt
                    .addStatement(dest.callSetter(destVarName, destField, dest.callGetter(sourceVarName, sourceField)));
        }

        return blockStmt;
    }

    public MethodCallExpr callSetter(String targetVar, String destField, String value) {
        if (value.startsWith("#{")) {
            value = value.substring(2, value.length() - 1);
        }

        return callSetter(targetVar, destField, new NameExpr(value));
    }

    public MethodCallExpr callSetter(String targetVar, String destField, Expression value) {
        String name = variableScope.getTypes().get(destField).getSanitizedName();
        String type = variableScope.getType(destField);
        String setter = "set" + StringUtils.capitalize(name); // todo cache FieldDeclarations in compilationUnit()
        return new MethodCallExpr(new NameExpr(targetVar), setter)
                .addArgument(new CastExpr(new ClassOrInterfaceType(null, type), new EnclosedExpr(value)));
    }

    public MethodCallExpr callGetter(String targetVar, String field) {
        String getter = "get" + StringUtils.capitalize(field); // todo cache FieldDeclarations in compilationUnit()
        return new MethodCallExpr(new NameExpr(targetVar), getter);
    }

    private CompilationUnit compilationUnit(String... annotations) {
        CompilationUnit compilationUnit = parse(this.getClass().getResourceAsStream(templateName));
        compilationUnit.setPackageDeclaration(packageName);
        Optional<ClassOrInterfaceDeclaration> processMethod = compilationUnit
                .findFirst(ClassOrInterfaceDeclaration.class, sl1 -> true);

        if (!processMethod.isPresent()) {
            throw new NoSuchElementException("Cannot find class declaration in the template");
        }
        ClassOrInterfaceDeclaration modelClass = processMethod.get();

        for (String annotation : annotations) {
            modelClass.addAnnotation(annotation);
        }

        if (asEntity) {
            modelClass.addExtendedType("io.automatiko.engine.addons.persistence.db.model.ProcessInstanceEntity");
            modelClass.addAnnotation(new NormalAnnotationExpr(new Name("javax.persistence.Entity"),
                    NodeList.nodeList(new MemberValuePair("name",
                            new StringLiteralExpr(camelToSnake(processId.toUpperCase() + version(version).toUpperCase()))))));

            modelClass.findAll(FieldDeclaration.class, fd -> fd.getVariable(0).getNameAsString().equals("metadata"))
                    .forEach(fd -> fd.addAnnotation("javax.persistence.Transient"));
        }

        if (supportsOpenApi) {
            modelClass.addAnnotation(
                    new NormalAnnotationExpr(new Name("org.eclipse.microprofile.openapi.annotations.media.Schema"),
                            NodeList.nodeList(new MemberValuePair("name", new StringLiteralExpr(name.replaceAll("\\s", ""))),
                                    new MemberValuePair("description", new StringLiteralExpr(description)))));
        }
        if (!WorkflowProcess.PRIVATE_VISIBILITY.equals(visibility)) {
            modelClass.addAnnotation(new NormalAnnotationExpr(new Name(Generated.class.getCanonicalName()),
                    NodeList.nodeList(new MemberValuePair("value", new StringLiteralExpr("automatik-codegen")),
                            new MemberValuePair("reference", new StringLiteralExpr(processId)),
                            new MemberValuePair("name",
                                    new StringLiteralExpr(StringUtils.capitalize(
                                            ProcessToExecModelGenerator.extractProcessId(processId, version)))),
                            new MemberValuePair("hidden", new BooleanLiteralExpr(hidden)))));
        }
        modelClass.setName(modelClassSimpleName);
        modelClass.getConstructors().forEach(c -> c.setName(modelClass.getName()));
        modelClass.findAll(MethodDeclaration.class, md -> md.getNameAsString().equals("build"))
                .forEach(md -> {

                    md.getBody().get().findAll(SimpleName.class).stream().findFirst().ifPresent(o -> {
                        o.setIdentifier(modelClassSimpleName);
                    });
                    md.toString();
                });
        modelClass.findAll(AnnotationExpr.class, ae -> ae.getNameAsString().equals("JsonDeserialize"))
                .forEach(ae -> {
                    ae.findAll(ClassOrInterfaceType.class, cit -> cit.getNameAsString().contains("$TYPE$")).stream().findFirst()
                            .ifPresent(
                                    cit -> cit.setName(cit.getNameAsString().replaceAll("\\$TYPE\\$", modelClassSimpleName)));
                    ae.toString();
                });

        // setup of the toMap method body
        BlockStmt toMapBody = new BlockStmt();
        ClassOrInterfaceType toMap = new ClassOrInterfaceType(null, new SimpleName(Map.class.getSimpleName()),
                NodeList.nodeList(new ClassOrInterfaceType(null, String.class.getSimpleName()),
                        new ClassOrInterfaceType(null, Object.class.getSimpleName())));
        VariableDeclarationExpr paramsField = new VariableDeclarationExpr(toMap, "params");
        toMapBody
                .addStatement(new AssignExpr(
                        paramsField, new ObjectCreationExpr(null,
                                new ClassOrInterfaceType(null, new SimpleName(HashMap.class.getSimpleName()),
                                        NodeList.nodeList(new ClassOrInterfaceType(null, String.class.getSimpleName()),
                                                new ClassOrInterfaceType(null, Object.class.getSimpleName()))),
                                NodeList.nodeList()),
                        AssignExpr.Operator.ASSIGN));

        // setup of static fromMap method body
        BlockStmt staticFromMap = new BlockStmt();

        if (modelClass.findFirst(MethodDeclaration.class, md -> md.getNameAsString().equals("getId")).isPresent()) {
            FieldAccessExpr idField = new FieldAccessExpr(new ThisExpr(), "id");
            staticFromMap.addStatement(new AssignExpr(idField, new NameExpr("id"), AssignExpr.Operator.ASSIGN));
        }

        for (Map.Entry<String, Variable> variable : variableScope.getTypes().entrySet()) {
            String varName = variable.getValue().getName();
            String vtype = variable.getValue().getType().getStringType();
            String sanitizedName = variable.getValue().getSanitizedName();

            FieldDeclaration fd = declareField(sanitizedName, vtype);
            modelClass.addMember(fd);

            List<String> tags = variable.getValue().getTags();
            fd.addAnnotation(new NormalAnnotationExpr(new Name(VariableInfo.class.getCanonicalName()),
                    NodeList.nodeList(new MemberValuePair("tags",
                            new StringLiteralExpr(tags.stream().collect(Collectors.joining(",")))))));
            fd.addAnnotation(new NormalAnnotationExpr(new Name(JsonProperty.class.getCanonicalName()),
                    NodeList.nodeList(new MemberValuePair("value", new StringLiteralExpr(varName)))));

            if (asEntity && tags.contains(Variable.TRANSIENT_TAG)) {
                fd.addAnnotation("javax.persistence.Transient");
            }

            if (supportsOpenApi) {
                fd.addAnnotation(new NormalAnnotationExpr(new Name("org.eclipse.microprofile.openapi.annotations.media.Schema"),
                        NodeList.nodeList(new MemberValuePair("name",
                                new StringLiteralExpr(sanitizedName)),
                                new MemberValuePair("description",
                                        new StringLiteralExpr(
                                                getOrDefault((String) variable.getValue().getMetaData("Documentation"),
                                                        ""))))));
            }
            applyValidation(fd, tags);
            applyPersistence(variable.getValue().getType(), fd, tags);

            fd.createGetter();
            fd.createSetter();

            // toMap method body
            MethodCallExpr putVariable = new MethodCallExpr(new NameExpr("params"), "put");
            putVariable.addArgument(new StringLiteralExpr(varName));
            putVariable.addArgument(new FieldAccessExpr(new ThisExpr(), sanitizedName));
            toMapBody.addStatement(putVariable);

            ClassOrInterfaceType type = parseClassOrInterfaceType(vtype);

            // from map instance method body
            FieldAccessExpr instanceField = new FieldAccessExpr(new ThisExpr(), sanitizedName);
            staticFromMap.addStatement(new AssignExpr(instanceField, new CastExpr(type,
                    new MethodCallExpr(new NameExpr("params"), "get").addArgument(new StringLiteralExpr(varName))),
                    AssignExpr.Operator.ASSIGN));
        }

        Optional<MethodDeclaration> toMapMethod = modelClass.findFirst(MethodDeclaration.class,
                sl -> sl.getName().asString().equals("toMap"));

        toMapBody.addStatement(new ReturnStmt(new NameExpr("params")));
        toMapMethod.ifPresent(methodDeclaration -> methodDeclaration.setBody(toMapBody));

        modelClass
                .findFirst(MethodDeclaration.class,
                        sl -> sl.getName().asString().equals("fromMap") && sl.getParameters().size() == 2)// make sure
                // to take
                // only the
                // method
                // with two
                // parameters
                // (id and
                // params)
                .ifPresent(m -> m.setBody(staticFromMap));

        for (Consumer<CompilationUnit> augmentor : augmentors) {
            augmentor.accept(compilationUnit);
        }

        if (!workflowType.equals(Process.WORKFLOW_TYPE)) {
            // remove metadata field and it's getters/setters
            modelClass.findFirst(FieldDeclaration.class, fd -> fd.getVariable(0).getNameAsString().equals("metadata"))
                    .ifPresent(fd -> fd.removeForced());
            modelClass
                    .findAll(MethodDeclaration.class,
                            md -> md.getNameAsString().equals("getMetadata") || md.getNameAsString().equals("setMetadata"))
                    .forEach(md -> md.removeForced());
        }

        return compilationUnit;
    }

    private void applyValidation(FieldDeclaration fd, List<String> tags) {

        if (supportsValidation) {
            fd.addAnnotation("javax.validation.Valid");

            if (tags != null && tags.contains(Variable.REQUIRED_TAG)) {
                fd.addAnnotation("javax.validation.constraints.NotNull");
            }
        }
    }

    private void applyPersistence(DataType dataType, FieldDeclaration fd, List<String> tags) {

        if (asEntity) {

            NodeList<Expression> cascade;
            if (removeAtCompletion) {
                cascade = NodeList.nodeList(
                        new NameExpr("javax.persistence.CascadeType.ALL"));
            } else {
                cascade = NodeList.nodeList(
                        new NameExpr("javax.persistence.CascadeType.PERSIST"),
                        new NameExpr("javax.persistence.CascadeType.MERGE"),
                        new NameExpr("javax.persistence.CascadeType.REFRESH"),
                        new NameExpr("javax.persistence.CascadeType.DETACH"));
            }

            Type type = fd.getVariable(0).getType();
            if (type.isArrayType() || Collection.class.isAssignableFrom(dataType.getClassType())) {
                fd.addAnnotation(new NormalAnnotationExpr(new Name("javax.persistence.OneToMany"),
                        NodeList.nodeList(
                                new MemberValuePair("cascade",
                                        new ArrayInitializerExpr(cascade)),
                                new MemberValuePair("fetch", new NameExpr("javax.persistence.FetchType.EAGER")))));

            } else if (dataType.getClassType() != null && Stream.of(dataType.getClassType().getAnnotations())
                    .anyMatch(a -> a.annotationType().getName().equals("javax.persistence.Entity"))) {
                fd.addAnnotation(new NormalAnnotationExpr(new Name("javax.persistence.OneToOne"),
                        NodeList.nodeList(
                                new MemberValuePair("cascade",
                                        new ArrayInitializerExpr(cascade)),
                                new MemberValuePair("fetch", new NameExpr("javax.persistence.FetchType.EAGER")))));
                fd.addAnnotation(new NormalAnnotationExpr(new Name("javax.persistence.JoinColumn"),
                        NodeList.nodeList()));
            } else {
                fd.addAnnotation(new NormalAnnotationExpr(new Name("javax.persistence.Column"),
                        NodeList.nodeList(new MemberValuePair("name",
                                new StringLiteralExpr(camelToSnake(fd.getVariable(0).getNameAsString().toUpperCase()))))));
            }
        }
    }

    private FieldDeclaration declareField(String name, String type) {
        return new FieldDeclaration().addVariable(new VariableDeclarator().setType(type).setName(name))
                .addModifier(Modifier.Keyword.PRIVATE);
    }

    public String getModelClassSimpleName() {
        return modelClassSimpleName;
    }

    public String getModelClassName() {
        return modelClassName;
    }

    public String getGeneratedClassModel() {
        return generate();
    }

    public boolean isSupportsValidation() {
        return supportsValidation;
    }

    public void setSupportsValidation(boolean supportsValidation) {
        this.supportsValidation = supportsValidation;
    }

    public boolean isSupportsOpenApi() {
        return supportsOpenApi;
    }

    public void setSupportsOpenApi(boolean supportsOpenApi) {
        this.supportsOpenApi = supportsOpenApi;
    }

    public boolean isAsEntity() {
        return asEntity;
    }

    public void setAsEntity(boolean asEntity, boolean removeAtCompletion) {
        this.asEntity = asEntity;
        this.removeAtCompletion = removeAtCompletion;
    }

    public void addAugmentor(Consumer<CompilationUnit> augmentor) {
        this.augmentors.add(augmentor);
    }

    @Override
    public String toString() {
        return "ModelMetaData [modelClassName=" + modelClassName + "]";
    }

    public static String version(String version) {
        if (version != null && !version.trim().isEmpty()) {
            return "_" + version.replaceAll("\\.", "_");
        }
        return "";
    }

    public static String camelToSnake(String str) {
        // Regular Expression 
        String regex = "([a-z])([A-Z]+)";

        // Replacement string 
        String replacement = "$1_$2";
        str = str.replaceAll(regex, replacement).toUpperCase();

        return str;
    }

    protected String getOrDefault(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
