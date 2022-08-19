
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;
import io.automatiko.engine.workflow.compiler.util.NodeLeftCountDownProcessEventListener;

public class TimerEventAsCodeTest extends AbstractCodegenTest {

    @Test
    public void testIntermediateCycleTimerEvent() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("IntermediateCatchEvent", "test workflow with timer cycle")
                .dataObject("x", String.class)
                .dataObject("y", String.class);

        builder.start("start here").then()
                .log("execute script", "Hello world").then().timer("timer").every(1, TimeUnit.SECONDS)

                .then().log("after timer", "fired").then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("timer", 3);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("IntermediateCatchEvent");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        processInstance.abort();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testIntermediateDurationTimerEvent() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("IntermediateCatchEvent", "test workflow with timer duration")
                .dataObject("x", String.class)
                .dataObject("y", String.class);

        builder.start("start here").then()
                .log("execute script", "Hello world").then().timer("timer").after(1, TimeUnit.SECONDS)

                .then().log("after timer", "fired").then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("timer", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("IntermediateCatchEvent");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testIntermediateCycleTimerEventISO() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("IntermediateCatchEvent", "test workflow with timer cycle")
                .dataObject("x", String.class)
                .dataObject("y", String.class);

        builder.start("start here").then()
                .log("execute script", "Hello world").then().timer("timer").every("R/PT1S")

                .then().log("after timer", "fired").then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("timer", 3);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("IntermediateCatchEvent");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        processInstance.abort();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testIntermediateDurationTimerEventISO() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("IntermediateCatchEvent", "test workflow with timer duration")
                .dataObject("x", String.class)
                .dataObject("y", String.class);

        builder.start("start here").then()
                .log("execute script", "Hello world").then().timer("timer").after("PT1S")

                .then().log("after timer", "fired").then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("timer", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("IntermediateCatchEvent");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

}
