
package io.automatiko.engine.codegen.process.persistence;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.workflow.encrypt.StoredDataCodec;
import io.automatiko.engine.codegen.AbstractGenerator;
import io.automatiko.engine.codegen.ApplicationSection;
import io.automatiko.engine.codegen.BodyDeclarationComparator;
import io.automatiko.engine.codegen.ConfigGenerator;
import io.automatiko.engine.codegen.GeneratedFile;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.codegen.metadata.MetaDataWriter;
import io.automatiko.engine.codegen.metadata.PersistenceLabeler;
import io.automatiko.engine.codegen.metadata.PersistenceProtoFilesLabeler;
import io.automatiko.engine.codegen.process.persistence.proto.Proto;
import io.automatiko.engine.codegen.process.persistence.proto.ProtoGenerator;

public class PersistenceGenerator extends AbstractGenerator {

    private static final String FILESYSTEM_PERSISTENCE_TYPE = "filesystem";
    private static final String INFINISPAN_PERSISTENCE_TYPE = "infinispan";
    private static final String DB_PERSISTENCE_TYPE = "db";
    private static final String DYNAMO_DB_PERSISTENCE_TYPE = "dynamodb";
    private static final String CASSANDRA_PERSISTENCE_TYPE = "cassandra";
    private static final String DEFAULT_PERSISTENCE_TYPE = FILESYSTEM_PERSISTENCE_TYPE;

    private static final String TEMPLATE_NAME = "templateName";
    private static final String PATH_NAME = "path";

    private static final String CODEC_NAME = "codec";

    private static final String APPLICATION_PROTO = "automatik-application.proto";
    private static final String PERSISTENCE_FS_PATH_PROP = "quarkus.automatiko.persistence.filesystem.path";
    private static final String PERSISTENCE_ISPN_TEMPLATE_PROP = "quarkus.automatiko.persistence.infinispan.template";

    private final File targetDirectory;
    private final Collection<?> modelClasses;
    private final boolean persistence;
    private final ProtoGenerator<?> protoGenerator;

    private List<String> parameters;

    private String packageName;
    private DependencyInjectionAnnotator annotator;

    private ClassLoader classLoader;

    private PersistenceProtoFilesLabeler persistenceProtoLabeler = new PersistenceProtoFilesLabeler();
    private PersistenceLabeler persistenceLabeler = new PersistenceLabeler();

    public PersistenceGenerator(File targetDirectory, Collection<?> modelClasses, boolean persistence,
            ProtoGenerator<?> protoGenerator, List<String> parameters) {
        this(targetDirectory, modelClasses, persistence, protoGenerator, Thread.currentThread().getContextClassLoader(),
                parameters);
    }

    public PersistenceGenerator(File targetDirectory, Collection<?> modelClasses, boolean persistence,
            ProtoGenerator<?> protoGenerator, ClassLoader classLoader, List<String> parameters) {
        this.targetDirectory = targetDirectory;
        this.modelClasses = modelClasses;
        this.persistence = persistence;
        this.protoGenerator = protoGenerator;
        this.classLoader = classLoader;
        this.parameters = parameters;
        if (this.persistence) {
            this.addLabeler(persistenceProtoLabeler);
            this.addLabeler(persistenceLabeler);
        }
    }

    @Override
    public ApplicationSection section() {
        return null;
    }

    @Override
    public Collection<GeneratedFile> generate() {
        String persistenceType = context.getBuildContext().config().persistence().type()
                .orElse(DEFAULT_PERSISTENCE_TYPE);

        List<GeneratedFile> generatedFiles = new ArrayList<>();
        if (persistence) {
            if (persistenceType.equals(INFINISPAN_PERSISTENCE_TYPE)) {
                inifinispanBasedPersistence(generatedFiles);
            } else if (persistenceType.equals(FILESYSTEM_PERSISTENCE_TYPE)) {
                fileSystemBasedPersistence(generatedFiles);
            } else if (persistenceType.equals(DB_PERSISTENCE_TYPE)) {
                dbBasedPersistence(generatedFiles);
            } else if (persistenceType.equals(DYNAMO_DB_PERSISTENCE_TYPE)) {
                dynamoDBBasedPersistence(generatedFiles);
            } else if (persistenceType.equals(CASSANDRA_PERSISTENCE_TYPE)) {
                cassandraBasedPersistence(generatedFiles);
            }

        }

        if (targetDirectory.isDirectory()) {
            MetaDataWriter.writeLabelsImageMetadata(targetDirectory, getLabels());
        }
        return generatedFiles;
    }

