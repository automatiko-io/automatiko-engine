
package io.automatiko.engine.codegen.di;

import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.stmt.BlockStmt;

public class CDIDependencyInjectionAnnotator implements DependencyInjectionAnnotator {

    @Override
    public <T extends NodeWithAnnotations<?>> T withNamed(T node, String name) {
        node.addAnnotation(new SingleMemberAnnotationExpr(new Name("javax.inject.Named"), new StringLiteralExpr(name)));
        return node;
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withApplicationComponent(T node) {
        node.addAnnotation("javax.enterprise.context.ApplicationScoped");
        return node;
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withNamedApplicationComponent(T node, String name) {
        return withNamed(withApplicationComponent(node), name);
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withSingletonComponent(T node) {
        node.addAnnotation("javax.inject.Singleton");
        return node;
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withNamedSingletonComponent(T node, String name) {
        return withNamed(withSingletonComponent(node), name);
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withInjection(T node) {
        node.addAnnotation("javax.inject.Inject");
        return node;
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withNamedInjection(T node, String name) {
        return withNamed(withInjection(node), name);
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withOptionalInjection(T node) {
        return withInjection(node);
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withIncomingMessage(T node, String channel) {
        node.addAnnotation(new SingleMemberAnnotationExpr(
                new Name("org.eclipse.microprofile.reactive.messaging.Incoming"), new StringLiteralExpr(channel)));
        return node;
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withOutgoingMessage(T node, String channel) {
        node.addAnnotation(new SingleMemberAnnotationExpr(
                new Name("org.eclipse.microprofile.reactive.messaging.Channel"), new StringLiteralExpr(channel)));
        return node;
    }

    @Override
    public MethodCallExpr withMessageProducer(MethodCallExpr produceMethod, String channel, Expression event) {
        produceMethod.addArgument(event);
        return produceMethod;
    }

    @Override
    public MethodDeclaration withInitMethod(Expression... expression) {
        BlockStmt body = new BlockStmt();
        for (Expression exp : expression) {
            body.addStatement(exp);
        }
        MethodDeclaration method = new MethodDeclaration().addModifier(Keyword.PUBLIC).setName("init")
                .setType(void.class).setBody(body);

        method.addAndGetParameter("io.quarkus.runtime.StartupEvent", "event")
                .addAnnotation("javax.enterprise.event.Observes");

        return method;
    }

    @Override
    public String optionalInstanceInjectionType() {
        return "javax.enterprise.inject.Instance";
    }

    @Override
    public Expression optionalInstanceExists(String fieldName) {
        MethodCallExpr condition = new MethodCallExpr(new NameExpr(fieldName), "isUnsatisfied");
        return new BinaryExpr(condition, new BooleanLiteralExpr(false), BinaryExpr.Operator.EQUALS);
    }

    @Override
    public String multiInstanceInjectionType() {
        return optionalInstanceInjectionType();
    }

    @Override
    public Expression getMultiInstance(String fieldName) {
        return new MethodCallExpr(
                new MethodCallExpr(new NameExpr("java.util.stream.StreamSupport"), "stream",
                        NodeList.nodeList(new MethodCallExpr(new NameExpr(fieldName), "spliterator"),
                                new BooleanLiteralExpr(false))),
                "collect",
                NodeList.nodeList(new MethodCallExpr(new NameExpr("java.util.stream.Collectors"), "toList")));
    }

    @Override
    public String applicationComponentType() {
        return "javax.enterprise.context.ApplicationScoped";
    }

    @Override
    public String emitterType(String dataType) {
        return "org.eclipse.microprofile.reactive.messaging.Emitter<" + dataType + ">";
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withConfigInjection(T node, String configKey) {
        node.addAnnotation(new NormalAnnotationExpr(new Name("org.eclipse.microprofile.config.inject.ConfigProperty"),
                NodeList.nodeList(new MemberValuePair("name", new StringLiteralExpr(configKey)))));
        withInjection(node);
        return node;
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withConfigInjection(T node, String configKey, String defaultValue) {
        node.addAnnotation(new NormalAnnotationExpr(new Name("org.eclipse.microprofile.config.inject.ConfigProperty"),
                NodeList.nodeList(new MemberValuePair("name", new StringLiteralExpr(configKey)),
                        new MemberValuePair("defaultValue", new StringLiteralExpr(defaultValue)))));
        return node;
    }

    @Override
    public String objectMapperInjectorSource(String packageName) {

        return null;
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withRestClientInjection(T node) {
        node.addAnnotation("org.eclipse.microprofile.rest.client.inject.RestClient");
        return node;
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withFunction(T node) {
        node.addAnnotation("io.quarkus.funqy.Funq");
        return node;
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withCloudEventMapping(T node, String trigger, String filter) {
        if (filter != null && !filter.isEmpty()) {
            String[] filterElements = filter.split("=");

            node.addAnnotation(new NormalAnnotationExpr(new Name("io.quarkus.funqy.knative.events.CloudEventMapping"),
                    NodeList.nodeList(new MemberValuePair("trigger", new StringLiteralExpr(trigger)),
                            new MemberValuePair("attributes",
                                    new NormalAnnotationExpr(new Name("io.quarkus.funqy.knative.events.EventAttribute"),
                                            NodeList.nodeList(
                                                    new MemberValuePair("name", new StringLiteralExpr(filterElements[0])),
                                                    new MemberValuePair("value",
                                                            new StringLiteralExpr(filterElements[1]))))))));
        } else {
            node.addAnnotation(new NormalAnnotationExpr(new Name("io.quarkus.funqy.knative.events.CloudEventMapping"),
                    NodeList.nodeList(new MemberValuePair("trigger", new StringLiteralExpr(trigger)))));
        }
        return node;
    }

    @Override
    public <T extends NodeWithAnnotations<?>> T withBlocking(T node) {
        node.addAnnotation("io.smallrye.common.annotation.Blocking");
        return node;
    }
}
