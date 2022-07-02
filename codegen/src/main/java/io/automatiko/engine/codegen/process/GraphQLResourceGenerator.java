
package io.automatiko.engine.codegen.process;

import java.util.Arrays;
import java.util.List;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.services.utils.StringUtils;

public class GraphQLResourceGenerator extends AbstractResourceGenerator {

    private static final String RESOURCE_TEMPLATE = "/class-templates/GraphQLResourceTemplate.java";

    public GraphQLResourceGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn, String processfqcn,
            String appCanonicalName) {
        super(context, process, modelfqcn, processfqcn, appCanonicalName, "GraphQLResource");
    }

    @Override
    protected String getResourceTemplate() {
        return RESOURCE_TEMPLATE;
    }

    @Override
    public String getUserTaskResourceTemplate() {
        return "/class-templates/GraphQLResourceUserTaskTemplate.java";
    }

    @Override
    protected String getSignalResourceTemplate() {
        return "/class-templates/GraphQLResourceSignalTemplate.java";
    }

    @Override
    public List<String> getRestAnnotations() {
        return Arrays.asList();
    }

    @Override
    public void collectSubProcessModels(String dataClassName, ClassOrInterfaceDeclaration template,
            List<AbstractResourceGenerator> subprocessGenerators) {
        String collectModelTemplate = "pi.subprocesses().stream().filter(p -> p.process().id().equals(\"$piId$\")).forEach(p -> output.add$UpId$(getSubModel_$modelpiId$((ProcessInstance<$Type$>) p)));";

        for (AbstractResourceGenerator generator : subprocessGenerators) {
            MethodDeclaration mapOutput = template
                    .findFirst(MethodDeclaration.class, m -> {
                        return m.getNameAsString().equals("mapOutput")
                                && m.getParameter(0).getTypeAsString().equals(dataClassName);
                    })
                    .get();
            BlockStmt body = mapOutput.getBody().get();

            body.findFirst(ReturnStmt.class).ifPresent(r -> r.remove());
            String sModel = collectModelTemplate.replaceAll("\\$piId\\$", generator.processId() + generator.version())
                    .replaceAll("\\$UpId\\$", StringUtils.capitalize(generator.processId() + generator.version()))
                    .replaceAll("\\$modelpiId\\$", generator.processId())
                    .replaceAll("\\$Type\\$",
                            generator.generatorModelClass());

            body.addStatement(StaticJavaParser.parseStatement(sModel));
            body.addStatement(new ReturnStmt(new NameExpr("output")));
        }
    }
}