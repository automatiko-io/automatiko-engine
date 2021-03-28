
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.codegen.data.Person;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatiko.engine.workflow.Sig;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.compiler.util.NodeLeftCountDownProcessEventListener;

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

        boolean completed = listener.waitTillCompleted(3000);
        assertThat(completed).isTrue();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testEventErrorSubProcess() throws Exception {

        Application app = generateCodeProcessesOnly("event-subprocess/EventSubprocessError.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("end-sub", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("EventSubprocessError_1");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        listener.waitTillCompleted(5000);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testEventErrorSubProcessDefaultHander() throws Exception {

        Application app = generateCodeProcessesOnly("event-subprocess/EventSubprocessErrorDefault.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("end-sub", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("EventSubprocessError_1");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        listener.waitTillCompleted(5000);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEventSignalSubProcessWithVersionedData() throws Exception {

        Application app = generateCodeProcessesOnly("event-subprocess/EventSubprocessSignalWithVersionedData.bpmn2");
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
        assertThat(person.getAge()).isEqualTo(20);

        processInstance.send(Sig.of("MySignal", new Person("john", 21)));

        result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("person");

        person = (Person) result.toMap().get("person");
        assertThat(person).isNotNull();
        assertThat(person.getName()).isEqualTo("john");
        assertThat(person.getAge()).isEqualTo(21);

        processInstance.send(Sig.of("MySignal", new Person("john", 25)));

        result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("person");

        person = (Person) result.toMap().get("person");
        assertThat(person).isNotNull();
        assertThat(person.getName()).isEqualTo("john");
        assertThat(person.getAge()).isEqualTo(25);

        Map<String, List<Object>> versions = (Map<String, List<Object>>) ((AbstractProcessInstance<?>) processInstance)
                .processInstance().getVariable(VariableScope.VERSIONED_VARIABLES);

        List<Object> personVersions = (List<Object>) versions.get("person");
        assertThat(personVersions).hasSize(2);

        processInstance.send(Sig.of("MySignal", new Person("john", 25)));

        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }
}
