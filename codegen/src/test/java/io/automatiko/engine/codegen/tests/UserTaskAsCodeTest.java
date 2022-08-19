
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.services.execution.BaseFunctions;
import io.automatiko.engine.services.identity.StaticIdentityProvider;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

public class UserTaskAsCodeTest extends AbstractCodegenTest {

    private Policy<?> securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));

    @Test
    public void testBasicUserTaskProcess() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class);

        builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .users("john").outputToDataObject("value", "y")
                .then().user("SecondTask").users("john").dataObjectAsInput("x").then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsEntry("y", "test");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 10);

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

}
