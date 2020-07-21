
package io.automatik.engine.workflow.serverless;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.automatik.engine.workflow.base.core.Work;
import io.automatik.engine.workflow.process.core.impl.ConstraintImpl;
import io.automatik.engine.workflow.process.core.impl.ExtendedNodeImpl;
import io.automatik.engine.workflow.process.core.node.ActionNode;
import io.automatik.engine.workflow.process.core.node.CompositeContextNode;
import io.automatik.engine.workflow.process.core.node.EndNode;
import io.automatik.engine.workflow.process.core.node.EventNode;
import io.automatik.engine.workflow.process.core.node.HumanTaskNode;
import io.automatik.engine.workflow.process.core.node.Join;
import io.automatik.engine.workflow.process.core.node.Split;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.core.node.SubProcessNode;
import io.automatik.engine.workflow.process.core.node.TimerNode;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatik.engine.workflow.process.executable.core.Metadata;
import io.automatik.engine.workflow.serverless.api.end.End;
import io.automatik.engine.workflow.serverless.api.events.EventDefinition;
import io.automatik.engine.workflow.serverless.api.functions.Function;
import io.automatik.engine.workflow.serverless.api.produce.ProduceEvent;

public class WorkflowFactoryTest extends BaseServerlessTest {

	@Test
	public void testCreateProcess() {
		ExecutableProcess process = testFactory.createProcess(singleInjectStateWorkflow);
		assertThat(process).isNotNull();
		assertThat(process.getId()).isEqualTo("serverless");
		assertThat(process.getName()).isEqualTo("workflow");
		assertThat(process.getVersion()).isEqualTo("1.0");
		assertThat(process.getPackageName()).isEqualTo("org.kie.kogito.serverless");
		assertThat(process.isAutoComplete()).isTrue();
		assertThat(process.getVisibility()).isEqualTo("Public");
		assertThat(process.getImports()).isNotNull();
		assertThat(process.getVariableScope()).isNotNull();
		assertThat(process.getVariableScope().getVariables()).isNotNull();
		assertThat(process.getVariableScope().getVariables()).hasSize(1);
	}

