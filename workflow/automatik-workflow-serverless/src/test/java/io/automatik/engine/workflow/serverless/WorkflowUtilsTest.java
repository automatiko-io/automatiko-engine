
package io.automatik.engine.workflow.serverless;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatik.engine.workflow.serverless.api.Workflow;
import io.automatik.engine.workflow.serverless.api.events.EventDefinition;
import io.automatik.engine.workflow.serverless.api.functions.Function;
import io.automatik.engine.workflow.serverless.api.interfaces.State;
import io.automatik.engine.workflow.serverless.api.mapper.BaseObjectMapper;
import io.automatik.engine.workflow.serverless.api.mapper.JsonObjectMapper;
import io.automatik.engine.workflow.serverless.api.mapper.YamlObjectMapper;
import io.automatik.engine.workflow.serverless.api.states.DefaultState;
import io.automatik.engine.workflow.serverless.api.states.InjectState;
import io.automatik.engine.workflow.serverless.api.switchconditions.DataCondition;
import io.automatik.engine.workflow.serverless.parser.util.ServerlessWorkflowUtils;
import io.automatik.engine.workflow.serverless.parser.util.WorkflowAppContext;

public class WorkflowUtilsTest extends BaseServerlessTest {

	@Test
	public void testGetObjectMapper() {
		BaseObjectMapper objectMapper = ServerlessWorkflowUtils.getObjectMapper("json");
		assertNotNull(objectMapper);
		assertThat(objectMapper).isInstanceOf(JsonObjectMapper.class);

		objectMapper = ServerlessWorkflowUtils.getObjectMapper("yml");
		assertNotNull(objectMapper);
		assertThat(objectMapper).isInstanceOf(YamlObjectMapper.class);

		assertThrows(IllegalArgumentException.class, () -> {
			ServerlessWorkflowUtils.getObjectMapper("unsupported");
		});

		assertThrows(IllegalArgumentException.class, () -> {
			ServerlessWorkflowUtils.getObjectMapper(null);
		});

	}

	@Test
	public void testGetWorkflowStartState() {
		assertThat(ServerlessWorkflowUtils.getWorkflowStartState(singleInjectStateWorkflow)).isNotNull();
		assertThat(ServerlessWorkflowUtils.getWorkflowStartState(singleInjectStateWorkflow))
				.isInstanceOf(InjectState.class);

	}

	@Test
	public void testGetWorkflowEndStatesSingle() {
		List<State> endStates = ServerlessWorkflowUtils.getWorkflowEndStates(singleInjectStateWorkflow);
		assertThat(endStates).isNotNull();
		assertThat(endStates).hasSize(1);
		State endState = endStates.get(0);
		assertThat(endState).isNotNull();
		assertThat(endState).isInstanceOf(InjectState.class);
	}

	@Test
	public void testGetWorkflowEndStatesMulti() {
		List<State> endStates = ServerlessWorkflowUtils.getWorkflowEndStates(multiInjectStateWorkflow);
		assertThat(endStates).isNotNull();
		assertThat(endStates).hasSize(2);
		State endState1 = endStates.get(0);
		assertThat(endState1).isNotNull();
		assertThat(endState1).isInstanceOf(InjectState.class);
		State endState2 = endStates.get(1);
		assertThat(endState2).isNotNull();
		assertThat(endState2).isInstanceOf(InjectState.class);
	}

	@Test
	public void testGetStatesByType() {
		List<State> relayStates = ServerlessWorkflowUtils.getStatesByType(multiInjectStateWorkflow,
				DefaultState.Type.INJECT);
		assertThat(relayStates).isNotNull();
		assertThat(relayStates).hasSize(2);
		assertThat(relayStates.get(0)).isInstanceOf(InjectState.class);
		assertThat(relayStates.get(1)).isInstanceOf(InjectState.class);

		List<State> noOperationStates = ServerlessWorkflowUtils.getStatesByType(multiInjectStateWorkflow,
				DefaultState.Type.OPERATION);
		assertThat(noOperationStates).isNotNull();
		assertThat(noOperationStates).hasSize(0);
	}

	@Test
	public void testIncludesSupportedStates() {
		assertThat(ServerlessWorkflowUtils.includesSupportedStates(singleInjectStateWorkflow)).isTrue();
	}

	@Test
	public void testGetWorkflowEventFor() {
		assertThat(ServerlessWorkflowUtils.getWorkflowEventFor(eventDefOnlyWorkflow, "sampleEvent")).isNotNull();
		assertThat(ServerlessWorkflowUtils.getWorkflowEventFor(eventDefOnlyWorkflow, "sampleEvent"))
				.isInstanceOf(EventDefinition.class);
	}

	@Test
	public void testSysOutFunctionScript() {
		String script = "$.a $.b";
		assertThat(ServerlessWorkflowUtils.sysOutFunctionScript(script)).isNotNull();
	}

