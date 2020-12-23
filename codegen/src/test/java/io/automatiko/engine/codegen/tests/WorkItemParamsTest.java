
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.codegen.AbstractCodegenTest;

class WorkItemParamsTest extends AbstractCodegenTest {

	@Test
	void testBasicServiceProcessTask() throws Exception {
		Application app = generateCodeProcessesOnly("servicetask/WorkItemParams.bpmn");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("WorkItemParamsTest_1_0");

		ProcessInstance<?> processInstance = p.createInstance(p.createModel());
		processInstance.start();

		assertThat(processInstance.startDate()).isNotNull();
		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		Map<String, Object> data = result.toMap();
		assertThat(data).hasSize(4).containsKeys("boolValue", "intValue", "floatValue", "stringValue");
		assertThat(data.get("boolValue")).isNotNull().isEqualTo(Boolean.FALSE);
		assertThat(data.get("intValue")).isNotNull().isEqualTo(101);
		assertThat(data.get("floatValue")).isNotNull().isEqualTo(2.1f);
		assertThat(data.get("stringValue")).isNotNull().isEqualTo("foofoo");
	}

}