	@Test
	public void testStartNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();
		StartNode startNode = testFactory.startNode(1L, "start", nodeContainer);
		assertThat(startNode).isNotNull();
		assertThat(startNode.getName()).isEqualTo("start");
	}

	@Test
	public void testMessageStartNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();
		EventDefinition eventDefinition = new EventDefinition().withName("testEvent").withSource("testSource")
				.withType("testType");
		StartNode startNode = testFactory.messageStartNode(1L, eventDefinition, nodeContainer);

		assertThat(startNode).isNotNull();
		assertThat(startNode.getName()).isEqualTo(eventDefinition.getName());
		assertThat(startNode.getMetaData()).isNotNull();
		assertThat(startNode.getMetaData().get(Metadata.TRIGGER_TYPE)).isEqualTo("ConsumeMessage");
		assertThat(startNode.getMetaData().get(Metadata.TRIGGER_REF)).isEqualTo(eventDefinition.getSource());
		assertThat(startNode.getMetaData().get(Metadata.MESSAGE_TYPE))
				.isEqualTo("com.fasterxml.jackson.databind.JsonNode");

		assertThat(startNode.getTriggers()).isNotNull();
		assertThat(startNode.getTriggers().size()).isEqualTo(1);
	}

	@Test
	public void testEndNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();
		EndNode endNode = testFactory.endNode(1L, "end", true, nodeContainer);
		assertThat(endNode).isNotNull();
		assertThat(endNode.getName()).isEqualTo("end");
	}

	@Test
	public void testMessageEndNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();

		End endDef = new End().withKind(End.Kind.EVENT)
				.withProduceEvent(new ProduceEvent().withEventRef("sampleEvent").withData("sampleData"));

		EndNode endNode = testFactory.messageEndNode(1L, "End", eventDefOnlyWorkflow, endDef, nodeContainer);

		assertThat(endNode).isNotNull();
		assertThat(endNode.getName()).isEqualTo("End");
		assertThat(endNode.getMetaData()).isNotNull();
		assertThat(endNode.getMetaData().get(Metadata.TRIGGER_REF)).isEqualTo("sampleSource");
		assertThat(endNode.getMetaData().get(Metadata.TRIGGER_TYPE)).isEqualTo("ProduceMessage");
		assertThat(endNode.getMetaData().get(Metadata.MESSAGE_TYPE))
				.isEqualTo("com.fasterxml.jackson.databind.JsonNode");
		assertThat(endNode.getMetaData().get(Metadata.MAPPING_VARIABLE)).isEqualTo("workflowdata");

		assertThat(endNode.getActions(ExtendedNodeImpl.EVENT_NODE_ENTER)).isNotNull();
		assertThat(endNode.getActions(ExtendedNodeImpl.EVENT_NODE_ENTER).size()).isEqualTo(1);
	}

	@Test
	public void testTimerNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();

		TimerNode timerNode = testFactory.timerNode(1L, "timer", "sampleDelay", nodeContainer);
		assertThat(timerNode).isNotNull();
		assertThat(timerNode.getName()).isEqualTo("timer");
		assertThat(timerNode.getMetaData().get("EventType")).isEqualTo("timer");
	}

	@Test
	public void testCallActivity() {
		TestNodeContainer nodeContainer = new TestNodeContainer();

		SubProcessNode subProcessNode = testFactory.callActivity(1L, "subprocess", "calledId", true, nodeContainer);
		assertThat(subProcessNode).isNotNull();
		assertThat(subProcessNode.getName()).isEqualTo("subprocess");
		assertThat(subProcessNode.getProcessId()).isEqualTo("calledId");
		assertThat(subProcessNode.getInMappings()).isNotNull();
		assertThat(subProcessNode.getInMappings()).hasSize(1);
		assertThat(subProcessNode.getOutMappings()).isNotNull();
		assertThat(subProcessNode.getOutMappings()).hasSize(1);
		assertThat(subProcessNode.getMetaData("BPMN.InputTypes")).isNotNull();
		assertThat(subProcessNode.getMetaData("BPMN.OutputTypes")).isNotNull();
	}

	@Test
	public void testMessageEndNodeAction() {
		TestNodeContainer nodeContainer = new TestNodeContainer();
		EndNode endNode = testFactory.endNode(1L, "end", true, nodeContainer);
		assertThat(endNode).isNotNull();
		assertThat(endNode.getName()).isEqualTo("end");

		testFactory.addMessageEndNodeAction(endNode, "testVar", "testMessageType");
		assertThat(endNode.getActions(ExtendedNodeImpl.EVENT_NODE_ENTER)).hasSize(1);
	}

	@Test
	public void testAddTriggerToStartNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();
		StartNode startNode = testFactory.startNode(1L, "start", nodeContainer);
		assertThat(startNode).isNotNull();
		assertThat(startNode.getName()).isEqualTo("start");

		testFactory.addTriggerToStartNode(startNode, "testTriggerType");
		assertThat(startNode.getTriggers()).hasSize(1);
	}

	@Test
	public void testScriptNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();

		ActionNode actionNode = testFactory.scriptNode(1L, "script", "testScript", nodeContainer);
		assertThat(actionNode).isNotNull();
		assertThat(actionNode.getName()).isEqualTo("script");
		assertThat(actionNode.getAction()).isNotNull();
	}

	@Test
	public void testServiceNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();
		Function function = new Function().withName("testFunction").withType("testType").withResource("testResource")
				.withMetadata(new HashMap<String, String>() {
					{
						put("interface", "testInterface");
						put("operation", "testOperation");
						put("implementation", "testImplementation");
					}
				});
		WorkItemNode workItemNode = testFactory.serviceNode(1L, "testService", function, nodeContainer);
		assertThat(workItemNode).isNotNull();
		assertThat(workItemNode.getName()).isEqualTo("testService");
		assertThat(workItemNode.getMetaData().get("Type")).isEqualTo("Service Task");
		assertThat(workItemNode.getWork()).isNotNull();

		Work work = workItemNode.getWork();
		assertThat(work.getName()).isEqualTo("Service Task");
		assertThat(work.getParameter("Interface")).isEqualTo("testInterface");
		assertThat(work.getParameter("Operation")).isEqualTo("testOperation");
		assertThat(work.getParameter("interfaceImplementationRef")).isEqualTo("testInterface");
		assertThat(work.getParameter("operationImplementationRef")).isEqualTo("testOperation");
		assertThat(work.getParameter("ParameterType")).isEqualTo("com.fasterxml.jackson.databind.JsonNode");
		assertThat(work.getParameter("implementation")).isEqualTo("testImplementation");

		assertThat(workItemNode.getInMappings()).isNotNull();
		assertThat(workItemNode.getInMappings()).hasSize(1);
		assertThat(workItemNode.getOutMappings()).isNotNull();
		assertThat(workItemNode.getOutMappings()).hasSize(1);
	}

	@Test
	public void testSubProcessNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();
		CompositeContextNode compositeContextNode = testFactory.subProcessNode(1L, "subprocess", nodeContainer);
		assertThat(compositeContextNode).isNotNull();
		assertThat(compositeContextNode.getName()).isEqualTo("subprocess");
		assertThat(compositeContextNode.isAutoComplete()).isTrue();
	}

	@Test
	public void testSplitConstraint() {
		ConstraintImpl constraint = testFactory.splitConstraint("testName", "testType", "testDialect", "testConstraint",
				0, true);
		assertThat(constraint).isNotNull();
		assertThat(constraint.getName()).isEqualTo("testName");
		assertThat(constraint.getType()).isEqualTo("testType");
		assertThat(constraint.getDialect()).isEqualTo("testDialect");
		assertThat(constraint.getConstraint()).isEqualTo("testConstraint");
		assertThat(constraint.getPriority()).isEqualTo(0);
		assertThat(constraint.isDefault()).isTrue();
	}

	@Test
	public void testSplitNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();

		Split split = testFactory.splitNode(1L, "testSplit", Split.TYPE_XOR, nodeContainer);
		assertThat(split).isNotNull();
		assertThat(split.getId()).isEqualTo(1L);
		assertThat(split.getName()).isEqualTo("testSplit");
		assertThat(split.getType()).isEqualTo(Split.TYPE_XOR);
		assertThat(split.getMetaData().get("UniqueId")).isEqualTo("1");
	}

	@Test
	public void testEventBasedSplitNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();

		Split split = testFactory.eventBasedSplit(1L, "testSplit", nodeContainer);
		assertThat(split).isNotNull();
		assertThat(split.getId()).isEqualTo(1L);
		assertThat(split.getName()).isEqualTo("testSplit");
		assertThat(split.getType()).isEqualTo(Split.TYPE_XAND);
		assertThat(split.getMetaData().get("UniqueId")).isEqualTo("1");
		assertThat(split.getMetaData().get("EventBased")).isEqualTo("true");
	}

	@Test
	public void testJoinNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();

		Join join = testFactory.joinNode(1L, "testJoin", Join.TYPE_XOR, nodeContainer);
		assertThat(join).isNotNull();
		assertThat(join.getId()).isEqualTo(1L);
		assertThat(join.getName()).isEqualTo("testJoin");
		assertThat(join.getType()).isEqualTo(Join.TYPE_XOR);
		assertThat(join.getMetaData().get("UniqueId")).isEqualTo("1");
	}

	@Test
	public void testProcessVar() {
		ExecutableProcess process = new ExecutableProcess();
		testFactory.processVar("testVar", JsonNode.class, process);

		assertThat(process.getVariableScope()).isNotNull();
		assertThat(process.getVariableScope().getVariables()).isNotNull();
		assertThat(process.getVariableScope().getVariables().size()).isEqualTo(1);
	}

	@Test
	public void testHumanTaskNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();
		ExecutableProcess process = new ExecutableProcess();

		Map<String, String> metaMap = new HashMap<>();
		metaMap.put("taskname", "testTaskName");
		metaMap.put("skippable", "false");
		metaMap.put("groupid", "testGroupId");
		metaMap.put("actorid", "testActorId");
		Function function = new Function().withName("testfunction1").withMetadata(metaMap);

		HumanTaskNode humanTaskNode = testFactory.humanTaskNode(1L, "test name", function, process, nodeContainer);

		assertThat(humanTaskNode).isNotNull();
		assertThat(humanTaskNode.getWork().getParameter("TaskName")).isEqualTo("testTaskName");
		assertThat(humanTaskNode.getWork().getParameter("Skippable")).isEqualTo("false");
		assertThat(humanTaskNode.getWork().getParameter("GroupId")).isEqualTo("testGroupId");
		assertThat(humanTaskNode.getWork().getParameter("ActorId")).isEqualTo("testActorId");
		assertThat(humanTaskNode.getWork().getParameter("NodeName")).isEqualTo("test name");

		assertThat(humanTaskNode.getInMappings()).isNotNull();
		assertThat(humanTaskNode.getInMappings().size()).isEqualTo(1);
		assertThat(humanTaskNode.getOutMappings()).isNotNull();
		assertThat(humanTaskNode.getOutMappings().size()).isEqualTo(1);

		assertThat(process.getVariableScope().getVariables()).isNotNull();
		assertThat(process.getVariableScope().getVariables().size()).isEqualTo(1);

	}

	@Test
	public void testHumanTaskNodeDefaultValues() {
		TestNodeContainer nodeContainer = new TestNodeContainer();
		ExecutableProcess process = new ExecutableProcess();

		Function function = new Function().withName("testfunction1");

		HumanTaskNode humanTaskNode = testFactory.humanTaskNode(1L, "test name", function, process, nodeContainer);

		assertThat(humanTaskNode).isNotNull();
		assertThat(humanTaskNode.getWork().getParameter("TaskName")).isEqualTo("workflowhtask");
		assertThat(humanTaskNode.getWork().getParameter("Skippable")).isEqualTo("true");
		assertThat(humanTaskNode.getWork().getParameter("GroupId")).isNull();
		assertThat(humanTaskNode.getWork().getParameter("ActorId")).isNull();
		assertThat(humanTaskNode.getWork().getParameter("NodeName")).isEqualTo("test name");

		assertThat(humanTaskNode.getInMappings()).isNotNull();
		assertThat(humanTaskNode.getInMappings().size()).isEqualTo(1);
		assertThat(humanTaskNode.getOutMappings()).isNotNull();
		assertThat(humanTaskNode.getOutMappings().size()).isEqualTo(1);

		assertThat(process.getVariableScope().getVariables()).isNotNull();
		assertThat(process.getVariableScope().getVariables().size()).isEqualTo(1);
	}

	@Test
	public void testSendEventNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();

		EventDefinition eventDefinition = new EventDefinition().withName("testEvent").withSource("testSource")
				.withType("testType");

		ActionNode actionNode = testFactory.sendEventNode(1L, eventDefinition, nodeContainer);
		assertThat(actionNode).isNotNull();
		assertThat(actionNode.getName()).isEqualTo("testEvent");
		assertThat(actionNode.getMetaData(Metadata.TRIGGER_TYPE)).isEqualTo("ProduceMessage");
		assertThat(actionNode.getMetaData(Metadata.MAPPING_VARIABLE)).isEqualTo("workflowdata");
		assertThat(actionNode.getMetaData(Metadata.TRIGGER_REF)).isEqualTo("testSource");
		assertThat(actionNode.getMetaData(Metadata.MESSAGE_TYPE)).isEqualTo("com.fasterxml.jackson.databind.JsonNode");

	}

	@Test
	public void testConsumeEventNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();

		EventDefinition eventDefinition = new EventDefinition().withName("testEvent").withSource("testSource")
				.withType("testType");

		EventNode eventNode = testFactory.consumeEventNode(1L, eventDefinition, nodeContainer);
		assertThat(eventNode).isNotNull();
		assertThat(eventNode.getName()).isEqualTo("testEvent");
		assertThat(eventNode.getVariableName()).isEqualTo("workflowdata");
		assertThat(eventNode.getType()).isEqualTo("Message-" + eventDefinition.getSource());
		assertThat(eventNode.getMetaData(Metadata.TRIGGER_TYPE)).isEqualTo("ConsumeMessage");
		assertThat(eventNode.getMetaData(Metadata.EVENT_TYPE)).isEqualTo("message");
		assertThat(eventNode.getMetaData(Metadata.TRIGGER_REF)).isEqualTo(eventDefinition.getSource());
		assertThat(eventNode.getMetaData(Metadata.MESSAGE_TYPE)).isEqualTo("com.fasterxml.jackson.databind.JsonNode");
	}

	@Test
	public void testCamelRouteServiceNode() {
		TestNodeContainer nodeContainer = new TestNodeContainer();
		Function function = new Function().withName("testFunction").withType("testType").withResource("testResource")
				.withMetadata(new HashMap<String, String>() {
					{
						put("endpoint", "direct:testendpoint");
					}
				});
		WorkItemNode workItemNode = testFactory.camelRouteServiceNode(1L, "testService", function, nodeContainer);
		assertThat(workItemNode).isNotNull();
		assertThat(workItemNode.getName()).isEqualTo("testService");
		assertThat(workItemNode.getMetaData().get("Type")).isEqualTo("Service Task");
		assertThat(workItemNode.getWork()).isNotNull();

		Work work = workItemNode.getWork();
		assertThat(work.getName()).isEqualTo("org.apache.camel.ProducerTemplate.requestBody");
		assertThat(work.getParameter("endpoint")).isEqualTo("direct:testendpoint");
		assertThat(work.getParameter("Interface")).isEqualTo("org.apache.camel.ProducerTemplate");
		assertThat(work.getParameter("Operation")).isEqualTo("requestBody");
		assertThat(work.getParameter("interfaceImplementationRef")).isEqualTo("org.apache.camel.ProducerTemplate");

		assertThat(workItemNode.getInMappings()).isNotNull();
		assertThat(workItemNode.getInMappings()).hasSize(1);
		assertThat(workItemNode.getOutMappings()).isNotNull();
		assertThat(workItemNode.getOutMappings()).hasSize(1);
	}

}
