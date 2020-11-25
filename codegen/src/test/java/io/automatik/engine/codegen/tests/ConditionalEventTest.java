
package io.automatik.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.Model;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.codegen.AbstractCodegenTest;
import io.automatik.engine.codegen.data.Person;

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
}
