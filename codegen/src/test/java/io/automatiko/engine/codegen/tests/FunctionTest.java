
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

public class FunctionTest extends AbstractCodegenTest {

    public FunctionTest() {
        super(false, true, false);
    }

    @Test
    public void testBasicServiceProcessTaskAsFunction() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("s", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("s");
        assertThat(result.toMap().get("s")).isNotNull().isEqualTo("Hello john!");
    }

    @Test
    public void testServiceProcessDifferentOperationsTask() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessDifferentOperations.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcessDifferentOperations_1_0");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("s", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("s");
        assertThat(result.toMap().get("s")).isNotNull().isEqualTo("Goodbye Hello john!!");
    }
}
