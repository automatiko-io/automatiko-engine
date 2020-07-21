
package io.automatik.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.Model;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.codegen.AbstractCodegenTest;

public class MessageStartEventTest extends AbstractCodegenTest {

	@Test
	public void testMessageStartEventProcess() throws Exception {

		Application app = generateCodeProcessesOnly("messagestartevent/MessageStartEvent.bpmn2");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("MessageStartEvent");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("customerId", "CUS-00998877");
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start("customers", null);

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(1).containsKeys("customerId");
		assertThat(result.toMap().get("customerId")).isNotNull().isEqualTo("CUS-00998877");
	}

	@Test
	public void testMessageStartAndEndEventProcess() throws Exception {

		Application app = generateCodeProcessesOnly("messagestartevent/MessageStartAndEndEvent.bpmn2");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("MessageStartEvent");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("customerId", "CUS-00998877");
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start("customers", null);

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(1).containsKeys("customerId");
		assertThat(result.toMap().get("customerId")).isNotNull().isEqualTo("CUS-00998877");
	}

	@Test
	public void testRESTApiForMessageStartEvent() throws Exception {

		Application app = generateCodeProcessesOnly("messagestartevent/MessageStartEvent.bpmn2");
		assertThat(app).isNotNull();

		Class<?> resourceClazz = Class.forName("org.kie.kogito.test.MessageStartEventResource", true,
				testClassLoader());
		assertNotNull(resourceClazz);
		Method[] methods = resourceClazz.getMethods();
		for (Method m : methods) {
			if (m.getName().startsWith("createResource")) {
				fail("For processes without none start event there should not be create resource method");
			}
		}
	}

	@Test
	public void testRESTApiForMessageEndEvent() throws Exception {

		Application app = generateCodeProcessesOnly("messagestartevent/MessageEndEvent.bpmn2");
		assertThat(app).isNotNull();

		Class<?> resourceClazz = Class.forName("org.kie.kogito.test.MessageStartEventResource", true,
				testClassLoader());
		assertNotNull(resourceClazz);
		Method[] methods = resourceClazz.getMethods();
		assertThat(methods).haveAtLeast(1, new Condition<Method>(m -> m.getName().startsWith("createResource"),
				"Must have method with name 'createResource'"));
	}

	@Test
	public void testMessageProducerForMessageEndEvent() throws Exception {

		Application app = generateCodeProcessesOnly("messagestartevent/MessageStartAndEndEvent.bpmn2");
		assertThat(app).isNotNull();
		// class name is with suffix that represents node id as there might be multiple
		// end message events
		Class<?> resourceClazz = Class.forName("org.kie.kogito.test.MessageStartEventMessageProducer_3", true,
				testClassLoader());
		assertNotNull(resourceClazz);
		Method[] methods = resourceClazz.getMethods();
		assertThat(methods).haveAtLeast(1,
				new Condition<Method>(m -> m.getName().equals("produce"), "Must have method with name 'produce'"));
	}

	@Test
	public void testNoneAndMessageStartEventProcess() throws Exception {

		Application app = generateCodeProcessesOnly("messagestartevent/NoneAndMessageStartEvent.bpmn2");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("MessageStartEvent");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("customerId", "CUS-00998877");
		m.fromMap(parameters);
		// first start it via none start event
		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(2).containsKeys("customerId", "path");
		assertThat(result.toMap().get("customerId")).isNotNull().isEqualTo("CUS-00998877");
		assertThat(result.toMap().get("path")).isNotNull().isEqualTo("none");

		// next start it via message start event
		processInstance = p.createInstance(m);
		processInstance.start("customers", null);

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
		result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(2).containsKeys("customerId", "path");
		assertThat(result.toMap().get("customerId")).isNotNull().isEqualTo("CUS-00998877");
		assertThat(result.toMap().get("path")).isNotNull().isEqualTo("message");
	}
}
