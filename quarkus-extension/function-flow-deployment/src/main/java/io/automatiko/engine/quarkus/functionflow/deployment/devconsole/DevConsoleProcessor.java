package io.automatiko.engine.quarkus.functionflow.deployment.devconsole;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.openapi.models.media.Schema;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.automatiko.engine.api.codegen.Generated;
import io.automatiko.engine.quarkus.functionflow.deployment.AutomatikoFunctionFlowProcessor;
import io.automatiko.engine.quarkus.functionflow.deployment.ExampleGenerator;
import io.automatiko.engine.quarkus.functionflow.dev.WorkflowFunctionFlowInfo;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.smallrye.openapi.runtime.io.schema.SchemaFactory;
import io.smallrye.openapi.runtime.scanner.spi.AnnotationScannerContext;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem create(CombinedIndexBuildItem index) throws Exception {
        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();
        workaroundMultiModuleDevMode(cardPageBuildItem);
        List<WorkflowFunctionFlowInfo> info = getInfo(index);

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .icon("font-awesome-solid:diagram-project")
                .componentLink("qwc-automatiko-function-flow.js")
                .title("Function Flows")
                .staticLabel(String.valueOf(info.size())));

        cardPageBuildItem.addBuildTimeData("functionFlows", info);

        return cardPageBuildItem;
    }

    protected List<WorkflowFunctionFlowInfo> getInfo(CombinedIndexBuildItem index) {

        Optional<String> host = Optional.of("localhost");
        Optional<Integer> port = Optional.of(8080);
        List<WorkflowFunctionFlowInfo> infos = new ArrayList<WorkflowFunctionFlowInfo>();
        Collection<AnnotationInstance> functions = index.getIndex()
                .getAnnotations(AutomatikoFunctionFlowProcessor.createDotName("io.quarkus.funqy.Funq"));
        DotName mapping = AutomatikoFunctionFlowProcessor.createDotName("io.quarkus.funqy.knative.events.CloudEventMapping");

        ExampleGenerator generator = new ExampleGenerator();
        AnnotationScannerContext ctx = AutomatikoFunctionFlowProcessor.buildAnnotationScannerContext(index.getIndex());

        for (AnnotationInstance f : functions) {
            if (f.target().kind().equals(Kind.METHOD)) {
                MethodInfo mi = f.target().asMethod();

                if (mi.declaringClass().declaredAnnotation(
                        AutomatikoFunctionFlowProcessor.createDotName(Generated.class.getCanonicalName())) == null) {
                    continue;
                }

                // create function trigger descriptor for every found function

                Map<String, String> filterAttributes = new HashMap<>();
                if (mi.annotation(mapping).value("attributes") != null) {
                    for (AnnotationValue aValue : ((AnnotationValue[]) mi.annotation(mapping).value("attributes").value())) {
                        AnnotationInstance attribute = (AnnotationInstance) aValue.value();
                        filterAttributes.put(attribute.value("name").asString(), attribute.value().asString());
                    }
                }
                boolean includeSubjectAttribute = false;
                Type param = mi.parameters().get(0).type();
                if (param instanceof ParameterizedType) {
                    param = ((ParameterizedType) param).arguments().get(0);
                    includeSubjectAttribute = true;
                }
                SchemaFactory.typeToSchema(ctx,
                        mi.parameters().get(0).type(), null);
                Schema fSchema = ctx.getOpenApi().getComponents().getSchemas().get(param.name().local());

                String payload = generator.generate(fSchema, ctx.getOpenApi());
                String type = mi.annotation(mapping).value("trigger").asString();
                String source = filterAttributes.getOrDefault("source", "string");
                String id = UUID.randomUUID().toString();
                String specversion = "1.0";

                // remove CE attribute as filters
                filterAttributes.remove("type");
                filterAttributes.remove("source");
                filterAttributes.remove("data");

                StringBuilder curlBinary = new StringBuilder("curl -X POST http://").append(host.get()).append(":")
                        .append(port.get())
                        .append("/ ");
                curlBinary.append("-H \"Content-Type:application/json; charset=UTF-8\" ");
                curlBinary.append("-H \"ce-id:").append(id).append("\" ");
                curlBinary.append("-H \"ce-specversion:").append(specversion).append("\" ");
                curlBinary.append("-H \"ce-type:").append(type).append("\" ");
                curlBinary.append("-H \"ce-source:").append(source).append("\" ");
                if (includeSubjectAttribute) {
                    curlBinary.append("-H \"ce-subject:workflow-instance-id\" ");
                }

                StringBuilder binaryInstructions = new StringBuilder();
                binaryInstructions.append("HTTP headers:").append("\n");
                binaryInstructions.append("Content-Type:application/json; charset=UTF-8").append("\n");
                binaryInstructions.append("ce-id:").append(id).append("\n");
                binaryInstructions.append("ce-specversion:").append(specversion).append("\n");
                binaryInstructions.append("ce-type:").append(type).append("\n");
                binaryInstructions.append("ce-source:").append(source).append("\n");
                if (includeSubjectAttribute) {
                    binaryInstructions.append("ce-subject:workflow-instance-id\n");
                }

                for (Entry<String, String> entry : filterAttributes.entrySet()) {
                    curlBinary.append("-H 'ce-").append(entry.getKey()).append(":").append(entry.getValue()).append("\" ");
                    binaryInstructions.append("ce-").append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
                }

                binaryInstructions.append("\nPOST payload:").append("\n");
                binaryInstructions.append(payload).append("\n");
                curlBinary.append("-d \"").append(payload.replaceAll("\"", "\\\\\\\"").replaceAll("\\s+", "")).append("\"");

                StringBuilder curlStructured = new StringBuilder("curl -X POST http://").append(host.get()).append(":")
                        .append(port.get())
                        .append("/ ");
                curlStructured.append("-H \"Content-Type:application/cloudevents+json; charset=UTF-8\" -d \"");
                StringBuilder structuredPayload = new StringBuilder(" {");
                structuredPayload.append("  \"id\": \"").append(id).append("\",");
                structuredPayload.append("  \"specversion\": \"").append(specversion).append("\",");
                structuredPayload.append("  \"type\": \"").append(type).append("\",");
                structuredPayload.append("  \"source\": \"").append(source).append("\",");
                structuredPayload.append("  \"datacontenttype\": \"application/json; charset=UTF-8\",");
                if (includeSubjectAttribute) {
                    structuredPayload.append("  \"subject\": \"workflow-instance-id\",");
                }

                for (Entry<String, String> entry : filterAttributes.entrySet()) {
                    structuredPayload.append("  \"" + entry.getKey() + "\": \"").append(entry.getValue()).append("\",");
                }

                structuredPayload.append("  \"data\": ");
                structuredPayload.append(payload);
                structuredPayload.append("}");

                curlStructured.append(structuredPayload.toString().replaceAll("\"", "\\\\\\\"").replaceAll("\\s+", ""));
                curlStructured.append("\"");

                StringBuilder structuredInstructions = new StringBuilder();
                structuredInstructions.append("{").append("\n");
                structuredInstructions.append("  \"id\": \"").append(id).append("\",\n");
                structuredInstructions.append("  \"specversion\": \"").append(specversion).append("\",\n");
                structuredInstructions.append("  \"type\": \"").append(type).append("\",\n");
                structuredInstructions.append("  \"source\": \"").append(source).append("\",\n");
                structuredInstructions.append("  \"datacontenttype\": \"application/json; charset=UTF-8\",\n");
                if (includeSubjectAttribute) {
                    structuredInstructions.append("  \"subject\": \"workflow-instance-id\",\n");
                }

                for (Entry<String, String> entry : filterAttributes.entrySet()) {
                    structuredInstructions.append("  \"" + entry.getKey() + "\": \"").append(entry.getValue()).append("\",\n");
                }

                structuredInstructions.append("  \"data\":");
                structuredInstructions.append(payload).append("\n");
                structuredInstructions.append("}").append("\n");

                infos.add(new WorkflowFunctionFlowInfo(mi.name(), "/",
                        binaryInstructions.toString(), curlBinary.toString(),
                        structuredInstructions.toString(),
                        curlStructured.toString()));
            }
        }

        return infos;
    }

    private void workaroundMultiModuleDevMode(CardPageBuildItem cardPageBuildItem) {
        Class<?> c = CardPageBuildItem.class;

        while (c.getSuperclass() != null) {
            try {
                c = c.getSuperclass();
                Field f = c.getDeclaredField("extensionIdentifier");
                f.setAccessible(true);
                f.set(cardPageBuildItem, "io.automatiko.quarkus.automatiko-function-flow");
            } catch (Exception e) {

            }
        }
    }
}
