
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.codegen.AbstractCodegenTest;

public class CompensationEventTest extends AbstractCodegenTest {

    @Test
    public void testIntermediateCompensationEventProcess() throws Exception {

        Application app = generateCodeProcessesOnly("compensation/UserTaskBeforeAssociatedActivity.bpmn2");

        Process<? extends Model> p = app.processes().processById("CompensateIntermediateThrowEvent");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", "0");
        m.fromMap(parameters);

        ProcessInstance<? extends Model> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems();
        assertEquals(1, workItems.size());
        assertEquals("task", workItems.get(0).getName());

        processInstance.completeWorkItem(workItems.get(0).getId(), null);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(processInstance.variables().toMap()).containsEntry("x", "1");

    }

    @Test
    public void testEndEventCompensationEventProcess() throws Exception {

        Application app = generateCodeProcessesOnly("compensation/UserTaskBeforeAssociatedActivityEnd.bpmn2");

        Process<? extends Model> p = app.processes().processById("CompensateEndEvent");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", "0");
        m.fromMap(parameters);

        ProcessInstance<? extends Model> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems();
        assertEquals(1, workItems.size());
        assertEquals("task", workItems.get(0).getName());

        processInstance.completeWorkItem(workItems.get(0).getId(), null);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(processInstance.variables().toMap()).containsEntry("x", "1");

    }
}
