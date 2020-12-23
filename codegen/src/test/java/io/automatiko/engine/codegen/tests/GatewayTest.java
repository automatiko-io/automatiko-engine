
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.workflow.Sig;

public class GatewayTest extends AbstractCodegenTest {

    @Test
    public void testEventBasedGatewayWithData() throws Exception {

        Application app = generateCodeProcessesOnly("gateway/EventBasedSplit.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("EventBasedSplit");

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        processInstance.send(Sig.of("First", "test"));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKey("x");
        assertThat(result.toMap().get("x")).isEqualTo("test");

        assertThat(p.instances().values(1, 10)).hasSize(0);

        // not test the other branch
        processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        processInstance.send(Sig.of("Second", "value"));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKey("x");
        assertThat(result.toMap().get("x")).isEqualTo("value");

        assertThat(p.instances().values(1, 10)).hasSize(0);
    }

    @Test
    public void testExclusiveGatewayStartToEnd() throws Exception {

        Application app = generateCodeProcessesOnly("gateway/ExclusiveSplit.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ExclusiveSplit");

        Map<String, Object> params = new HashMap<>();
        params.put("x", "First");
        params.put("y", "None");
        Model m = p.createModel();
        m.fromMap(params);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");
    }

    @Test
    public void testInclusiveGatewayStartToEnd() throws Exception {

        Application app = generateCodeProcessesOnly("gateway/InclusiveSplit.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("InclusiveSplit");

        Map<String, Object> params = new HashMap<>();
        params.put("x", "Second");
        params.put("y", "None");
        Model m = p.createModel();
        m.fromMap(params);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");
    }
}
