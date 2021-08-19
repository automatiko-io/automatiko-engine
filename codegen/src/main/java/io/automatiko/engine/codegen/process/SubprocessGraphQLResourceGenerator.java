
package io.automatiko.engine.codegen.process;

import java.util.Collections;
import java.util.List;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.GeneratorContext;

public class SubprocessGraphQLResourceGenerator extends AbstractResourceGenerator {

    private static final String RESOURCE_TEMPLATE = "/class-templates/SubprocessGraphQLResourceTemplate.java";

    public SubprocessGraphQLResourceGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn,
            String processfqcn, String appCanonicalName) {
        super(context, process, modelfqcn, processfqcn, appCanonicalName, "GraphQLResource");
    }

    @Override
    protected String getResourceTemplate() {
        return RESOURCE_TEMPLATE;
    }

    @Override
    public String getUserTaskResourceTemplate() {
        return "/class-templates/SubprocessGraphQLResourceUserTaskTemplate.java";
    }

    @Override
    protected String getSignalResourceTemplate() {
        return "/class-templates/SubprocessGraphQLResourceSignalTemplate.java";
    }

    @Override
    public List<String> getRestAnnotations() {
        return Collections.emptyList();
    }

}