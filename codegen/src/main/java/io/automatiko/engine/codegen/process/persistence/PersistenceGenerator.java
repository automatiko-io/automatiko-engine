
package io.automatiko.engine.codegen.process.persistence;

import static io.automatiko.engine.codegen.CodegenUtils.genericType;

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
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.audit.Auditor;
import io.automatiko.engine.api.uow.TransactionLogStore;
import io.automatiko.engine.api.workflow.encrypt.StoredDataCodec;
import io.automatiko.engine.codegen.AbstractGenerator;
import io.automatiko.engine.codegen.ApplicationSection;
import io.automatiko.engine.codegen.BodyDeclarationComparator;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.codegen.ConfigGenerator;
import io.automatiko.engine.codegen.GeneratedFile;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import jakarta.enterprise.inject.Instance;

public class PersistenceGenerator extends AbstractGenerator {

    public static final String FS_PATH_KEY = "quarkus.automatiko.persistence.filesystem.path";
    public static final String FS_LOCK_TIMEOUT_KEY = "quarkus.automatiko.persistence.filesystem.lock-timeout";
    public static final String FS_LOCK_LIMIT_KEY = "quarkus.automatiko.persistence.filesystem.lock-limit";
    public static final String FS_LOCK_WAIT_KEY = "quarkus.automatiko.persistence.filesystem.lock-wait";

    public static final String DYNAMODB_CREATE_TABLES_KEY = "quarkus.automatiko.persistence.dynamodb.create-tables";
    public static final String DYNAMODB_READ_CAPACITY_KEY = "quarkus.automatiko.persistence.dynamodb.read-capacity";
    public static final String DYNAMODB_WRITE_CAPACITY_KEY = "quarkus.automatiko.persistence.dynamodb.write-capacity";

    public static final String CASSANDRA_CREATE_KEYSPACE_KEY = "quarkus.automatiko.persistence.cassandra.create-keyspace";
    public static final String CASSANDRA_CREATE_TABLES_KEY = "quarkus.automatiko.persistence.cassandra.create-tables";
    public static final String CASSANDRA_KEYSPACE_KEY = "quarkus.automatiko.persistence.cassandra.keyspace";

    public static final String MONGO_DATABASE_KEY = "quarkus.automatiko.persistence.mongodb.database";
    public static final String MONGO_LOCK_TIMEOUT_KEY = "quarkus.automatiko.persistence.mongodb.lock-timeout";
    public static final String MONGO_LOCK_LIMIT_KEY = "quarkus.automatiko.persistence.mongodb.lock-limit";
    public static final String MONGO_LOCK_WAIT_KEY = "quarkus.automatiko.persistence.mongodb.lock-wait";

    private static final String FILESYSTEM_PERSISTENCE_TYPE = "filesystem";
    private static final String DB_PERSISTENCE_TYPE = "db";
    private static final String DYNAMO_DB_PERSISTENCE_TYPE = "dynamodb";
    private static final String CASSANDRA_PERSISTENCE_TYPE = "cassandra";
    private static final String MONGODB_PERSISTENCE_TYPE = "mongodb";
    private static final String DEFAULT_PERSISTENCE_TYPE = FILESYSTEM_PERSISTENCE_TYPE;

    private static final String PATH_NAME = "path";

    private static final String CODEC_NAME = "codec";

    private static final String TRANSACTION_LOG_STORE_NAME = "transactionLogStore";

    private static final String AUDITOR_NAME = "auditor";

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
                .orElse(null);

        if (persistenceType == null) {
            persistenceType = CodegenUtils.discoverPersistenceType(context);
            // since it was discovered set the properties for completeness
            context.setApplicationProperty("quarkus.automatiko.persistence.type", persistenceType);
            context.setApplicationProperty("quarkus.automatiko.jobs.type", persistenceType);
        }

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

        ConstructorDeclaration constructor = persistenceProviderClazz.addConstructor(Keyword.PUBLIC);

