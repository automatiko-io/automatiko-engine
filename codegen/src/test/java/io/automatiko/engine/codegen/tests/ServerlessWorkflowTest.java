
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.codegen.AbstractCodegenTest;

public class ServerlessWorkflowTest extends AbstractCodegenTest {

	@ParameterizedTest
	@ValueSource(strings = { "serverless/single-inject-state.sw.json", "serverless/single-inject-state.sw.yml" })
	public void testSingleInjectWorkflow(String processLocation) throws Exception {

		Application app = generateCodeProcessesOnly(processLocation);
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("singleinject_1_0");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();

		String jsonParamStr = "{}";

		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonParamObj = mapper.readTree(jsonParamStr);

		parameters.put("workflowdata", jsonParamObj);
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(1).containsKeys("workflowdata");

		assertThat(result.toMap().get("workflowdata")).isInstanceOf(JsonNode.class);

		JsonNode dataOut = (JsonNode) result.toMap().get("workflowdata");

		assertThat(dataOut.get("name").textValue()).isEqualTo("john");
	}

	@ParameterizedTest
	@ValueSource(strings = { "serverless/switch-state.sw.json", "serverless/switch-state.sw.yml" })
	public void testApproveSwitchStateWorkflow(String processLocation) throws Exception {

		Application app = generateCodeProcessesOnly(processLocation);
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("switchworkflow_1_0");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();

		String jsonParamStr = "{}";

		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonParamObj = mapper.readTree(jsonParamStr);

		parameters.put("workflowdata", jsonParamObj);
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(1).containsKeys("workflowdata");

		assertThat(result.toMap().get("workflowdata")).isInstanceOf(JsonNode.class);

		JsonNode dataOut = (JsonNode) result.toMap().get("workflowdata");

		assertThat(dataOut.get("decision").textValue()).isEqualTo("Approved");
	}

	@ParameterizedTest
	@ValueSource(strings = { "serverless/switch-state-deny.sw.json", "serverless/switch-state-deny.sw.yml" })
	public void testDenySwitchStateWorkflow(String processLocation) throws Exception {

		Application app = generateCodeProcessesOnly(processLocation);
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("switchworkflow_1_0");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();

		String jsonParamStr = "{}";

		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonParamObj = mapper.readTree(jsonParamStr);

		parameters.put("workflowdata", jsonParamObj);
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(1).containsKeys("workflowdata");

		assertThat(result.toMap().get("workflowdata")).isInstanceOf(JsonNode.class);

		JsonNode dataOut = (JsonNode) result.toMap().get("workflowdata");

		assertThat(dataOut.get("decision").textValue()).isEqualTo("Denied");
	}

	@Test
	public void testSubFlowWorkflow() throws Exception {

		Application app = generateCodeProcessesOnly("serverless/single-subflow.sw.json",
				"serverless/called-subflow.sw.json");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("singlesubflow_1_0");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();

		String jsonParamStr = "{}";

		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonParamObj = mapper.readTree(jsonParamStr);

		parameters.put("workflowdata", jsonParamObj);
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(1).containsKeys("workflowdata");

		assertThat(result.toMap().get("workflowdata")).isInstanceOf(JsonNode.class);

		JsonNode dataOut = (JsonNode) result.toMap().get("workflowdata");

		assertThat(dataOut.get("parentData").textValue()).isEqualTo("parentTestData");
		assertThat(dataOut.get("childData").textValue()).isEqualTo("childTestData");

	}

	@Test
	public void testParallelExecWorkflow() throws Exception {
		try {
			Application app = generateCodeProcessesOnly("serverless/parallel-state.sw.json",
					"serverless/parallel-state-branch1.sw.json", "serverless/parallel-state-branch2.sw.json");
			assertThat(app).isNotNull();

			Process<? extends Model> p = app.processes().processById("parallelworkflow_1_0");

			Model m = p.createModel();
			Map<String, Object> parameters = new HashMap<>();

			String jsonParamStr = "{}";

			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonParamObj = mapper.readTree(jsonParamStr);

			parameters.put("workflowdata", jsonParamObj);
			m.fromMap(parameters);

			ProcessInstance<?> processInstance = p.createInstance(m);
			processInstance.start();

			assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

			Model result = (Model) processInstance.variables();
			assertThat(result.toMap()).hasSize(1).containsKeys("workflowdata");

			assertThat(result.toMap().get("workflowdata")).isInstanceOf(JsonNode.class);

			JsonNode dataOut = (JsonNode) result.toMap().get("workflowdata");

			assertThat(dataOut.get("branch1data").textValue()).isEqualTo("testBranch1Data");
			assertThat(dataOut.get("branch2data").textValue()).isEqualTo("testBranch2Data");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "serverless/compensation.sw.json", "serverless/compensation.sw.yml"})
	public void testCompensationWorkflow(String processLocation) throws Exception {

		Application app = generateCodeProcessesOnly(processLocation);
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("compensationworkflow");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();

		String jsonParamStr = "{\"x\": \"0\"}";

		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonParamObj = mapper.readTree(jsonParamStr);

		parameters.put("workflowdata", jsonParamObj);
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(1).containsKeys("workflowdata");

		assertThat(result.toMap().get("workflowdata")).isInstanceOf(JsonNode.class);

		JsonNode dataOut = (JsonNode) result.toMap().get("workflowdata");

		System.out.println(dataOut.toPrettyString());

		assertThat(dataOut.get("x").textValue()).isEqualTo("2");
	}

}