
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

public class VariableTest extends AbstractCodegenTest {

    @Test
    public void testVariablesWithReservedNameOnServiceTask() throws Exception {
        Application app = generateCodeProcessesOnly("servicetask/ServiceTaskWithReservedNameVariable.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("test_1_0");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("package", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("result");
        assertThat(result.toMap()).hasSize(2).containsKeys("package");
        assertThat(result.toMap().get("result")).isNotNull().isEqualTo("Hello Hello john!!");
    }

    @Test
    public void testVariablesDefaultValues() throws Exception {
        Application app = generateCodeProcessesOnly("default var values.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("varvalues");

        Model m = p.createModel();
        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(6).containsEntry("var1", false);
        assertThat(result.toMap()).hasSize(6).containsEntry("var2", Double.valueOf(0));
        assertThat(result.toMap()).hasSize(6).containsEntry("var3", Float.valueOf(0));
        assertThat(result.toMap()).hasSize(6).containsEntry("var4", Integer.valueOf(0));
        assertThat(result.toMap()).hasSize(6).containsEntry("var5", Long.valueOf(0));
        assertThat(result.toMap()).hasSize(6).containsEntry("var6", "");
    }
}