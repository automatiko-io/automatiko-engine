
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.codegen.LambdaParser;
import io.automatiko.engine.services.identity.StaticIdentityProvider;
import io.automatiko.engine.workflow.builder.BuilderContext;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

@SuppressWarnings("unchecked")
public class TimerStartEventAsCodeTest extends AbstractCodegenTest {

    private Policy<?> securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));

    @BeforeAll
    public static void prepare() {
        LambdaParser.parseLambdas(
                "src/test/java/" + TimerStartEventAsCodeTest.class.getCanonicalName().replace(".", "/") + ".java",
                md -> true);
    }

    @AfterAll
    public static void clear() {

        BuilderContext.clear();
    }

    @Test
    public void testStartEventProcessTimer() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("MessageStartEvent_1", "Workflow with message start");
        builder.dataObject("customerId", String.class)
                .startOnTimer("customers").after(1, TimeUnit.SECONDS)
                .then()
                .user("log message").users("john")
                .then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("MessageStartEvent_1");
        p.activate();

        Thread.sleep(1500);

        Collection<?> instances = (Collection<?>) p.instances().values(0, 10);

        assertThat(instances).hasSize(1);

        ProcessInstance<?> instance = (ProcessInstance<?>) instances.iterator().next();

        List<WorkItem> tasks = instance.workItems(securityPolicy);
        assertThat(tasks).hasSize(1);

        instance.completeWorkItem(tasks.get(0).getId(), null, securityPolicy);

        instances = (Collection<?>) p.instances().values(0, 10);

        assertThat(instances).hasSize(0);
    }

    @Test
    public void testStartEventProcessTimerExpression() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("MessageStartEvent_1", "Workflow with message start");
        builder.dataObject("customerId", String.class)
                .startOnTimer("customers").afterFromExpression(() -> java.time.Duration.ofSeconds(1).toString())
                .then()
                .user("log message").users("john")
                .then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("MessageStartEvent_1");
        p.activate();

        Thread.sleep(1500);

        Collection<?> instances = (Collection<?>) p.instances().values(0, 10);

        assertThat(instances).hasSize(1);

        ProcessInstance<?> instance = (ProcessInstance<?>) instances.iterator().next();

        List<WorkItem> tasks = instance.workItems(securityPolicy);
        assertThat(tasks).hasSize(1);

        instance.completeWorkItem(tasks.get(0).getId(), null, securityPolicy);

        instances = (Collection<?>) p.instances().values(0, 10);

        assertThat(instances).hasSize(0);
    }
}
