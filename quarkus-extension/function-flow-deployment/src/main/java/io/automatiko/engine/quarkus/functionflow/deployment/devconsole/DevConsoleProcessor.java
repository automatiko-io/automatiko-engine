package io.automatiko.engine.quarkus.functionflow.deployment.devconsole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.microprofile.openapi.models.media.Schema;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.automatiko.engine.quarkus.functionflow.deployment.AutomatikoFunctionFlowProcessor;
import io.automatiko.engine.quarkus.functionflow.deployment.ExampleGenerator;
import io.automatiko.engine.quarkus.functionflow.dev.WorkflowFunctionFlowInfo;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.smallrye.openapi.runtime.io.schema.SchemaFactory;
import io.smallrye.openapi.runtime.scanner.SchemaRegistry;
import io.smallrye.openapi.runtime.scanner.spi.AnnotationScannerContext;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem collectWorkflowInfo(CombinedIndexBuildItem index) throws Exception {
        List<WorkflowFunctionFlowInfo> infos = new ArrayList<WorkflowFunctionFlowInfo>();
        Collection<AnnotationInstance> functions = index.getIndex()
                .getAnnotations(AutomatikoFunctionFlowProcessor.createDotName("io.quarkus.funqy.Funq"));
        DotName mapping = AutomatikoFunctionFlowProcessor.createDotName("io.quarkus.funqy.knative.events.CloudEventMapping");

        ExampleGenerator generator = new ExampleGenerator();
        AnnotationScannerContext ctx = AutomatikoFunctionFlowProcessor.buildAnnotationScannerContext(index.getIndex());
        SchemaRegistry.newInstance(ctx);

        for (AnnotationInstance f : functions) {
            if (f.target().kind().equals(Kind.METHOD)) {
                MethodInfo mi = f.target().asMethod();
                // create function trigger descriptor for every found function

                Map<String, String> filterAttributes = new HashMap<>();
                if (mi.annotation(mapping).value("attributes") != null) {
                    for (AnnotationValue aValue : ((AnnotationValue[]) mi.annotation(mapping).value("attributes").value())) {
                        AnnotationInstance attribute = (AnnotationInstance) aValue.value();
                        filterAttributes.put(attribute.value("name").asString(), attribute.value().asString());
                    }
                }

                SchemaFactory.typeToSchema(ctx,
                        mi.parameters().get(0), Collections.emptyList());
                Schema fSchema = ctx.getOpenApi().getComponents().getSchemas().get(mi.parameters().get(0).name().local());

                String payload = generator.generate(fSchema, ctx.getOpenApi());
                String type = mi.annotation(mapping).value("trigger").asString();
                String source = filterAttributes.getOrDefault("source", "string");
                String id = UUID.randomUUID().toString();
                String specversion = "1.0";

                // remove CE attribute as filters
                filterAttributes.remove("type");
                filterAttributes.remove("source");
                filterAttributes.remove("data");

                StringBuilder binaryInstructions = new StringBuilder();
                binaryInstructions.append("HTTP headers:").append("\n");
                binaryInstructions.append("ce-id=").append(id).append("\n");
                binaryInstructions.append("ce-specversion=").append(specversion).append("\n");
                binaryInstructions.append("ce-type=").append(type).append("\n");
                binaryInstructions.append("ce-source=").append(source).append("\n");

                for (Entry<String, String> entry : filterAttributes.entrySet()) {
                    binaryInstructions.append("ce-").append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
                }

                binaryInstructions.append("\nPOST payload:").append("\n");
                binaryInstructions.append(payload).append("\n");

                StringBuilder structuredInstructions = new StringBuilder();
                structuredInstructions.append("{").append("\n");
                structuredInstructions.append("  \"id\": \"").append(id).append("\",\n");
                structuredInstructions.append("  \"specversion\": \"").append(specversion).append("\",\n");
                structuredInstructions.append("  \"type\": \"").append(type).append("\",\n");
                structuredInstructions.append("  \"source\": \"").append(source).append("\",\n");

                for (Entry<String, String> entry : filterAttributes.entrySet()) {
                    binaryInstructions.append("  \"" + entry.getKey() + "\": \"").append(entry.getValue()).append("\",\n");
                }

                structuredInstructions.append("  \"data\":").append("\n");
                structuredInstructions.append(payload).append("\n");
                structuredInstructions.append("}").append("\n");

                infos.add(new WorkflowFunctionFlowInfo(mi.name(), "/",
                        binaryInstructions.toString(),
                        structuredInstructions.toString()));
            }
        }
        SchemaRegistry.remove();

        return new DevConsoleTemplateInfoBuildItem("workflowFunctionFlowInfos", infos);
    }
}
