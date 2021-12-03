
package io.automatiko.engine.codegen.process.persistence;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
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

public class PersistenceGenerator extends AbstractGenerator {

    private static final String FILESYSTEM_PERSISTENCE_TYPE = "filesystem";
    private static final String DB_PERSISTENCE_TYPE = "db";
    private static final String DYNAMO_DB_PERSISTENCE_TYPE = "dynamodb";
    private static final String CASSANDRA_PERSISTENCE_TYPE = "cassandra";
    private static final String MONGODB_PERSISTENCE_TYPE = "mongodb";
    private static final String DEFAULT_PERSISTENCE_TYPE = FILESYSTEM_PERSISTENCE_TYPE;

    private static final String PATH_NAME = "path";

    private static final String CODEC_NAME = "codec";

    private static final String PERSISTENCE_FS_PATH_PROP = "quarkus.automatiko.persistence.filesystem.path";

    private final File targetDirectory;
    private final Collection<?> modelClasses;
    private final boolean persistence;

    private String packageName;
    private DependencyInjectionAnnotator annotator;

    private ClassLoader classLoader;

    public PersistenceGenerator(File targetDirectory, Collection<?> modelClasses, boolean persistence,
            ClassLoader classLoader) {
        this.targetDirectory = targetDirectory;
        this.modelClasses = modelClasses;
        this.persistence = persistence;
        this.classLoader = classLoader;

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
            if (persistenceType.equals(FILESYSTEM_PERSISTENCE_TYPE)) {
                fileSystemBasedPersistence(generatedFiles);
            } else if (persistenceType.equals(DB_PERSISTENCE_TYPE)) {
                dbBasedPersistence(generatedFiles);
            } else if (persistenceType.equals(DYNAMO_DB_PERSISTENCE_TYPE)) {
                dynamoDBBasedPersistence(generatedFiles);
            } else if (persistenceType.equals(CASSANDRA_PERSISTENCE_TYPE)) {
                cassandraBasedPersistence(generatedFiles);
            } else if (persistenceType.equals(MONGODB_PERSISTENCE_TYPE)) {
                mongodbBasedPersistence(generatedFiles);
            }

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

    protected void mongodbBasedPersistence(List<GeneratedFile> generatedFiles) {
        ClassOrInterfaceDeclaration persistenceProviderClazz = new ClassOrInterfaceDeclaration()
                .setName("ProcessInstancesFactoryImpl").setModifiers(Modifier.Keyword.PUBLIC)
                .addExtendedType("io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory");

        CompilationUnit compilationUnit = new CompilationUnit("io.automatiko.engine.addons.persistence.impl");
        compilationUnit.getTypes().add(persistenceProviderClazz);

        persistenceProviderClazz.addConstructor(Keyword.PUBLIC);

        ConstructorDeclaration constructor = persistenceProviderClazz.addConstructor(Keyword.PUBLIC)
                .addParameter("com.mongodb.client.MongoClient", "mongoClient")
                .addParameter("io.automatiko.engine.api.config.MongodbPersistenceConfig", "config");

        BlockStmt body = new BlockStmt();
        ExplicitConstructorInvocationStmt superExp = new ExplicitConstructorInvocationStmt(false, null,
                NodeList.nodeList(new NameExpr("mongoClient"), new NameExpr("config")));
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
