package io.automatiko.engine.quarkus.function.deployment.devconsole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.media.Schema;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.MethodInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatiko.engine.function.dev.WorkflowFunctionInfo;
import io.automatiko.engine.quarkus.function.deployment.AutomatikoFunctionProcessor;
import io.automatiko.engine.quarkus.function.deployment.ExampleGenerator;
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
        List<WorkflowFunctionInfo> infos = new ArrayList<WorkflowFunctionInfo>();
        Collection<AnnotationInstance> functions = index.getIndex()
                .getAnnotations(AutomatikoFunctionProcessor.createDotName("io.quarkus.funqy.Funq"));
        ObjectMapper mapper = new ObjectMapper();
        ExampleGenerator generator = new ExampleGenerator();
        AnnotationScannerContext ctx = AutomatikoFunctionProcessor.buildAnnotationScannerContext(index.getIndex());
        SchemaRegistry.newInstance(ctx);

        for (AnnotationInstance f : functions) {
            if (f.target().kind().equals(Kind.METHOD)) {
                MethodInfo mi = f.target().asMethod();
                // create function trigger descriptor for every found function

                SchemaFactory.typeToSchema(ctx,
                        mi.parameters().get(0), Collections.emptyList());
                Schema fSchema = ctx.getOpenApi().getComponents().getSchemas().get(mi.parameters().get(0).name().local());

                Map<String, Object> example = generator.generate(fSchema, ctx.getOpenApi());

                String putInstructions = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(example);

                StringBuilder getInstructions = new StringBuilder();
                AutomatikoFunctionProcessor.flatMap(null, example).entrySet()
                        .forEach(e -> getInstructions.append(e.getKey() + "=" + e.getValue() + "&"));
                infos.add(new WorkflowFunctionInfo(mi.name(), "/" + mi.name(),
                        getInstructions.deleteCharAt(getInstructions.length() - 1).toString(),
                        putInstructions));
            }
        }
        SchemaRegistry.remove();

        return new DevConsoleTemplateInfoBuildItem("workflowFunctionInfos", infos);
    }
}
