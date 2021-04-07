
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatiko.engine.workflow.compiler.util.NodeLeftCountDownProcessEventListener;

public class TimerEventTest extends AbstractCodegenTest {

    @Test
    public void testIntermediateCycleTimerEvent() throws Exception {

        Application app = generateCodeProcessesOnly("timer/IntermediateCatchEventTimerCycleISO.bpmn2");
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

        Application app = generateCodeProcessesOnly("timer/IntermediateCatchEventTimerDurationISO.bpmn2");
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
    public void testIntermediateDateTimerEvent() throws Exception {

        Application app = generateCodeProcessesOnly("timer/IntermediateCatchEventTimerDateISO.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("timer", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("IntermediateCatchEvent");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        OffsetDateTime plusTwoSeconds = OffsetDateTime.now().plusSeconds(2);
        parameters.put("date", plusTwoSeconds.toString());
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBoundaryDurationTimerEventOnTask() throws Exception {

        Application app = generateCodeProcessesOnly("timer/TimerBoundaryEventDurationISOOnTask.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("TimerEvent", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("TimerBoundaryEvent");

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
    public void testBoundaryCycleTimerEventOnTask() throws Exception {

        Application app = generateCodeProcessesOnly("timer/TimerBoundaryEventCycleISOOnTask.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("TimerEvent", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("TimerBoundaryEvent");

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
    public void testBoundaryDateTimerEventOnTask() throws Exception {

        Application app = generateCodeProcessesOnly("timer/TimerBoundaryEventDateISOOnTask.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("TimerEvent", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("TimerBoundaryEvent");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        OffsetDateTime plusTwoSeconds = OffsetDateTime.now().plusSeconds(2);
        parameters.put("date", plusTwoSeconds.toString());
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBoundaryDurationTimerEventOnSubprocess() throws Exception {

        Application app = generateCodeProcessesOnly("timer/TimerBoundaryEventDurationISO.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("TimerEvent", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("TimerBoundaryEvent");

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
    public void testStartTimerEvent() throws Exception {

        Application app = generateCodeProcessesOnly("timer/StartTimerDuration.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("timer fired", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("defaultPackage.TimerProcess");
        // activate to schedule timers
        p.activate();

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        Collection<?> instances = p.instances().values(1, 10);
        if (instances.size() == 0) {
            Thread.sleep(1000);
            instances = p.instances().values(1, 10);
        }
        assertThat(instances).hasSize(1);

        ProcessInstance<?> processInstance = (ProcessInstance<?>) instances.iterator().next();
        assertThat(processInstance).isNotNull();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);

        instances = p.instances().values(1, 10);
        assertThat(instances).hasSize(0);

    }

    @Test
    public void testStartTimerEventTimeCycle() throws Exception {

        Application app = generateCodeProcessesOnly("timer/StartTimerCycle.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("timer fired", 2);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("defaultPackage.TimerProcess");
        // activate to schedule timers
        p.activate();

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        Collection<?> instances = p.instances().values(1, 10);
        assertThat(instances).hasSize(2);

        ProcessInstance<?> processInstance = (ProcessInstance<?>) instances.iterator().next();
        assertThat(processInstance).isNotNull();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        // deactivate to cancel timer, so there should be no more timers fired
        p.deactivate();

        // reset the listener to make sure nothing more is triggered
        listener.reset(1);
        completed = listener.waitTillCompleted(3000);
        assertThat(completed).isFalse();
        // same amount of instances should be active as before deactivation
        instances = p.instances().values(1, 10);
        assertThat(instances).hasSize(2);
        // clean up by aborting all instances
        instances.forEach(i -> ((ProcessInstance<?>) i).abort());
        instances = p.instances().values(1, 10);
        assertThat(instances).hasSize(0);

    }

    @Test
    public void testStartTimerEventTimeCycleCron() throws Exception {

        Application app = generateCodeProcessesOnly("timer/StartTimerCycleCron.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("timer fired", 2);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("defaultPackage.TimerProcess");
        // activate to schedule timers
        p.activate();

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        Collection<?> instances = p.instances().values(1, 10);
        assertThat(instances).hasSize(2);

        ProcessInstance<?> processInstance = (ProcessInstance<?>) instances.iterator().next();
        assertThat(processInstance).isNotNull();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        // deactivate to cancel timer, so there should be no more timers fired
        p.deactivate();

        // reset the listener to make sure nothing more is triggered
        listener.reset(1);
        completed = listener.waitTillCompleted(3000);

        // same amount of instances should be active as before deactivation
        instances = p.instances().values(1, 10);

        // clean up by aborting all instances
        instances.forEach(i -> ((ProcessInstance<?>) i).abort());
        instances = p.instances().values(1, 10);
        assertThat(instances).hasSize(0);

    }

    @Test
    public void testIntermediateCycleTimerCronEvent() throws Exception {

        Application app = generateCodeProcessesOnly("timer/IntermediateCatchEventTimerCycleCron.bpmn2");
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
    public void testBoundaryCycleCronTimerEventOnTask() throws Exception {

        Application app = generateCodeProcessesOnly("timer/TimerBoundaryEventCycleCronOnTask.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("TimerEvent", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("TimerBoundaryEvent");

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
