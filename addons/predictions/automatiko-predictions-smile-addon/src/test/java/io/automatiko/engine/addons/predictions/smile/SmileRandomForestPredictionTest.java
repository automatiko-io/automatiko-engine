package io.automatiko.engine.addons.predictions.smile;

import static io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.automatiko.engine.addons.predictions.api.PredictionAwareHumanTaskLifeCycle;
import io.automatiko.engine.addons.predictions.api.PredictionService;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.ProcessConfig;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.services.io.ClassPathResource;
import io.automatiko.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatiko.engine.services.uow.DefaultUnitOfWorkManager;
import io.automatiko.engine.workflow.CachedWorkItemHandlerConfig;
import io.automatiko.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatiko.engine.workflow.StaticProcessConfig;
import io.automatiko.engine.workflow.base.instance.context.variable.DefaultVariableInitializer;
import io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskWorkItemHandler;
import io.automatiko.engine.workflow.bpmn2.BpmnProcess;
import io.automatiko.engine.workflow.bpmn2.BpmnVariables;

public class SmileRandomForestPredictionTest {

    private PredictionService predictionService;

    private ProcessConfig config;

    @BeforeEach
    public void configure() {

        final RandomForestConfiguration configuration = new RandomForestConfiguration();

        final Map<String, AttributeType> inputFeatures = new HashMap<>();
        inputFeatures.put("ActorId", AttributeType.NOMINAL);
        configuration.setInputFeatures(inputFeatures);

        configuration.setOutcomeName("output");
        configuration.setOutcomeType(AttributeType.NOMINAL);
        configuration.setConfidenceThreshold(0.7);
        configuration.setNumTrees(1);

        predictionService = new SmileRandomForest(configuration);
        CachedWorkItemHandlerConfig wiConfig = new CachedWorkItemHandlerConfig();
        wiConfig.register("Human Task",
                new HumanTaskWorkItemHandler(new PredictionAwareHumanTaskLifeCycle(predictionService)));
        config = new StaticProcessConfig(wiConfig, new DefaultProcessEventListenerConfig(),
                new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()), null,
                new DefaultVariableInitializer(), null);

        for (int i = 0; i < 10; i++) {
            predictionService.train(null, Collections.singletonMap("ActorId", "john"),
                    Collections.singletonMap("output", "predicted value"));
        }
        for (int i = 0; i < 8; i++) {
            predictionService.train(null, Collections.singletonMap("ActorId", "mary"),
                    Collections.singletonMap("output", "value"));
        }
    }

    @Test
    public void testUserTaskWithPredictionService() {

        BpmnProcess process = (BpmnProcess) BpmnProcess.from(config, new ClassPathResource("BPMN2-UserTask.bpmn2"))
                .get(0);
        process.configure();

        ProcessInstance<BpmnVariables> processInstance = process
                .createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

        processInstance.start();
        assertEquals(STATE_COMPLETED, processInstance.status());

        Model result = (Model) processInstance.variables();
        assertEquals(2, result.toMap().size());
        assertEquals("predicted value", result.toMap().get("s"));

    }
}
