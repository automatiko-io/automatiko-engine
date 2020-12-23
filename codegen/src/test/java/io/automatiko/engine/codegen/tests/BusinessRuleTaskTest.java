
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.codegen.AbstractCodegenTest;

public class BusinessRuleTaskTest extends AbstractCodegenTest {

	@Test
	public void testDecision() throws Exception {
		Application app = generateCode(Collections.singletonList("decision/models/dmnprocess.bpmn2"),
				Collections.emptyList(),
				Collections.singletonList("decision/models/vacationDaysAlt/vacationDaysAlt.dmn"),
				Collections.emptyList(), false);

		Process<? extends Model> p = app.processes().processById("DmnProcess_1_0");

		// first run 16, 1 and expected days is 27
		{
			Model m = p.createModel();
			HashMap<String, Object> vars = new HashMap<>();
			vars.put("age", 16);
			vars.put("yearsOfService", 1);
			m.fromMap(vars);

			ProcessInstance<? extends Model> processInstance = p.createInstance(m);
			processInstance.start();

			assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
			Model result = processInstance.variables();

			assertThat(result.toMap().get("vacationDays")).isNotNull().isEqualTo(BigDecimal.valueOf(27));
		}

		// second run 44, 20 and expected days is 24
		{
			Model m = p.createModel();
			HashMap<String, Object> vars = new HashMap<>();
			vars.put("age", 44);
			vars.put("yearsOfService", 20);
			m.fromMap(vars);

			ProcessInstance<? extends Model> processInstance = p.createInstance(m);
			processInstance.start();

			assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
			Model result = processInstance.variables();

			assertThat(result.toMap().get("vacationDays")).isNotNull().isEqualTo(BigDecimal.valueOf(24));
		}

		// second run 50, 30 and expected days is 30
		{
			Model m = p.createModel();
			HashMap<String, Object> vars = new HashMap<>();
			vars.put("age", 50);
			vars.put("yearsOfService", 30);
			m.fromMap(vars);

			ProcessInstance<? extends Model> processInstance = p.createInstance(m);
			processInstance.start();

			assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
			Model result = processInstance.variables();

			assertThat(result.toMap().get("vacationDays")).isNotNull().isEqualTo(BigDecimal.valueOf(30));
		}
	}
}
