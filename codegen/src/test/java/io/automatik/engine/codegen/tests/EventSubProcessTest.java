
package io.automatik.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.Model;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.codegen.AbstractCodegenTest;
import io.automatik.engine.codegen.data.Person;
import io.automatik.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatik.engine.workflow.Sig;
import io.automatik.engine.workflow.compiler.util.NodeLeftCountDownProcessEventListener;

public class EventSubProcessTest extends AbstractCodegenTest {

	@Test
	public void testEventSignalSubProcess() throws Exception {

		Application app = generateCodeProcessesOnly("event-subprocess/EventSubprocessSignal.bpmn2");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("EventSubprocessSignal_1");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

		processInstance.send(Sig.of("MySignal", null));

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
	}

	@Test
	public void testEventSignalSubProcessWithData() throws Exception {

		Application app = generateCodeProcessesOnly("event-subprocess/EventSubprocessSignalWithData.bpmn2");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("EventSubprocessSignal_1");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

		processInstance.send(Sig.of("MySignal", new Person("john", 20)));

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
		Model result = (Model) processInstance.variables();
		assertThat(result.toMap()).hasSize(1).containsKeys("person");

		Person person = (Person) result.toMap().get("person");
		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("john");

	}

	@Test
	public void testEventTimerSubProcess() throws Exception {

		Application app = generateCodeProcessesOnly("event-subprocess/EventSubprocessTimer.bpmn2");
		assertThat(app).isNotNull();

		NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("start-sub", 1);
		((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

		Process<? extends Model> p = app.processes().processById("EventSubprocessTimer_1");

		Model m = p.createModel();
		Map<String, Object> parameters = new HashMap<>();
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = p.createInstance(m);
		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

		boolean completed = listener.waitTillCompleted(30000000);
		assertThat(completed).isTrue();

		assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
	}
}
