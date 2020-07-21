
package io.automatik.engine.codegen.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.Model;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.codegen.AbstractCodegenTest;

import static org.assertj.core.api.Assertions.assertThat;

public class VariableTest extends AbstractCodegenTest {

	@Test
	public void testVariablesWithReservedNameOnServiceTask() throws Exception {
		Application app = generateCodeProcessesOnly("servicetask/ServiceTaskWithReservedNameVariable.bpmn2");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("test");

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
}