        Parameter path = new Parameter(new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                NodeList.nodeList(new ClassOrInterfaceType(null, "String"))), PATH_NAME);

        Parameter configuredLockTimeout = new Parameter(
                new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                        NodeList.nodeList(new ClassOrInterfaceType(null, "Integer"))),
                "configuredLockTimeout");
        Parameter configuredLockLimit = new Parameter(
                new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                        NodeList.nodeList(new ClassOrInterfaceType(null, "Integer"))),
                "configuredLockLimit");
        Parameter configuredLockWait = new Parameter(
                new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                        NodeList.nodeList(new ClassOrInterfaceType(null, "Integer"))),
                "configuredLockWait");

        constructor.addParameter(path);
        constructor.addParameter(configuredLockTimeout);
        constructor.addParameter(configuredLockLimit);
        constructor.addParameter(configuredLockWait);

        BlockStmt body = new BlockStmt();
        ExplicitConstructorInvocationStmt superExp = new ExplicitConstructorInvocationStmt(false, null,
                NodeList.nodeList(new NameExpr("configuredLockTimeout"), new NameExpr("configuredLockLimit"),
                        new NameExpr("configuredLockWait")));
        body.addStatement(superExp);
        body.addStatement(new AssignExpr(new FieldAccessExpr(new ThisExpr(), "path"), new NameExpr("path"), Operator.ASSIGN));

        constructor.setBody(body);

        if (useInjection()) {
            annotator.withApplicationComponent(persistenceProviderClazz);

            annotator.withConfig(path, FS_PATH_KEY);
            annotator.withConfig(configuredLockTimeout, FS_LOCK_TIMEOUT_KEY);
            annotator.withConfig(configuredLockLimit, FS_LOCK_LIMIT_KEY);
            annotator.withConfig(configuredLockWait, FS_LOCK_WAIT_KEY);

            FieldDeclaration pathField = new FieldDeclaration()
                    .addVariable(
                            new VariableDeclarator()
                                    .setType(new ClassOrInterfaceType(null,
                                            new SimpleName(Optional.class.getCanonicalName()),
                                            NodeList.nodeList(
                                                    new ClassOrInterfaceType(null, String.class.getCanonicalName()))))
                                    .setName(PATH_NAME));

            // allow to inject path for the file system storage
            BlockStmt pathMethodBody = new BlockStmt();
            pathMethodBody.addStatement(new ReturnStmt(
                    new MethodCallExpr(new NameExpr(PATH_NAME), "orElse").addArgument(new StringLiteralExpr("/tmp"))));

            MethodDeclaration pathMethod = new MethodDeclaration().addModifier(Keyword.PUBLIC).setName(PATH_NAME)
                    .setType(String.class).setBody(pathMethodBody);

            persistenceProviderClazz.addMember(pathField);
            persistenceProviderClazz.addMember(pathMethod);

            addCodecComponents(persistenceProviderClazz);

            addTransactionLogStoreComponents(persistenceProviderClazz);
            addAuditorComponents(persistenceProviderClazz);
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

            addTransactionLogStoreComponents(persistenceProviderClazz);
            addAuditorComponents(persistenceProviderClazz);
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
                .addParameter("software.amazon.awssdk.services.dynamodb.DynamoDbClient", "dynamodb");

        Parameter createTables = new Parameter(new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                NodeList.nodeList(new ClassOrInterfaceType(null, "Boolean"))), "createTables");
        Parameter readCapacity = new Parameter(new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                NodeList.nodeList(new ClassOrInterfaceType(null, "Long"))), "readCapacity");
        Parameter writeCapacity = new Parameter(
                new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                        NodeList.nodeList(new ClassOrInterfaceType(null, "Long"))),
                "writeCapacity");

        constructor.addParameter(createTables).addParameter(readCapacity).addParameter(writeCapacity);

        BlockStmt body = new BlockStmt();
        ExplicitConstructorInvocationStmt superExp = new ExplicitConstructorInvocationStmt(false, null,
                NodeList.nodeList(new NameExpr("dynamodb"), new NameExpr("createTables"), new NameExpr("readCapacity"),
                        new NameExpr("writeCapacity")));
        body.addStatement(superExp);

        constructor.setBody(body);

        if (useInjection()) {
            annotator.withApplicationComponent(persistenceProviderClazz);
            annotator.withInjection(constructor);

            annotator.withConfig(createTables, DYNAMODB_CREATE_TABLES_KEY);
            annotator.withConfig(readCapacity, DYNAMODB_READ_CAPACITY_KEY);
            annotator.withConfig(writeCapacity, DYNAMODB_WRITE_CAPACITY_KEY);

            addCodecComponents(persistenceProviderClazz);

            addTransactionLogStoreComponents(persistenceProviderClazz);
            addAuditorComponents(persistenceProviderClazz);
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
                .addParameter("com.datastax.oss.driver.api.core.CqlSession", "cqlSession");

        Parameter createKeyspace = new Parameter(
                new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                        NodeList.nodeList(new ClassOrInterfaceType(null, "Boolean"))),
                "createKeyspace");
        Parameter createTables = new Parameter(new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                NodeList.nodeList(new ClassOrInterfaceType(null, "Boolean"))), "createTables");
        Parameter keyspace = new Parameter(new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                NodeList.nodeList(new ClassOrInterfaceType(null, "String"))), "keyspace");

        constructor.addParameter(createKeyspace).addParameter(createTables).addParameter(keyspace);

        BlockStmt body = new BlockStmt();
        ExplicitConstructorInvocationStmt superExp = new ExplicitConstructorInvocationStmt(false, null,
                NodeList.nodeList(new NameExpr("cqlSession"), new NameExpr("createKeyspace"), new NameExpr("createTables"),
                        new NameExpr("keyspace")));
        body.addStatement(superExp);

        constructor.setBody(body);

        if (useInjection()) {
            annotator.withApplicationComponent(persistenceProviderClazz);
            annotator.withInjection(constructor);

            annotator.withConfig(createKeyspace, CASSANDRA_CREATE_KEYSPACE_KEY);
            annotator.withConfig(createTables, CASSANDRA_CREATE_TABLES_KEY);
            annotator.withConfig(keyspace, CASSANDRA_KEYSPACE_KEY);

            addCodecComponents(persistenceProviderClazz);

            addTransactionLogStoreComponents(persistenceProviderClazz);
            addAuditorComponents(persistenceProviderClazz);
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
                .addParameter(new ClassOrInterfaceType(null, new SimpleName(Instance.class.getCanonicalName()),
                        NodeList.nodeList(new ClassOrInterfaceType(null, "com.mongodb.client.MongoClient"))), "mongoClient");

        Parameter database = new Parameter(new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                NodeList.nodeList(new ClassOrInterfaceType(null, "String"))), "database");

        Parameter configuredLockTimeout = new Parameter(
                new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                        NodeList.nodeList(new ClassOrInterfaceType(null, "Integer"))),
                "configuredLockTimeout");
        Parameter configuredLockLimit = new Parameter(
                new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                        NodeList.nodeList(new ClassOrInterfaceType(null, "Integer"))),
                "configuredLockLimit");
        Parameter configuredLockWait = new Parameter(
                new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                        NodeList.nodeList(new ClassOrInterfaceType(null, "Integer"))),
                "configuredLockWait");

        constructor.addParameter(database);
        constructor.addParameter(configuredLockTimeout);
        constructor.addParameter(configuredLockLimit);
        constructor.addParameter(configuredLockWait);

        BlockStmt body = new BlockStmt();
        ExplicitConstructorInvocationStmt superExp = new ExplicitConstructorInvocationStmt(false, null,
                NodeList.nodeList(new NameExpr("mongoClient"), new NameExpr("database"),
                        new NameExpr("configuredLockTimeout"), new NameExpr("configuredLockLimit"),
                        new NameExpr("configuredLockWait")));
        body.addStatement(superExp);

        constructor.setBody(body);

        if (useInjection()) {
            annotator.withApplicationComponent(persistenceProviderClazz);
            annotator.withInjection(constructor);

            annotator.withConfig(database, MONGO_DATABASE_KEY);
            annotator.withConfig(configuredLockTimeout, MONGO_LOCK_TIMEOUT_KEY);
            annotator.withConfig(configuredLockLimit, MONGO_LOCK_LIMIT_KEY);
            annotator.withConfig(configuredLockWait, MONGO_LOCK_WAIT_KEY);

            addCodecComponents(persistenceProviderClazz);

            addTransactionLogStoreComponents(persistenceProviderClazz);
            addAuditorComponents(persistenceProviderClazz);
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

    private void addTransactionLogStoreComponents(ClassOrInterfaceDeclaration persistenceProviderClazz) {

        // allow to inject transaction log store implementation
        FieldDeclaration tLogStoreField = new FieldDeclaration()
                .addVariable(
                        new VariableDeclarator()
                                .setType(genericType(annotator.optionalInstanceInjectionType(), TransactionLogStore.class))
                                .setName(TRANSACTION_LOG_STORE_NAME));
        BlockStmt tLogStoreMethodBody = new BlockStmt();
        Expression condition = annotator.optionalInstanceExists(TRANSACTION_LOG_STORE_NAME);
        IfStmt injectedIf = new IfStmt(condition,
                new ReturnStmt(new MethodCallExpr(new NameExpr(TRANSACTION_LOG_STORE_NAME), "get")),
                new ReturnStmt(new NullLiteralExpr()));
        tLogStoreMethodBody.addStatement(injectedIf);

        MethodDeclaration tLogStoreMethod = new MethodDeclaration().addModifier(Keyword.PUBLIC)
                .setName(TRANSACTION_LOG_STORE_NAME)
                .setType(TransactionLogStore.class.getCanonicalName()).setBody(tLogStoreMethodBody);

        annotator.withOptionalInjection(tLogStoreField);

        persistenceProviderClazz.addMember(tLogStoreField);
        persistenceProviderClazz.addMember(tLogStoreMethod);
    }

    private void addAuditorComponents(ClassOrInterfaceDeclaration persistenceProviderClazz) {

        // allow to inject auditor implementation
        FieldDeclaration auditorStoreField = new FieldDeclaration()
                .addVariable(
                        new VariableDeclarator()
                                .setType(genericType(annotator.optionalInstanceInjectionType(), Auditor.class))
                                .setName(AUDITOR_NAME));
        BlockStmt auditorMethodBody = new BlockStmt();
        Expression condition = annotator.optionalInstanceExists(AUDITOR_NAME);
        IfStmt injectedIf = new IfStmt(condition,
                new ReturnStmt(new MethodCallExpr(new NameExpr(AUDITOR_NAME), "get")),
                new ReturnStmt(new NullLiteralExpr()));
        auditorMethodBody.addStatement(injectedIf);

        MethodDeclaration auditorMethod = new MethodDeclaration().addModifier(Keyword.PUBLIC)
                .setName(AUDITOR_NAME)
                .setType(Auditor.class.getCanonicalName()).setBody(auditorMethodBody);

        annotator.withOptionalInjection(auditorStoreField);

        persistenceProviderClazz.addMember(auditorStoreField);
        persistenceProviderClazz.addMember(auditorMethod);
    }
}
