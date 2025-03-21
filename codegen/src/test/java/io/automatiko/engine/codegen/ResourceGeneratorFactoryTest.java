
package io.automatiko.engine.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.context.QuarkusApplicationBuildContext;
import io.automatiko.engine.codegen.process.AbstractResourceGenerator;
import io.automatiko.engine.codegen.process.ReactiveResourceGenerator;
import io.automatiko.engine.codegen.process.ResourceGenerator;
import io.automatiko.engine.quarkus.AutomatikoBuildTimeConfig;

@ExtendWith(MockitoExtension.class)
class ResourceGeneratorFactoryTest {

    public static final String MODEL_FQCN = "modelfqcn";
    public static final String PROCESS_FQCN = "processfqcn";
    public static final String APP_CANONICAL_NAME = "appCanonicalName";

    private ResourceGeneratorFactory tested;

    @Mock
    private WorkflowProcess process;

    @Mock
    private AutomatikoBuildTimeConfig config;

    @BeforeEach
    public void setUp() {
        lenient().when(process.getId()).thenReturn("process.id");
        lenient().when(process.getPackageName()).thenReturn("name.process");
        tested = new ResourceGeneratorFactory();
    }

    @Test
    void testCreateQuarkus(@Mock GeneratorContext generatorContext) {
        when(generatorContext.getBuildContext())
                .thenReturn(new QuarkusApplicationBuildContext(config, p -> true,
                        c -> Collections.emptyList(), capability -> false));
        Optional<AbstractResourceGenerator> context = tested.create(generatorContext, process, MODEL_FQCN, PROCESS_FQCN,
                APP_CANONICAL_NAME);
        assertThat(context.isPresent()).isTrue();
        assertThat(context.get()).isExactlyInstanceOf(ResourceGenerator.class);
    }

    @Test
    void testCreateQuarkusReactive(@Mock GeneratorContext generatorContext) {
        when(generatorContext.getApplicationProperty(GeneratorConfig.REST_RESOURCE_TYPE_PROP))
                .thenReturn(Optional.of("reactive"));
        when(generatorContext.getBuildContext())
                .thenReturn(new QuarkusApplicationBuildContext(config, p -> true,
                        c -> Collections.emptyList(), capability -> false));

        Optional<AbstractResourceGenerator> context = tested.create(generatorContext, process, MODEL_FQCN, PROCESS_FQCN,
                APP_CANONICAL_NAME);
        assertThat(context.isPresent()).isTrue();
        assertThat(context.get()).isExactlyInstanceOf(ReactiveResourceGenerator.class);
    }
}