package io.automatik.engine.addons.predictions.api;

import static io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static io.automatik.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.automatik.engine.api.Model;
import io.automatik.engine.api.auth.SecurityPolicy;
import io.automatik.engine.api.workflow.ProcessConfig;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.WorkItem;
import io.automatik.engine.api.workflow.workitem.Policy;
import io.automatik.engine.services.identity.StaticIdentityProvider;
import io.automatik.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatik.engine.services.uow.DefaultUnitOfWorkManager;
import io.automatik.engine.workflow.CachedWorkItemHandlerConfig;
import io.automatik.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatik.engine.workflow.StaticProcessConfig;
import io.automatik.engine.workflow.base.core.resources.ClassPathResource;
import io.automatik.engine.workflow.base.instance.context.variable.DefaultVariableInitializer;
import io.automatik.engine.workflow.base.instance.impl.humantask.HumanTaskWorkItemHandler;
import io.automatik.engine.workflow.bpmn2.BpmnProcess;
import io.automatik.engine.workflow.bpmn2.BpmnVariables;

public class PredictionAwareHumanTaskLifeCycleTest {

	private Policy<?> securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));

	private AtomicBoolean predictNow;
	private List<String> trainedTasks;

	private PredictionService predictionService;

	private ProcessConfig config;

	@BeforeEach
	public void configure() {

		predictNow = new AtomicBoolean(false);
		trainedTasks = new ArrayList<>();

		predictionService = new PredictionService() {

			@Override
			public void train(io.automatik.engine.api.runtime.process.WorkItem task, Map<String, Object> inputData,
					Map<String, Object> outputData) {
				trainedTasks.add(task.getId());
			}

			@Override
			public PredictionOutcome predict(io.automatik.engine.api.runtime.process.WorkItem task,
					Map<String, Object> inputData) {
				if (predictNow.get()) {
					return new PredictionOutcome(95, 75, Collections.singletonMap("output", "predicted value"));
				}

				return new PredictionOutcome();
			}

			@Override
			public String getIdentifier() {
				return "test";
			}
		};

		CachedWorkItemHandlerConfig wiConfig = new CachedWorkItemHandlerConfig();
		wiConfig.register("Human Task",
				new HumanTaskWorkItemHandler(new PredictionAwareHumanTaskLifeCycle(predictionService)));
		config = new StaticProcessConfig(wiConfig, new DefaultProcessEventListenerConfig(),
				new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()), null,
				new DefaultVariableInitializer());
	}

	@Test
	public void testUserTaskWithPredictionService() {
		predictNow.set(true);

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

		assertEquals(0, trainedTasks.size());

	}

	@Test
	public void testUserTaskWithoutPredictionService() {

		BpmnProcess process = (BpmnProcess) BpmnProcess.from(config, new ClassPathResource("BPMN2-UserTask.bpmn2"))
				.get(0);
		process.configure();

		ProcessInstance<BpmnVariables> processInstance = process
				.createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

		processInstance.start();
		assertEquals(STATE_ACTIVE, processInstance.status());

		WorkItem workItem = processInstance.workItems(securityPolicy).get(0);
		assertNotNull(workItem);
		assertEquals("john", workItem.getParameters().get("ActorId"));
		processInstance.completeWorkItem(workItem.getId(), Collections.singletonMap("output", "given value"),
				securityPolicy);
		assertEquals(STATE_COMPLETED, processInstance.status());

		Model result = (Model) processInstance.variables();
		assertEquals(2, result.toMap().size());
		assertEquals("given value", result.toMap().get("s"));

		assertEquals(1, trainedTasks.size());

	}
}
