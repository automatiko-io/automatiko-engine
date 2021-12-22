
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.codegen.data.Person;

public class ConditionalEventTest extends AbstractCodegenTest {

    @Test
    public void testBasicConditionalEventProcess() throws Exception {

        Application app = generateCodeProcessesOnly("conditionalevent/ConditionalEvent.bpmn2");

        Process<? extends Model> p = app.processes().processById("conditions");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        m.fromMap(Collections.singletonMap("data", "data"));
        processInstance.updateVariables(m);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testBoundaryConditionalEventProcess() throws Exception {

        Application app = generateCodeProcessesOnly("conditionalevent/ConditionalBoundaryEvent.bpmn2");

        Process<? extends Model> p = app.processes().processById("boundaryCondition");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("person", new Person("john", 30));
        m.fromMap(parameters);

        ProcessInstance processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        m.fromMap(Collections.singletonMap("person", new Person("john", 45)));
        processInstance.updateVariables(m);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testEventSubprocessConditionalEventProcess() throws Exception {

        Application app = generateCodeProcessesOnly("conditionalevent/ConditionalEventSubprocess.bpmn2");

        Process<? extends Model> p = app.processes().processById("subprocessCondition");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("person", new Person("john", 30));
        m.fromMap(parameters);

        ProcessInstance processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        m.fromMap(Collections.singletonMap("person", new Person("john", 45)));
        processInstance.updateVariables(m);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);

    }

    @Test
    public void testNonInterruptingEventSubprocessConditionalEventProcess() throws Exception {

        Application app = generateCodeProcessesOnly("conditionalevent/ConditionalEventSubprocessNonInterrupting.bpmn2");

        Process<? extends Model> p = app.processes().processById("subprocessCondition");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("person", new Person("john", 30));
        m.fromMap(parameters);

        ProcessInstance processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        m.fromMap(Collections.singletonMap("person", new Person("john", 45)));
        processInstance.updateVariables(m);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        m.fromMap(Collections.singletonMap("person", new Person("john", 15)));
        processInstance.updateVariables(m);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        m.fromMap(Collections.singletonMap("person", new Person("john", 45)));
        processInstance.updateVariables(m);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        Object counter = ((Model) processInstance.variables()).toMap().get("counter");
        assertThat(counter).isNotNull().asList().hasSize(2);

        processInstance.abort();
    }
}
