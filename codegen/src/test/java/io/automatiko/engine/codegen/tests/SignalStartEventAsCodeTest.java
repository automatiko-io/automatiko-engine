
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
import io.automatiko.engine.workflow.Sig;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

public class SignalStartEventAsCodeTest extends AbstractCodegenTest {

    @Test
    public void testStartEventProcessAdditionalPathBySignal() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("MessageStartEvent_1", "Workflow with message start");
        builder.dataObject("customerId", String.class)
                .start("customers")
                .then()
                .user("log message").users("john")
                .then().end("done");

        builder.additionalPathOnSignal("test", true).signal("notify").then()
                .log("Notification", "here is notification from signal")
                .then().end("completed");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("MessageStartEvent_1");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("customerId", "CUS-00998877");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("customerId");
        assertThat(result.toMap().get("customerId")).isNotNull().isEqualTo("CUS-00998877");

        processInstance.send(Sig.of("notify"));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testStartEventProcessAdditionalPathBySignalWithPayload() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("MessageStartEvent_1", "Workflow with message start");
        builder.dataObject("customerId", String.class)
                .start("customers")
                .then()
                .user("log message").users("john")
                .then().end("done");

        builder.additionalPathOnSignal("test", true).signal("notify").toDataObject("customerId").then()
                .log("Notification", "here is notification from signal")
                .then().end("completed");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("MessageStartEvent_1");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("customerId", "CUS-00998877");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("customerId");
        assertThat(result.toMap().get("customerId")).isNotNull().isEqualTo("CUS-00998877");

        processInstance.send(Sig.of("notify", "00000"));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
        assertThat(result.toMap().get("customerId")).isNotNull().isEqualTo("00000");
    }

}
