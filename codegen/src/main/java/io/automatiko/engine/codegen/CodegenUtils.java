
package io.automatiko.engine.codegen;

import static io.automatiko.engine.codegen.CodeGenConstants.AMQP_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.AMQP_CONNECTOR_CLASS;
import static io.automatiko.engine.codegen.CodeGenConstants.CAMEL_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.CAMEL_CONNECTOR_CLASS;
import static io.automatiko.engine.codegen.CodeGenConstants.CASSANDRA_PERSISTENCE;
import static io.automatiko.engine.codegen.CodeGenConstants.CASSANDRA_PERSISTENCE_CLASS;
import static io.automatiko.engine.codegen.CodeGenConstants.DB_PERSISTENCE;
import static io.automatiko.engine.codegen.CodeGenConstants.DB_PERSISTENCE_CLASS;
import static io.automatiko.engine.codegen.CodeGenConstants.DIRECT_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.DYNAMODB_PERSISTENCE;
import static io.automatiko.engine.codegen.CodeGenConstants.DYNAMODB_PERSISTENCE_CLASS;
import static io.automatiko.engine.codegen.CodeGenConstants.FS_PERSISTENCE;
import static io.automatiko.engine.codegen.CodeGenConstants.FS_PERSISTENCE_CLASS;
import static io.automatiko.engine.codegen.CodeGenConstants.HTTP_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.HTTP_CONNECTOR_CLASS;
import static io.automatiko.engine.codegen.CodeGenConstants.JMS_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.JMS_CONNECTOR_CLASS;
import static io.automatiko.engine.codegen.CodeGenConstants.KAFKA_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.KAFKA_CONNECTOR_CLASS;
import static io.automatiko.engine.codegen.CodeGenConstants.MONGO_PERSISTENCE;
import static io.automatiko.engine.codegen.CodeGenConstants.MONGO_PERSISTENCE_CLASS;
import static io.automatiko.engine.codegen.CodeGenConstants.MQTT_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.MQTT_CONNECTOR_CLASS;
import static io.automatiko.engine.codegen.CodeGenConstants.OPERATOR_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.OPERATOR_CONNECTOR_CLASS;
import static io.automatiko.engine.codegen.CodeGenConstants.PULSAR_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.PULSAR_CONNECTOR_CLASS;
import static io.automatiko.engine.codegen.CodeGenConstants.RABBITMQ_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.RABBITMQ_CONNECTOR_CLASS;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.workflow.compiler.canonical.TriggerMetaData;
import io.automatiko.engine.workflow.process.executable.core.Metadata;

public class CodegenUtils {

    public static ClassOrInterfaceType genericType(Class<?> outer, Class<?> inner) {
        return genericType(outer.getCanonicalName(), inner.getCanonicalName());
    }

    public static ClassOrInterfaceType genericType(String outer, Class<?> inner) {
        return genericType(outer, inner.getCanonicalName());
    }

    public static ClassOrInterfaceType genericType(String outer, String inner) {
        return new ClassOrInterfaceType(null, outer).setTypeArguments(new ClassOrInterfaceType(null, inner));
    }

    public static MethodDeclaration method(Modifier.Keyword modifier, Class<?> type, String name, BlockStmt body) {
        return method(modifier, type, name, NodeList.nodeList(), body);
    }

    public static MethodDeclaration method(Modifier.Keyword modifier, Class<?> type, String name,
            NodeList<Parameter> parameters, BlockStmt body) {
        return new MethodDeclaration().setModifiers(modifier).setType(type == null ? "void" : type.getCanonicalName())
                .setName(name).setParameters(parameters).setBody(body);
    }

    public static ObjectCreationExpr newObject(Class<?> type) {
        return newObject(type.getCanonicalName());
    }

    public static ObjectCreationExpr newObject(Class<?> type, Expression... arguments) {
        return newObject(type.getCanonicalName(), arguments);
    }

    public static ObjectCreationExpr newObject(String type) {
        return new ObjectCreationExpr(null, new ClassOrInterfaceType(null, type), new NodeList<>());
    }

    public static ObjectCreationExpr newObject(String type, Expression... arguments) {
        return new ObjectCreationExpr(null, new ClassOrInterfaceType(null, type), NodeList.nodeList(arguments));
    }