    @Override
    public void updateConfig(ConfigGenerator cfg) {
    }

    @Override
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public void setDependencyInjection(DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
    }

    protected boolean useInjection() {
        return this.annotator != null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void inifinispanBasedPersistence(List<GeneratedFile> generatedFiles) {
        Collection dataModelClasses = protoGenerator.extractDataClasses((Collection) modelClasses,
                targetDirectory.toString());
        Path protoFilePath = Paths.get(targetDirectory.getParent(), "src/main/resources", "/persistence",
                APPLICATION_PROTO);
        File persistencePath = Paths.get(targetDirectory.getAbsolutePath(), "/classes/persistence").toFile();

        if (persistencePath != null && persistencePath.isDirectory()) {
            // only process proto files generated by the inner generator
            for (final File protoFile : Objects
                    .requireNonNull(persistencePath.listFiles((dir, name) -> !APPLICATION_PROTO.equalsIgnoreCase(name)
                            && name.toLowerCase().endsWith(PersistenceProtoFilesLabeler.PROTO_FILE_EXT))))
                this.persistenceProtoLabeler.processProto(protoFile);
        }

        if (!protoFilePath.toFile().exists()) {
            try {
                // generate proto file based on known data model
                Proto proto = protoGenerator.generate(packageName, dataModelClasses,
                        "import \"automatik-types.proto\";");
                protoFilePath = Paths.get(targetDirectory.toString(), "classes", "/persistence", APPLICATION_PROTO);

                Files.createDirectories(protoFilePath.getParent());
                Files.write(protoFilePath, proto.toString().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException("Error during proto file generation/store", e);
            }

        }

        ClassOrInterfaceDeclaration persistenceProviderClazz = new ClassOrInterfaceDeclaration()
                .setName("ProcessInstancesFactoryImpl").setModifiers(Modifier.Keyword.PUBLIC)
                .addExtendedType("io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory");

        CompilationUnit compilationUnit = new CompilationUnit("io.automatiko.engine.addons.persistence.impl");
        compilationUnit.getTypes().add(persistenceProviderClazz);

        persistenceProviderClazz.addConstructor(Keyword.PUBLIC).setBody(new BlockStmt().addStatement(
                new ExplicitConstructorInvocationStmt(false, null, NodeList.nodeList(new NullLiteralExpr()))));

        ConstructorDeclaration constructor = persistenceProviderClazz.addConstructor(Keyword.PUBLIC);

        List<Expression> paramNames = new ArrayList<>();
        for (String parameter : parameters) {
            String name = "param" + paramNames.size();
            constructor.addParameter(parameter, name);
            paramNames.add(new NameExpr(name));
        }
        BlockStmt body = new BlockStmt();
        ExplicitConstructorInvocationStmt superExp = new ExplicitConstructorInvocationStmt(false, null,
                NodeList.nodeList(paramNames));
        body.addStatement(superExp);

        constructor.setBody(body);

        if (useInjection()) {
            annotator.withApplicationComponent(persistenceProviderClazz);
            annotator.withInjection(constructor);

            FieldDeclaration templateNameField = new FieldDeclaration()
                    .addVariable(
                            new VariableDeclarator()
                                    .setType(new ClassOrInterfaceType(null,
                                            new SimpleName(Optional.class.getCanonicalName()),
                                            NodeList.nodeList(
                                                    new ClassOrInterfaceType(null, String.class.getCanonicalName()))))
                                    .setName(TEMPLATE_NAME));
            annotator.withConfigInjection(templateNameField, PERSISTENCE_ISPN_TEMPLATE_PROP);
            // allow to inject template name for the cache
            BlockStmt templateMethodBody = new BlockStmt();
            templateMethodBody.addStatement(new ReturnStmt(
                    new MethodCallExpr(new NameExpr(TEMPLATE_NAME), "orElse").addArgument(new StringLiteralExpr(""))));

            MethodDeclaration templateNameMethod = new MethodDeclaration().addModifier(Keyword.PUBLIC)
                    .setName("template").setType(String.class).setBody(templateMethodBody);

            persistenceProviderClazz.addMember(templateNameField);
            persistenceProviderClazz.addMember(templateNameMethod);

            addCodecComponents(persistenceProviderClazz);
        }
        List<String> variableMarshallers = new ArrayList<>();
        // handler process variable marshallers
        if (protoFilePath.toFile().exists()) {
            MarshallerGenerator marshallerGenerator = new MarshallerGenerator(this.classLoader);
            try {
                String protoContent = new String(Files.readAllBytes(protoFilePath));

                List<CompilationUnit> marshallers = marshallerGenerator.generate(protoContent);

                if (!marshallers.isEmpty()) {

                    for (CompilationUnit marshallerClazz : marshallers) {
                        String packageName = marshallerClazz.getPackageDeclaration().map(pd -> pd.getName().toString())
                                .orElse("");
                        String clazzName = packageName + "." + marshallerClazz
                                .findFirst(ClassOrInterfaceDeclaration.class).map(c -> c.getName().toString()).get();

                        variableMarshallers.add(clazzName);

                        generatedFiles
                                .add(new GeneratedFile(GeneratedFile.Type.CLASS, clazzName.replace('.', '/') + ".java",
                                        marshallerClazz.toString().getBytes(StandardCharsets.UTF_8)));
                    }
                }

                // handler process variable marshallers
                if (!variableMarshallers.isEmpty()) {

                    MethodDeclaration protoMethod = new MethodDeclaration().addModifier(Keyword.PUBLIC).setName("proto")
                            .setType(String.class).setBody(new BlockStmt()
                                    .addStatement(new ReturnStmt(new StringLiteralExpr().setString(protoContent))));

                    persistenceProviderClazz.addMember(protoMethod);

                    ClassOrInterfaceType listType = new ClassOrInterfaceType(null, List.class.getCanonicalName());
                    BlockStmt marshallersMethodBody = new BlockStmt();
                    VariableDeclarationExpr marshallerList = new VariableDeclarationExpr(
                            new VariableDeclarator(listType, "list",
                                    new ObjectCreationExpr(null,
                                            new ClassOrInterfaceType(null, ArrayList.class.getCanonicalName()),
                                            NodeList.nodeList())));
                    marshallersMethodBody.addStatement(marshallerList);

                    for (String marshallerClazz : variableMarshallers) {

                        MethodCallExpr addMarshallerMethod = new MethodCallExpr(new NameExpr("list"), "add")
                                .addArgument(new ObjectCreationExpr(null,
                                        new ClassOrInterfaceType(null, marshallerClazz), NodeList.nodeList()));
                        marshallersMethodBody.addStatement(addMarshallerMethod);

                    }

                    marshallersMethodBody.addStatement(new ReturnStmt(new NameExpr("list")));

                    MethodDeclaration marshallersMethod = new MethodDeclaration().addModifier(Keyword.PUBLIC)
                            .setName("marshallers").setType(listType).setBody(marshallersMethodBody);

                    persistenceProviderClazz.addMember(marshallersMethod);
                }

                String packageName = compilationUnit.getPackageDeclaration().map(pd -> pd.getName().toString())
                        .orElse("");
                String clazzName = packageName + "." + persistenceProviderClazz
                        .findFirst(ClassOrInterfaceDeclaration.class).map(c -> c.getName().toString()).get();

                generatedFiles.add(new GeneratedFile(GeneratedFile.Type.CLASS, clazzName.replace('.', '/') + ".java",
                        compilationUnit.toString().getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                throw new RuntimeException("Error when generating marshallers for defined variables", e);
            }
            persistenceProviderClazz.getMembers().sort(new BodyDeclarationComparator());
        }
    }

    protected void fileSystemBasedPersistence(List<GeneratedFile> generatedFiles) {
        ClassOrInterfaceDeclaration persistenceProviderClazz = new ClassOrInterfaceDeclaration()
                .setName("ProcessInstancesFactoryImpl").setModifiers(Modifier.Keyword.PUBLIC)
                .addExtendedType("io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory");

        CompilationUnit compilationUnit = new CompilationUnit("io.automatiko.engine.addons.persistence.impl");
        compilationUnit.getTypes().add(persistenceProviderClazz);

        if (useInjection()) {
            annotator.withApplicationComponent(persistenceProviderClazz);

            FieldDeclaration pathField = new FieldDeclaration()
                    .addVariable(
                            new VariableDeclarator()
                                    .setType(new ClassOrInterfaceType(null,
                                            new SimpleName(Optional.class.getCanonicalName()),
                                            NodeList.nodeList(
                                                    new ClassOrInterfaceType(null, String.class.getCanonicalName()))))
                                    .setName(PATH_NAME));
            annotator.withConfigInjection(pathField, PERSISTENCE_FS_PATH_PROP);
            // allow to inject path for the file system storage
            BlockStmt pathMethodBody = new BlockStmt();
            pathMethodBody.addStatement(new ReturnStmt(
                    new MethodCallExpr(new NameExpr(PATH_NAME), "orElse").addArgument(new StringLiteralExpr("/tmp"))));

            MethodDeclaration pathMethod = new MethodDeclaration().addModifier(Keyword.PUBLIC).setName(PATH_NAME)
                    .setType(String.class).setBody(pathMethodBody);

            persistenceProviderClazz.addMember(pathField);
            persistenceProviderClazz.addMember(pathMethod);

            addCodecComponents(persistenceProviderClazz);
        }

        String packageName = compilationUnit.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
        String clazzName = packageName + "." + persistenceProviderClazz.findFirst(ClassOrInterfaceDeclaration.class)
                .map(c -> c.getName().toString()).get();

        generatedFiles.add(new GeneratedFile(GeneratedFile.Type.CLASS, clazzName.replace('.', '/') + ".java",
                compilationUnit.toString().getBytes(StandardCharsets.UTF_8)));

        persistenceProviderClazz.getMembers().sort(new BodyDeclarationComparator());
    }

    protected void dbBasedPersistence(List<GeneratedFile> generatedFiles) {
        ClassOrInterfaceDeclaration persistenceProviderClazz = new ClassOrInterfaceDeclaration()
                .setName("ProcessInstancesFactoryImpl").setModifiers(Modifier.Keyword.PUBLIC)
                .addExtendedType("io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory");

        CompilationUnit compilationUnit = new CompilationUnit("io.automatiko.engine.addons.persistence.impl");
        compilationUnit.getTypes().add(persistenceProviderClazz);

        if (useInjection()) {
            annotator.withApplicationComponent(persistenceProviderClazz);

            addCodecComponents(persistenceProviderClazz);
        }

        String packageName = compilationUnit.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
        String clazzName = packageName + "." + persistenceProviderClazz.findFirst(ClassOrInterfaceDeclaration.class)
                .map(c -> c.getName().toString()).get();

        generatedFiles.add(new GeneratedFile(GeneratedFile.Type.CLASS, clazzName.replace('.', '/') + ".java",
                compilationUnit.toString().getBytes(StandardCharsets.UTF_8)));

        persistenceProviderClazz.getMembers().sort(new BodyDeclarationComparator());
    }

    protected void dynamoDBBasedPersistence(List<GeneratedFile> generatedFiles) {
        ClassOrInterfaceDeclaration persistenceProviderClazz = new ClassOrInterfaceDeclaration()
                .setName("ProcessInstancesFactoryImpl").setModifiers(Modifier.Keyword.PUBLIC)
                .addExtendedType("io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory");

        CompilationUnit compilationUnit = new CompilationUnit("io.automatiko.engine.addons.persistence.impl");
        compilationUnit.getTypes().add(persistenceProviderClazz);

        persistenceProviderClazz.addConstructor(Keyword.PUBLIC);

        ConstructorDeclaration constructor = persistenceProviderClazz.addConstructor(Keyword.PUBLIC)
                .addParameter("software.amazon.awssdk.services.dynamodb.DynamoDbClient", "dynamodb")
                .addParameter("io.automatiko.engine.api.config.DynamoDBPersistenceConfig", "config");

        BlockStmt body = new BlockStmt();
        ExplicitConstructorInvocationStmt superExp = new ExplicitConstructorInvocationStmt(false, null,
                NodeList.nodeList(new NameExpr("dynamodb"), new NameExpr("config")));
        body.addStatement(superExp);

        constructor.setBody(body);

        if (useInjection()) {
            annotator.withApplicationComponent(persistenceProviderClazz);
            annotator.withInjection(constructor);

            addCodecComponents(persistenceProviderClazz);
        }

        String packageName = compilationUnit.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
        String clazzName = packageName + "." + persistenceProviderClazz.findFirst(ClassOrInterfaceDeclaration.class)
                .map(c -> c.getName().toString()).get();

        generatedFiles.add(new GeneratedFile(GeneratedFile.Type.CLASS, clazzName.replace('.', '/') + ".java",
                compilationUnit.toString().getBytes(StandardCharsets.UTF_8)));

        persistenceProviderClazz.getMembers().sort(new BodyDeclarationComparator());
    }

    protected void cassandraBasedPersistence(List<GeneratedFile> generatedFiles) {
        ClassOrInterfaceDeclaration persistenceProviderClazz = new ClassOrInterfaceDeclaration()
                .setName("ProcessInstancesFactoryImpl").setModifiers(Modifier.Keyword.PUBLIC)
                .addExtendedType("io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory");

        CompilationUnit compilationUnit = new CompilationUnit("io.automatiko.engine.addons.persistence.impl");
        compilationUnit.getTypes().add(persistenceProviderClazz);

        persistenceProviderClazz.addConstructor(Keyword.PUBLIC);

        ConstructorDeclaration constructor = persistenceProviderClazz.addConstructor(Keyword.PUBLIC)
                .addParameter("com.datastax.oss.driver.api.core.CqlSession", "cqlSession")
                .addParameter("io.automatiko.engine.api.config.CassandraPersistenceConfig", "config");

        BlockStmt body = new BlockStmt();
        ExplicitConstructorInvocationStmt superExp = new ExplicitConstructorInvocationStmt(false, null,
                NodeList.nodeList(new NameExpr("cqlSession"), new NameExpr("config")));
        body.addStatement(superExp);

        constructor.setBody(body);

        if (useInjection()) {
            annotator.withApplicationComponent(persistenceProviderClazz);
            annotator.withInjection(constructor);

            addCodecComponents(persistenceProviderClazz);
        }

        String packageName = compilationUnit.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
        String clazzName = packageName + "." + persistenceProviderClazz.findFirst(ClassOrInterfaceDeclaration.class)
                .map(c -> c.getName().toString()).get();

        generatedFiles.add(new GeneratedFile(GeneratedFile.Type.CLASS, clazzName.replace('.', '/') + ".java",
                compilationUnit.toString().getBytes(StandardCharsets.UTF_8)));

        persistenceProviderClazz.getMembers().sort(new BodyDeclarationComparator());
    }

    private void addCodecComponents(ClassOrInterfaceDeclaration persistenceProviderClazz) {

        // allow to inject codec implementation
        FieldDeclaration codecField = new FieldDeclaration()
                .addVariable(
                        new VariableDeclarator()
                                .setType(new ClassOrInterfaceType(null, StoredDataCodec.class.getCanonicalName()))
                                .setName(CODEC_NAME));
        BlockStmt codecMethodBody = new BlockStmt();
        codecMethodBody.addStatement(new ReturnStmt(new NameExpr(CODEC_NAME)));

        MethodDeclaration codecMethod = new MethodDeclaration().addModifier(Keyword.PUBLIC).setName(CODEC_NAME)
                .setType(StoredDataCodec.class.getCanonicalName()).setBody(codecMethodBody);

        annotator.withInjection(codecField);

        persistenceProviderClazz.addMember(codecField);
        persistenceProviderClazz.addMember(codecMethod);
    }
}
