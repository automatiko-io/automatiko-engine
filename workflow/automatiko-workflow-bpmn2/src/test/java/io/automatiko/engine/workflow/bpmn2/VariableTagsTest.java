
package io.automatiko.engine.workflow.bpmn2;

import static io.automatiko.engine.api.workflow.ProcessInstance.STATE_ABORTED;
import static io.automatiko.engine.api.workflow.ProcessInstance.STATE_ACTIVE;
import static io.automatiko.engine.api.workflow.ProcessInstance.STATE_COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.event.process.DefaultProcessEventListener;
import io.automatiko.engine.api.event.process.ProcessVariableChangedEvent;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.workflow.ProcessConfig;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.VariableViolationException;
import io.automatiko.engine.workflow.bpmn2.BpmnProcess;
import io.automatiko.engine.workflow.bpmn2.BpmnVariables;
import io.automatiko.engine.workflow.bpmn2.objects.TestWorkItemHandler;

public class VariableTagsTest extends JbpmBpmn2TestCase {

	@Test
	public void testProcessWithMissingRequiredVariable() throws Exception {

		TestWorkItemHandler workItemHandler = new TestWorkItemHandler("Human Task");

		ProcessConfig config = config(workItemHandler);
		BpmnProcess process = create(config, "variable-tags/approval-with-required-variable-tags.bpmn2");
		assertThrows(VariableViolationException.class, () -> process.createInstance(BpmnVariables.create()));

	}

	@Test
	public void testProcessWithRequiredVariable() throws Exception {
		TestWorkItemHandler workItemHandler = new TestWorkItemHandler("Human Task");

		ProcessConfig config = config(workItemHandler);
		BpmnProcess process = create(config, "variable-tags/approval-with-required-variable-tags.bpmn2");

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("approver", "john");

		ProcessInstance<BpmnVariables> instance = process.createInstance(BpmnVariables.create(parameters));
		instance.start();

		assertEquals(STATE_ACTIVE, instance.status());

		WorkItem workItem = workItemHandler.getWorkItem();
		assertNotNull(workItem);
		instance.completeWorkItem(workItem.getId(), null);

		workItem = workItemHandler.getWorkItem();
		assertNotNull(workItem);
		instance.completeWorkItem(workItem.getId(), null);

		assertEquals(STATE_COMPLETED, instance.status());
	}

	@Test
	public void testProcessWithReadonlyVariable() throws Exception {
		TestWorkItemHandler workItemHandler = new TestWorkItemHandler("Human Task");

		ProcessConfig config = config(workItemHandler);
		BpmnProcess process = create(config, "variable-tags/approval-with-readonly-variable-tags.bpmn2");

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("approver", "john");

		ProcessInstance<BpmnVariables> instance = process.createInstance(BpmnVariables.create(parameters));
		instance.start();

		assertEquals(STATE_ACTIVE, instance.status());
		WorkItem workItem = workItemHandler.getWorkItem();
		assertNotNull(workItem);

		assertThrows(VariableViolationException.class,
				() -> instance.completeWorkItem(workItem.getId(), Collections.singletonMap("ActorId", "john")));

		instance.abort();

		assertEquals(STATE_ABORTED, instance.status());
	}

	@Test
	public void testProcessWithCustomVariableTag() throws Exception {

		TestWorkItemHandler workItemHandler = new TestWorkItemHandler("Human Task");

		DefaultProcessEventListener listener = new DefaultProcessEventListener() {

			@Override
			public void beforeVariableChanged(ProcessVariableChangedEvent event) {
				if (event.hasTag("onlyAdmin")) {
					throw new VariableViolationException(event.getProcessInstance().getId(), event.getVariableId(),
							"Variable can only be set by admins");
				}
			}

		};

		ProcessConfig config = config(Collections.singletonList(workItemHandler), Collections.singletonList(listener));
		BpmnProcess process = create(config, "variable-tags/approval-with-custom-variable-tags.bpmn2");
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("approver", "john");

		assertThrows(VariableViolationException.class, () -> process.createInstance(BpmnVariables.create(parameters)));

	}

	@Test
	public void testRequiredVariableFiltering() {
		BpmnProcess process = create("variable-tags/approval-with-custom-variable-tags.bpmn2");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("approver", "john");

		ProcessInstance<BpmnVariables> instance = process.createInstance(BpmnVariables.create(params));
		instance.start();

		assertEquals(STATE_ACTIVE, instance.status());

		assertThat(instance.variables().toMap()).hasSize(1);
		assertThat(instance.variables().toMap(BpmnVariables.OUTPUTS_ONLY)).hasSize(0);
		assertThat(instance.variables().toMap(BpmnVariables.INPUTS_ONLY)).hasSize(0);
		assertThat(instance.variables().toMap(BpmnVariables.INTERNAL_ONLY)).hasSize(0);
		assertThat(instance.variables().toMap(v -> v.hasTag("onlyAdmin"))).hasSize(1).containsEntry("approver", "john");

		instance.abort();

		assertEquals(STATE_ABORTED, instance.status());
	}
}
