
package io.automatik.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.Model;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.WorkItem;
import io.automatik.engine.api.workflow.workitem.WorkItemExecutionError;
import io.automatik.engine.codegen.AbstractCodegenTest;
import io.automatik.engine.codegen.data.Person;
import io.automatik.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatik.engine.workflow.compiler.util.NodeLeftCountDownProcessEventListener;

public class EmbeddedSubProcessTest extends AbstractCodegenTest {

    @Test
    public void testEmbeddedSubProcess() throws Exception {

        Application app = generateCodeProcessesOnly("subprocess/EmbeddedSubProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("SubProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testEmbeddedSubProcessWithUserTaskAndVariableScopes() throws Exception {

        Application app = generateCodeProcessesOnly("subprocess/EmbeddedSubProcessWithUserTask.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("embeddedWithUserTask_1_0");
        Person person = new Person("john", 25);
        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("person", person);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> tasks = processInstance.workItems();
        assertThat(tasks).hasSize(1);

        WorkItem wi = processInstance.workItem(tasks.get(0).getId());
        assertThat(wi).isNotNull();

        assertThat(wi.getParameters()).containsKey("person");
        assertThat(wi.getParameters()).extracting("person").isEqualTo(person);

        processInstance.completeWorkItem(tasks.get(0).getId(),
                new HashMap<>(Collections.singletonMap("person", new Person("mary", 20))));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    @Timeout(unit = TimeUnit.SECONDS, value = 10)
    public void testEmbeddedSubprocessServiceProcessTaskWithRetry() throws Exception {

        Application app = generateCodeProcessesOnly("subprocess/EmbeddedSubprocessWihErrorRetry.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("Error happened", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);
        Process<? extends Model> p = app.processes().processById("subprocessRetry");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        listener.waitTillCompleted();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("name", "ErrorData");
        assertThat(result.toMap().get("name")).isNotNull().isEqualTo("john");
        assertThat(result.toMap().get("ErrorData")).isNotNull().isInstanceOf(WorkItemExecutionError.class);

        assertThat(((WorkItemExecutionError) result.toMap().get("ErrorData")).getErrorCode()).isEqualTo("500");
    }
}
