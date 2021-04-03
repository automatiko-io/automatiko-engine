
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

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

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).containsKeys("name");

        JsonNode dataOut = (JsonNode) result.toMap().get("name");

        assertThat(dataOut.textValue()).isEqualTo("john");
    }

    @ParameterizedTest
    @ValueSource(strings = { "serverless/switch-state.sw.json", "serverless/switch-state.sw.yml" })
    public void testApproveSwitchStateWorkflow(String processLocation) throws Exception {

        Application app = generateCodeProcessesOnly(processLocation);
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("switchworkflow_1_0");

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).containsKeys("decision");

        JsonNode dataOut = (JsonNode) result.toMap().get("decision");

        assertThat(dataOut.textValue()).isEqualTo("Approved");
    }

    @ParameterizedTest
    @ValueSource(strings = { "serverless/switch-state-deny.sw.json", "serverless/switch-state-deny.sw.yml" })
    public void testDenySwitchStateWorkflow(String processLocation) throws Exception {

        Application app = generateCodeProcessesOnly(processLocation);
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("switchworkflow_1_0");

        Model m = p.createModel();
        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).containsKeys("decision");

        JsonNode dataOut = (JsonNode) result.toMap().get("decision");

        assertThat(dataOut.textValue()).isEqualTo("Denied");
    }

    @Test
    public void testSubFlowWorkflow() throws Exception {

        Application app = generateCodeProcessesOnly("serverless/single-subflow.sw.json",
                "serverless/called-subflow.sw.json");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("singlesubflow_1_0");

        Model m = p.createModel();
        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).containsKeys("parentData", "childData");

        Map<String, Object> dataOut = result.toMap();

        assertThat(((JsonNode) dataOut.get("parentData")).textValue()).isEqualTo("parentTestData");
        assertThat(((JsonNode) dataOut.get("childData")).textValue()).isEqualTo("childTestData");

    }

    @Test
    public void testParallelExecWorkflow() throws Exception {

        Application app = generateCodeProcessesOnly("serverless/parallel-state.sw.json",
                "serverless/parallel-state-branch1.sw.json", "serverless/parallel-state-branch2.sw.json");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("parallelworkflow_1_0");

        Model m = p.createModel();
        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).containsKeys("branch1data", "branch2data");

        Map<String, Object> dataOut = result.toMap();

        assertThat(((JsonNode) dataOut.get("branch1data")).textValue()).isEqualTo("testBranch1Data");
        assertThat(((JsonNode) dataOut.get("branch2data")).textValue()).isEqualTo("testBranch2Data");

    }

    @ParameterizedTest
    @ValueSource(strings = { "serverless/compensation.sw.json", "serverless/compensation.sw.yml" })
    public void testCompensationWorkflow(String processLocation) throws Exception {

        Application app = generateCodeProcessesOnly(processLocation);
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("compensationworkflow");

        Model m = p.createModel();

        String jsonParamStr = "{\"x\": \"0\"}";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonParamObj = mapper.readTree(jsonParamStr);

        m.fromMap(toMap(jsonParamObj));

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).containsKeys("x");

        JsonNode dataOut = (JsonNode) result.toMap().get("x");
        assertThat(dataOut.textValue()).isEqualTo("2");
    }

    @ParameterizedTest
    @ValueSource(strings = { "serverless/operation-no-actions.sw.json", "serverless/operation-no-actions.sw.yml" })
    public void testNoActionsOperationWorkflow(String processLocation) throws Exception {

        Application app = generateCodeProcessesOnly(processLocation);
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("noactions_1_0");

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    protected Map<String, Object> toMap(JsonNode json) {
        Map<String, Object> copy = new LinkedHashMap<>();
        Iterator<Entry<String, JsonNode>> it = json.fields();

        while (it.hasNext()) {
            Entry<String, JsonNode> entry = (Entry<String, JsonNode>) it.next();
            copy.put(entry.getKey(), entry.getValue());
        }

        return copy;
    }

}