    public static void interpolateArguments(MethodDeclaration md, String dataType) {
        md.getParameters().forEach(p -> p.setType(dataType));
    }

    // Defaults the "to be interpolated type" to $Type$.
    public static void interpolateTypes(ClassOrInterfaceType t, String dataClazzName) {
        SimpleName returnType = t.getName();
        Map<String, String> interpolatedTypes = new HashMap<>();
        interpolatedTypes.put("$Type$", dataClazzName);
        interpolateTypes(returnType, interpolatedTypes);
        t.getTypeArguments().ifPresent(ta -> interpolateTypeArguments(ta, interpolatedTypes));
    }

    public static void interpolateEventTypes(ClassOrInterfaceType t, String dataClazzName) {
        SimpleName returnType = t.getName();
        Map<String, String> interpolatedTypes = new HashMap<>();
        interpolatedTypes.put("$DataEventType$", dataClazzName);
        interpolateTypes(returnType, interpolatedTypes);
        t.getTypeArguments().ifPresent(ta -> interpolateTypeArguments(ta, interpolatedTypes));
    }

    public static void interpolateTypes(ClassOrInterfaceType t, Map<String, String> typeInterpolations) {
        SimpleName returnType = t.getName();
        interpolateTypes(returnType, typeInterpolations);
        t.getTypeArguments().ifPresent(ta -> interpolateTypeArguments(ta, typeInterpolations));
    }

    public static void interpolateTypes(SimpleName returnType, Map<String, String> typeInterpolations) {
        typeInterpolations.entrySet().stream().forEach(entry -> {
            String identifier = returnType.getIdentifier();
            String newIdentifier = identifier.replace(entry.getKey(), entry.getValue());
            returnType.setIdentifier(newIdentifier);
        });
    }

    public static void interpolateTypeArguments(NodeList<Type> ta, Map<String, String> typeInterpolations) {
        ta.stream().filter(Type::isClassOrInterfaceType).map(Type::asClassOrInterfaceType)
                .forEach(t -> interpolateTypes(t, typeInterpolations));
    }

    public static boolean isProcessField(FieldDeclaration fd) {
        return fd.getElementType().asClassOrInterfaceType().getNameAsString().equals("Process");
    }

    public static boolean isApplicationField(FieldDeclaration fd) {
        return fd.getElementType().asClassOrInterfaceType().getNameAsString().equals("Application");
    }

    public static boolean isIdentitySupplierField(FieldDeclaration fd) {
        return fd.getElementType().asClassOrInterfaceType().getNameAsString().equals("IdentitySupplier");
    }

    public static boolean isEventSourceField(FieldDeclaration fd) {
        return fd.getElementType().asClassOrInterfaceType().getNameAsString().equals("EventSource");
    }

    public static MethodDeclaration extractOptionalInjection(String type, String fieldName, String defaultMethod,
            DependencyInjectionAnnotator annotator) {
        return extractOptionalInjection(type, fieldName, defaultMethod, annotator, null);
    }

    public static MethodDeclaration extractOptionalInjection(String type, String fieldName, String defaultMethod,
            DependencyInjectionAnnotator annotator, IfStmt disabledStatement) {
        BlockStmt body = new BlockStmt();

        if (disabledStatement != null) {
            body.addStatement(disabledStatement);
        }

        MethodDeclaration extractMethod = new MethodDeclaration().addModifier(Modifier.Keyword.PROTECTED)
                .setName("extract_" + fieldName).setType(type).setBody(body);
        Expression condition = annotator.optionalInstanceExists(fieldName);
        IfStmt valueExists = new IfStmt(condition, new ReturnStmt(annotator.getOptionalInstance(fieldName)),
                new ReturnStmt(new NameExpr(defaultMethod)));
        body.addStatement(valueExists);
        return extractMethod;
    }

