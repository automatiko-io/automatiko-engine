
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Disabled;
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
import io.automatiko.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatiko.engine.workflow.compiler.util.NodeLeftCountDownProcessEventListener;

public class ServerlessWorkflowTest extends AbstractCodegenTest {

    @ParameterizedTest
    @ValueSource(strings = { "serverless/single-inject-state.sw.json" })
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
    @ValueSource(strings = { "serverless/simple-increment.sw.json" })
    public void testSimpleIncrementtWorkflow(String processLocation) throws Exception {

        Application app = generateCodeProcessesOnly(processLocation);
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("helloworld_1_0");

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).containsKeys("count");

        JsonNode dataOut = (JsonNode) result.toMap().get("count");

        assertThat(dataOut.intValue()).isEqualTo(11);
    }

    @ParameterizedTest
    @ValueSource(strings = { "serverless/single-inject-state-timeout.sw.json" })
    public void testSingleInjectWithExecTimeoutWorkflow(String processLocation) throws Exception {

        Application app = generateCodeProcessesOnly(processLocation);
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("Execution timeout :: end",
                1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("singleinject_1_0");

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).containsKeys("name");

        JsonNode dataOut = (JsonNode) result.toMap().get("name");

        assertThat(dataOut.textValue()).isEqualTo("john");

        boolean completed = listener.waitTillCompleted(3000000);
        assertThat(completed).isTrue();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
        result = (Model) processInstance.variables();
        assertThat(result.toMap()).containsKeys("name");

        dataOut = (JsonNode) result.toMap().get("name");

        assertThat(dataOut.textValue()).isEqualTo("anothernotset");

    }

    @ParameterizedTest
    @ValueSource(strings = { "serverless/switch-state.sw.json" })
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
    @ValueSource(strings = { "serverless/switch-state-deny.sw.json" })
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

    @Disabled
    @Test
    public void testParallelExecWorkflowNumCompleted() throws Exception {

        Application app = generateCodeProcessesOnly("serverless/parallel-state-num-completed.sw.json",
                "serverless/parallel-state-branch1.sw.json", "serverless/parallel-state-branch2.sw.json");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("parallelworkflow_1_0");

        Model m = p.createModel();
        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).containsKeys("branch2data");

        Map<String, Object> dataOut = result.toMap();

        assertThat(((JsonNode) dataOut.get("branch2data")).textValue()).isEqualTo("testBranch2Data");

    }

    @Disabled
    @ParameterizedTest
    @ValueSource(strings = { "serverless/compensation.sw.json" })
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
    @ValueSource(strings = { "serverless/operation-no-actions.sw.json" })
    public void testNoActionsOperationWorkflow(String processLocation) throws Exception {

        Application app = generateCodeProcessesOnly(processLocation);
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("noactions_1_0");

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @ParameterizedTest
    @ValueSource(strings = { "serverless/logvar.sw.json" })
    public void testLogVarWorkflow(String processLocation) throws Exception {

        Application app = generateCodeProcessesOnly(processLocation);
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("logvar_1_0");

        Model m = p.createModel();
        String jsonParamStr = "{\"name\": \"john\"}";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonParamObj = mapper.readTree(jsonParamStr);

        m.fromMap(toMap(jsonParamObj));

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).containsKeys("name");

        JsonNode dataOut = (JsonNode) result.toMap().get("name");

        assertThat(dataOut.textValue()).isEqualTo("john");
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