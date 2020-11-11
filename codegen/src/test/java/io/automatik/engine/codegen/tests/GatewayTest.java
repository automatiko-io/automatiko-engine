
package io.automatik.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.Model;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.codegen.AbstractCodegenTest;
import io.automatik.engine.workflow.Sig;

public class GatewayTest extends AbstractCodegenTest {

	@Test
	public void testEventBasedGatewayWithData() throws Exception {

		Application app = generateCode(Collections.singletonList("gateway/EventBasedSplit.bpmn2"),
				Collections.singletonList("ruletask/BusinessRuleTask.drl"));
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

}
