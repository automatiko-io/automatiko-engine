
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.config.AutomatikoBuildConfig;
import io.automatiko.engine.api.config.MessagingBuildConfig;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.services.identity.StaticIdentityProvider;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

public class MessageStartEventAsCodeTest extends AbstractCodegenTest {

    private Policy<?> securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));

    @BeforeEach
    public void setup() {

        this.config = new AutomatikoBuildConfig() {
            @Override
            public MessagingBuildConfig messaging() {
                return new MessagingBuildConfig() {
                    @Override
                    public boolean asCloudevents() {
                        return false;
                    }
                };
            }
        };
    }

    @Test
    public void testMessageStartEventProcess() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("MessageStartEvent_1", "Workflow with message start");
        builder.dataObject("customerId", String.class)
                .startOnMessage("customers").type(String.class).toDataObject("customerId")
                .then()
                .log("log message", "Logged customer with id {}", "customerId")
                .then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("MessageStartEvent_1");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start("customers", null, "CUS-00998877");

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("customerId");
        assertThat(result.toMap().get("customerId")).isNotNull().isEqualTo("CUS-00998877");
    }

}
