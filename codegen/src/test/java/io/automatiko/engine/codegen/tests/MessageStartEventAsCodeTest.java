
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.quarkus.AutomatikoBuildTimeConfig;
import io.automatiko.engine.quarkus.DatabasePersistenceBuildTimeConfig;
import io.automatiko.engine.quarkus.JobsBuildTimeConfig;
import io.automatiko.engine.quarkus.MessagingBuildTimeConfig;
import io.automatiko.engine.quarkus.MetricsBuildTimeConfig;
import io.automatiko.engine.quarkus.PersistenceBuildTimeConfig;
import io.automatiko.engine.quarkus.RestBuildTimeConfig;
import io.automatiko.engine.services.identity.StaticIdentityProvider;
import io.automatiko.engine.workflow.builder.JoinNodeBuilder;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

public class MessageStartEventAsCodeTest extends AbstractCodegenTest {

    private Policy<?> securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));

    @BeforeEach
    public void setup() {

        this.config = new AutomatikoBuildTimeConfig() {
            @Override
            public MessagingBuildTimeConfig messaging() {
                return new MessagingBuildTimeConfig() {
                    @Override
                    public boolean asCloudevents() {
                        return false;
                    }

                    @Override
                    public boolean asCloudeventsBinary() {

                        return false;
                    }
                };
            }

            @Override
            public Optional<String> packageName() {

                return Optional.empty();
            }

            @Override
            public Optional<String> resourcePathPrefix() {

                return Optional.empty();
            }

            @Override
            public Optional<String> resourcePathFormat() {

                return Optional.empty();
            }

            @Override
            public Optional<String> sourceFolder() {

                return Optional.empty();
            }

            @Override
            public Optional<String> projectPaths() {

                return Optional.empty();
            }

            @Override
            public Optional<Boolean> includeAutomatikoApi() {

                return Optional.empty();
            }

            @Override
            public Optional<String> targetDeployment() {

                return Optional.empty();
            }

            @Override
            public MetricsBuildTimeConfig metrics() {

                return null;
            }

            @Override
            public PersistenceBuildTimeConfig persistence() {
                return new PersistenceBuildTimeConfig() {

                    @Override
                    public Optional<String> type() {
                        return Optional.empty();
                    }

                    @Override
                    public DatabasePersistenceBuildTimeConfig database() {
                        return new DatabasePersistenceBuildTimeConfig() {

                            @Override
                            public Optional<Boolean> removeAtCompletion() {
                                return Optional.empty();
                            }
                        };
                    }
                };
            }

            @Override
            public JobsBuildTimeConfig jobs() {

                return null;
            }

            @Override
            public RestBuildTimeConfig rest() {

                return null;
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

    @Test
    public void testMessageStartEvenAndNonetProcess() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("MessageStartEvent_1", "Workflow with message start");
        builder.dataObject("customerId", String.class);

        JoinNodeBuilder join = builder
                .start("none")
                .thenJoin("join");

        builder
                .startOnMessage("customers").type(String.class).toDataObject("customerId")
                .thenJoin("join");

        join.then().log("log message", "Logged customer with id {}", "customerId")
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
