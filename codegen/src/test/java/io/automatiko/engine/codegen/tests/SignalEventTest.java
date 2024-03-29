
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.uow.UnitOfWork;
import io.automatiko.engine.api.workflow.EventDescription;
import io.automatiko.engine.api.workflow.GroupedNamedDataType;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.workflow.Sig;

public class SignalEventTest extends AbstractCodegenTest {

    @Test
    public void testIntermediateSignalEventWithData() throws Exception {

        Application app = generateCode(Collections.singletonList("signalevent/IntermediateCatchEventSignal.bpmn2"),
                Collections.singletonList("ruletask/BusinessRuleTask.drl"));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("IntermediateCatchEvent");

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        Set<EventDescription<?>> eventDescriptions = processInstance.events();
        assertThat(eventDescriptions).hasSize(1).extracting("event").contains("workItemCompleted");
        assertThat(eventDescriptions).extracting("eventType").contains("workItem");
        assertThat(eventDescriptions).extracting("processInstanceId").contains(processInstance.id());

        List<WorkItem> workItems = processInstance.workItems();
        assertThat(workItems).hasSize(1);

        processInstance.completeWorkItem(workItems.get(0).getId(), null);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        eventDescriptions = processInstance.events();
        assertThat(eventDescriptions).hasSize(1).extracting("event").contains("MyMessage");
        assertThat(eventDescriptions).extracting("eventType").contains("signal");
        assertThat(eventDescriptions).extracting("processInstanceId").contains(processInstance.id());

        processInstance.send(Sig.of("MyMessage", "test"));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKey("x");
        assertThat(result.toMap().get("x")).isEqualTo("test");

        assertThat(p.instances().values(1, 10)).hasSize(0);
    }

    @Test
    public void testBoundarySignalEventWithData() throws Exception {

        Application app = generateCode(Collections.singletonList("signalevent/BoundarySignalEventOnTask.bpmn2"),
                Collections.singletonList("ruletask/BusinessRuleTask.drl"));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("BoundarySignalOnTask");

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        Set<EventDescription<?>> eventDescriptions = processInstance.events();
        assertThat(eventDescriptions).hasSize(2).extracting("event").contains("MySignal", "workItemCompleted");
        assertThat(eventDescriptions).extracting("eventType").contains("signal", "workItem");
        assertThat(eventDescriptions).extracting("dataType").hasAtLeastOneElementOfType(GroupedNamedDataType.class);
        assertThat(eventDescriptions).extracting("processInstanceId").contains(processInstance.id());

        processInstance.send(Sig.of("MySignal", "test"));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKey("x");
        assertThat(result.toMap().get("x")).isEqualTo("test");

        assertThat(p.instances().values(1, 10)).hasSize(0);
    }

    @Test
    public void testBoundaryInterruptingSignalEventWithData() throws Exception {

        Application app = generateCode(
                Collections.singletonList("signalevent/BoundaryInterruptingSignalEventOnTask.bpmn2"),
                Collections.singletonList("ruletask/BusinessRuleTask.drl"));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("BoundarySignalOnTask");

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        processInstance.send(Sig.of("MySignal", "test"));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKey("x");
        assertThat(result.toMap().get("x")).isEqualTo("test");

        assertThat(p.instances().values(1, 10)).hasSize(0);
    }

    @Test
    public void testIntermediateSignalEventWithDataControlledByUnitOfWork() throws Exception {

        Application app = generateCode(Collections.singletonList("signalevent/IntermediateCatchEventSignal.bpmn2"),
                Collections.singletonList("ruletask/BusinessRuleTask.drl"));
        assertThat(app).isNotNull();
        // create first unit of work
        UnitOfWork uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();

        Process<? extends Model> p = app.processes().processById("IntermediateCatchEvent");

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        // since unit of work is not ended yet there are no instance added
        assertThat(p.instances().values(1, 10)).hasSize(0);
        uow.end();
        // after the unit of work is ended process instance shows up in the list
        assertThat(p.instances().values(1, 10)).hasSize(1);

        uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();
        List<WorkItem> workItems = processInstance.workItems();
        assertThat(workItems).hasSize(1);

        processInstance.completeWorkItem(workItems.get(0).getId(), null);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        uow.end();

        uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();
        assertThat(p.instances().values(1, 10)).hasSize(1);
        processInstance.send(Sig.of("MyMessage", "test"));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKey("x");
        assertThat(result.toMap().get("x")).isEqualTo("test");

        // since the unit of work is not ended yet there is still instance visible
        assertThat(p.instances().values(ProcessInstanceReadMode.READ_ONLY, ProcessInstance.STATE_COMPLETED, 1, 10)).hasSize(1);
        uow.end();
        // after unit of work is ended instance is gone from the list
        assertThat(p.instances().values(1, 10)).hasSize(0);
    }
}
