
package io.automatik.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.Model;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.codegen.AbstractCodegenTest;
import io.automatik.engine.codegen.data.Person;
import io.automatik.engine.codegen.data.PersonWithList;
import io.automatik.engine.codegen.process.ProcessCodegenException;
import io.automatik.engine.workflow.Sig;

public class MessageIntermediateEventTest extends AbstractCodegenTest {

	@Test
	public void testMessageThrowEventProcess() throws Exception {

		Application app = generateCodeProcessesOnly("messageevent/IntermediateThrowEventMessage.bpmn2");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("MessageIntermediateEvent");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(1).containsKeys("customerId");
	}

	@Test
	public void testMessageCatchEventProcess() throws Exception {

		Application app = generateCodeProcessesOnly("messageevent/IntermediateCatchEventMessage.bpmn2");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("IntermediateCatchEvent");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

		processInstance.send(Sig.of("Message-customers", "CUS-00998877"));

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(1).containsKeys("customerId");
		assertThat(result.toMap().get("customerId")).isNotNull().isEqualTo("CUS-00998877");
	}

	@Test
	public void testMessageBoundaryCatchEventProcess() throws Exception {

		Application app = generateCodeProcessesOnly("messageevent/BoundaryMessageEventOnTask.bpmn2");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("BoundaryMessageOnTask");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

		processInstance.send(Sig.of("Message-customers", "CUS-00998877"));

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(1).containsKeys("customerId");
		assertThat(result.toMap().get("customerId")).isNotNull().isEqualTo("CUS-00998877");
	}

	@Test
	public void malformedShouldThrowException() {
		assertThrows(ProcessCodegenException.class, () -> {
			generateCodeProcessesOnly("messageevent/EventNodeMalformed.bpmn2");
		});
	}

	@Test
	public void testMessageCatchEventIOExpressionProcess() throws Exception {

		Application app = generateCodeProcessesOnly("messageevent/IntermediateCatchEventMessageIOExpression.bpmn2");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("IntermediateCatchEvent");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("person", new Person());
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

		processInstance.send(Sig.of("Message-customers", "CUS-00998877"));

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(2).containsKeys("customerId", "person");
		assertThat(result.toMap().get("person")).isNotNull().extracting("name").isEqualTo("CUS-00998877");
	}

	@Test
	public void testMessageBoundaryCatchEventIOExpressionProcess() throws Exception {

		Application app = generateCodeProcessesOnly("messageevent/BoundaryMessageEventOnTaskIOExpression.bpmn2");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("BoundaryMessageOnTask");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("person", new Person());
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

		processInstance.send(Sig.of("Message-customers", "CUS-00998877"));

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(2).containsKeys("customerId", "person");
		assertThat(result.toMap().get("person")).isNotNull().extracting("name").isEqualTo("CUS-00998877");
	}

	@Test
	public void testMessageCatchEventIOExpressionListProcess() throws Exception {

		Application app = generateCodeProcessesOnly("messageevent/IntermediateCatchEventMessageIOExpressionList.bpmn2");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("IntermediateCatchEvent");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("person", new PersonWithList(null, 0, false, new ArrayList<String>(), null, null, null));
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

		processInstance.send(Sig.of("Message-customers", "CUS-00998877"));

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(2).containsKeys("customerId", "person");
		assertThat(result.toMap().get("person")).isNotNull().extracting("stringList", InstanceOfAssertFactories.LIST)
				.hasSize(1).contains("CUS-00998877");
	}
}