    public static String getConnector(String propertyName, GeneratorContext context, String connector) {

        if (connector != null) {
            return matchConnectorByName(connector);
        }

        if (context.getApplicationProperty(propertyName).isPresent()) {
            return matchConnectorByName(context.getApplicationProperty(propertyName).get());
        }

        // auto discovery by classpath scan
        if (context.getBuildContext().hasClassAvailable(MQTT_CONNECTOR_CLASS)) {
            return MQTT_CONNECTOR;
        } else if (context.getBuildContext().hasClassAvailable(KAFKA_CONNECTOR_CLASS)) {
            return KAFKA_CONNECTOR;
        } else if (context.getBuildContext().hasClassAvailable(AMQP_CONNECTOR_CLASS)) {
            return AMQP_CONNECTOR;
        } else if (context.getBuildContext().hasClassAvailable(CAMEL_CONNECTOR_CLASS)) {
            return CAMEL_CONNECTOR;
        } else if (context.getBuildContext().hasClassAvailable(OPERATOR_CONNECTOR_CLASS)) {
            return OPERATOR_CONNECTOR;
        } else if (context.getBuildContext().hasClassAvailable(JMS_CONNECTOR_CLASS)) {
            return JMS_CONNECTOR;
        } else if (context.getBuildContext().hasClassAvailable(HTTP_CONNECTOR_CLASS)) {
            return HTTP_CONNECTOR;
        } else if (context.getBuildContext().hasClassAvailable(PULSAR_CONNECTOR_CLASS)) {
            return PULSAR_CONNECTOR;
        } else if (context.getBuildContext().hasClassAvailable(RABBITMQ_CONNECTOR_CLASS)) {
            return RABBITMQ_CONNECTOR;
        }

        return "unknown";
    }

    public static String matchConnectorByName(String connector) {
        if (connector.toLowerCase().endsWith("mqtt")) {
            return MQTT_CONNECTOR;
        } else if (connector.toLowerCase().endsWith("kafka")) {
            return KAFKA_CONNECTOR;
        } else if (connector.toLowerCase().endsWith("amqp")) {
            return AMQP_CONNECTOR;
        } else if (connector.toLowerCase().endsWith("camel")) {
            return CAMEL_CONNECTOR;
        } else if (connector.toLowerCase().endsWith("jms")) {
            return JMS_CONNECTOR;
        } else if (connector.toLowerCase().endsWith("http")) {
            return HTTP_CONNECTOR;
        } else if (connector.toLowerCase().endsWith("pulsar")) {
            return PULSAR_CONNECTOR;
        } else if (connector.toLowerCase().endsWith("rabbitmq")) {
            return RABBITMQ_CONNECTOR;
        } else if (connector.toLowerCase().endsWith("direct")) {
            return DIRECT_CONNECTOR;
        }

        return "unknown";
    }

    public static String triggerSanitizedName(TriggerMetaData trigger, String version) {

        // check if node information is available in the context and use it to extract the channel name if exists
        String channel = (String) trigger.getContext(Metadata.TRIGGER_CHANNEL);
        // channel is explicitly used to name the channel for incoming/outgoing annotation
        if (channel != null) {
            return channel;
        }

        return trigger.getName().replaceAll("/", "-").replaceAll("\\+", "x").replaceAll("#", "any") + version(version);
    }

    public static String version(String version) {
        return version(version, "_");

    }

    public static String version(String version, String prefix) {
        if (version != null && !version.trim().isEmpty()) {
            return prefix + version.replaceAll("\\.", "_");
        }
        return "";

    }

    public static String discoverPersistenceType(GeneratorContext context) {

        // auto discovery by classpath scan
        if (context.getBuildContext().hasClassAvailable(MONGO_PERSISTENCE_CLASS)) {
            return MONGO_PERSISTENCE;
        } else if (context.getBuildContext().hasClassAvailable(CASSANDRA_PERSISTENCE_CLASS)) {
            return CASSANDRA_PERSISTENCE;
        } else if (context.getBuildContext().hasClassAvailable(DB_PERSISTENCE_CLASS)) {
            return DB_PERSISTENCE;
        } else if (context.getBuildContext().hasClassAvailable(DYNAMODB_PERSISTENCE_CLASS)) {
            return DYNAMODB_PERSISTENCE;
        } else if (context.getBuildContext().hasClassAvailable(FS_PERSISTENCE_CLASS)) {
            return FS_PERSISTENCE;
        }

        return "unknown";
    }
}