	@Test
	public void testGetJsonPathScript() {
		String script = "$.a $.b";
		assertThat(ServerlessWorkflowUtils.getJsonPathScript(script)).isNotNull();
	}

	@Test
	public void testGetInjectScript() throws Exception {
		String toInject = "{\n" + "  \"name\": \"john\"\n" + "}";

		ObjectMapper mapper = new ObjectMapper();
		JsonNode toInjectObj = mapper.readTree(toInject);

		assertThat(ServerlessWorkflowUtils.getInjectScript(toInjectObj)).isNotNull();
	}

	@Test
	public void testDataConditionScriptDefaultVar() {
		assertThat(ServerlessWorkflowUtils.conditionScript("$.name", DataCondition.Operator.EQUALS, "john"))
				.isNotNull();
		assertThat(ServerlessWorkflowUtils.conditionScript("$.name", DataCondition.Operator.EQUALS, "john"))
				.isEqualTo("return workflowdata.get(\"name\").textValue().equals(\"john\");");
	}

	@Test
	public void testConditionScriptCustomVar() {
		assertThat(ServerlessWorkflowUtils.conditionScript("person.name", DataCondition.Operator.EQUALS, "john"))
				.isNotNull();
		assertThat(ServerlessWorkflowUtils.conditionScript("person.name", DataCondition.Operator.EQUALS, "john"))
				.isEqualTo("return person.get(\"name\").textValue().equals(\"john\");");
	}

	@Test
	public void testResolveFunctionMetadata() throws Exception {
		Function function = new Function().withName("testfunction1").withMetadata(new HashMap() {
			{
				put("testprop1", "customtestprop1val");
			}
		});

		String testProp1Val = ServerlessWorkflowUtils.resolveFunctionMetadata(function, "testprop1",
				WorkflowAppContext.ofAppResources());
		assertThat(testProp1Val).isNotNull();
		assertThat(testProp1Val).isEqualTo("customtestprop1val");

		String testProp2Val = ServerlessWorkflowUtils.resolveFunctionMetadata(function, "testprop2",
				WorkflowAppContext.ofAppResources());
		assertThat(testProp2Val).isNotNull();
		assertThat(testProp2Val).isEqualTo("testprop2val");
	}

	@Test
	public void testResolveEvenDefinitiontMetadata() throws Exception {
		EventDefinition eventDefinition = new EventDefinition().withName("testevent1").withMetadata(new HashMap() {
			{
				put("testprop1", "customtestprop1val");
			}
		});

		String testProp1Val = ServerlessWorkflowUtils.resolveEvenDefinitiontMetadata(eventDefinition, "testprop1",
				WorkflowAppContext.ofAppResources());
		assertThat(testProp1Val).isNotNull();
		assertThat(testProp1Val).isEqualTo("customtestprop1val");

		String testProp2Val = ServerlessWorkflowUtils.resolveEvenDefinitiontMetadata(eventDefinition, "testprop2",
				WorkflowAppContext.ofAppResources());
		assertThat(testProp2Val).isNotNull();
		assertThat(testProp2Val).isEqualTo("testprop2val");
	}

	@Test
	public void testResolveStatetMetadata() throws Exception {
		DefaultState defaultState = new DefaultState().withName("teststate1").withMetadata(new HashMap() {
			{
				put("testprop1", "customtestprop1val");
			}
		});

		String testProp1Val = ServerlessWorkflowUtils.resolveStatetMetadata(defaultState, "testprop1",
				WorkflowAppContext.ofAppResources());
		assertThat(testProp1Val).isNotNull();
		assertThat(testProp1Val).isEqualTo("customtestprop1val");

		String testProp2Val = ServerlessWorkflowUtils.resolveStatetMetadata(defaultState, "testprop2",
				WorkflowAppContext.ofAppResources());
		assertThat(testProp2Val).isNotNull();
		assertThat(testProp2Val).isEqualTo("testprop2val");

	}

	@Test
	public void testResolveWorkflowMetadata() throws Exception {
		Workflow workflow = new Workflow().withId("workflowid1").withMetadata(new HashMap() {
			{
				put("testprop1", "customtestprop1val");
			}
		});

		String testProp1Val = ServerlessWorkflowUtils.resolveWorkflowMetadata(workflow, "testprop1",
				WorkflowAppContext.ofAppResources());
		assertThat(testProp1Val).isNotNull();
		assertThat(testProp1Val).isEqualTo("customtestprop1val");

		String testProp2Val = ServerlessWorkflowUtils.resolveWorkflowMetadata(workflow, "testprop2",
				WorkflowAppContext.ofAppResources());
		assertThat(testProp2Val).isNotNull();
		assertThat(testProp2Val).isEqualTo("testprop2val");
	}

}
