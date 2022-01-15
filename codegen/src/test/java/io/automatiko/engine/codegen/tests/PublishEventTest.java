
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.event.EventPublisher;
import io.automatiko.engine.api.uow.UnitOfWork;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessErrors;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.api.workflow.flexible.ItemDescription.Status;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.services.event.ProcessInstanceDataEvent;
import io.automatiko.engine.services.event.UserTaskInstanceDataEvent;
import io.automatiko.engine.services.event.impl.MilestoneEventBody;
import io.automatiko.engine.services.event.impl.ProcessInstanceEventBody;
import io.automatiko.engine.services.event.impl.UserTaskInstanceEventBody;
import io.automatiko.engine.services.identity.StaticIdentityProvider;

public class PublishEventTest extends AbstractCodegenTest {

    @Test
    public void testProcessWithMilestoneEvents() throws Exception {
        Application app = generateCodeProcessesOnly("cases/milestones/SimpleMilestone.bpmn");

        assertThat(app).isNotNull();
        TestEventPublisher publisher = new TestEventPublisher();
        app.unitOfWorkManager().eventManager().setService("http://myhost");
        app.unitOfWorkManager().eventManager().addPublisher(publisher);

        UnitOfWork uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();

        Process<? extends Model> p = app.processes().processById("TestCase.SimpleMilestone_1_0");

        ProcessInstance<?> processInstance = p.createInstance(p.createModel());
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        uow.end();

        List<DataEvent<?>> events = publisher.extract();
        assertThat(events).isNotNull().hasSize(1);

        DataEvent<?> event = events.get(0);
        assertThat(event).isInstanceOf(ProcessInstanceDataEvent.class);
        ProcessInstanceDataEvent processDataEvent = (ProcessInstanceDataEvent) event;
        assertThat(processDataEvent.getSource()).isEqualTo("http://myhost/SimpleMilestone");

        Set<MilestoneEventBody> milestones = ((ProcessInstanceDataEvent) event).getData().getMilestones();
        assertThat(milestones).hasSize(2).extracting(e -> e.getName(), e -> e.getStatus()).containsExactlyInAnyOrder(
                tuple("AutoStartMilestone", Status.COMPLETED.name()),
                tuple("SimpleMilestone", Status.COMPLETED.name()));
    }

    @Test
    public void testBasicUserTaskProcess() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        TestEventPublisher publisher = new TestEventPublisher();
        app.unitOfWorkManager().eventManager().setService("http://myhost");
        app.unitOfWorkManager().eventManager().addPublisher(publisher);

        UnitOfWork uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();
        uow.end();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        List<DataEvent<?>> events = publisher.extract();
        assertThat(events).isNotNull().hasSize(2);
        ProcessInstanceEventBody body = assertProcessInstanceEvent(events.get(0), "UserTasksProcess",
                "UserTasksProcess", 1);
        assertThat(body.getNodeInstances()).hasSize(2).extractingResultOf("getNodeType").contains("StartNode",
                "HumanTaskNode");
        assertThat(body.getNodeInstances()).extractingResultOf("getTriggerTime").allMatch(v -> v != null);
        assertThat(body.getNodeInstances()).extractingResultOf("getLeaveTime").containsNull();// human task is active
                                                                                              // thus null for leave
                                                                                              // time

        assertUserTaskInstanceEvent(events.get(1), "First Task", null, "1", "Ready", "UserTasksProcess");

        List<WorkItem> workItems = processInstance.workItems(SecurityPolicy.of(new StaticIdentityProvider("john")));
        assertEquals(1, workItems.size());
        assertEquals("FirstTask", workItems.get(0).getName());

        uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();
        processInstance.completeWorkItem(workItems.get(0).getId(), null,
                SecurityPolicy.of(new StaticIdentityProvider("john")));
        uow.end();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        events = publisher.extract();
        assertThat(events).isNotNull().hasSize(3);
        body = assertProcessInstanceEvent(events.get(0), "UserTasksProcess", "UserTasksProcess", 1);
        assertThat(body.getNodeInstances()).hasSize(2).extractingResultOf("getNodeType").contains("HumanTaskNode",
                "HumanTaskNode");
        assertThat(body.getNodeInstances()).extractingResultOf("getTriggerTime").allMatch(v -> v != null);
        assertThat(body.getNodeInstances()).extractingResultOf("getLeaveTime").containsNull();// human task is active
                                                                                              // thus null for leave
                                                                                              // time

        assertUserTaskInstanceEvent(events.get(1), "Second Task", null, "1", "Ready", "UserTasksProcess");
        assertUserTaskInstanceEvent(events.get(2), "First Task", null, "1", "Completed", "UserTasksProcess");

        workItems = processInstance.workItems(SecurityPolicy.of(new StaticIdentityProvider("john")));
        assertEquals(1, workItems.size());
        assertEquals("SecondTask", workItems.get(0).getName());

        uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();
        processInstance.completeWorkItem(workItems.get(0).getId(), null,
                SecurityPolicy.of(new StaticIdentityProvider("john")));
        uow.end();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        events = publisher.extract();
        assertThat(events).isNotNull().hasSize(2);
        body = assertProcessInstanceEvent(events.get(0), "UserTasksProcess", "UserTasksProcess", 2);
        assertThat(body.getNodeInstances()).hasSize(2).extractingResultOf("getNodeType").contains("HumanTaskNode",
                "EndNode");
        assertThat(body.getNodeInstances()).extractingResultOf("getTriggerTime").allMatch(v -> v != null);
        assertThat(body.getNodeInstances()).extractingResultOf("getLeaveTime").allMatch(v -> v != null);

        assertUserTaskInstanceEvent(events.get(1), "Second Task", null, "1", "Completed", "UserTasksProcess");
    }

    @Test
    public void testBasicUserTaskProcessWithSecurityRoles() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcessWithSecurityRoles.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        TestEventPublisher publisher = new TestEventPublisher();
        app.unitOfWorkManager().eventManager().setService("http://myhost");
        app.unitOfWorkManager().eventManager().addPublisher(publisher);

        UnitOfWork uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();
        uow.end();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        List<DataEvent<?>> events = publisher.extract();
        assertThat(events).isNotNull().hasSize(2);
        ProcessInstanceEventBody body = assertProcessInstanceEvent(events.get(0), "UserTasksProcess",
                "UserTasksProcess", 1);
        assertThat(body.getRoles()).hasSize(2).contains("employees", "managers");
        assertThat(body.getNodeInstances()).hasSize(2).extractingResultOf("getNodeType").contains("StartNode",
                "HumanTaskNode");
        assertThat(body.getNodeInstances()).extractingResultOf("getTriggerTime").allMatch(v -> v != null);
        assertThat(body.getNodeInstances()).extractingResultOf("getLeaveTime").containsNull();// human task is active
                                                                                              // thus null for leave
                                                                                              // time

        assertUserTaskInstanceEvent(events.get(1), "First Task", null, "1", "Ready", "UserTasksProcess");
    }

    @Test
    public void testBasicUserTaskProcessWithTags() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcessTags.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "john");
        m.fromMap(parameters);

        TestEventPublisher publisher = new TestEventPublisher();
        app.unitOfWorkManager().eventManager().setService("http://myhost");
        app.unitOfWorkManager().eventManager().addPublisher(publisher);

        UnitOfWork uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();
        uow.end();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        List<DataEvent<?>> events = publisher.extract();
        assertThat(events).isNotNull().hasSize(2);
        ProcessInstanceEventBody body = assertProcessInstanceEvent(events.get(0), "UserTasksProcess",
                "UserTasksProcess", 1);
        assertThat(body.getTags()).hasSize(2).contains("important", "john");
        assertThat(body.getNodeInstances()).hasSize(2).extractingResultOf("getNodeType").contains("StartNode",
                "HumanTaskNode");
        assertThat(body.getNodeInstances()).extractingResultOf("getTriggerTime").allMatch(v -> v != null);
        assertThat(body.getNodeInstances()).extractingResultOf("getLeaveTime").containsNull();// human task is active
                                                                                              // thus null for leave
                                                                                              // time

        assertUserTaskInstanceEvent(events.get(1), "First Task", null, "1", "Ready", "UserTasksProcess");

        List<WorkItem> workItems = processInstance.workItems(SecurityPolicy.of(new StaticIdentityProvider("john")));
        assertEquals(1, workItems.size());
        assertEquals("FirstTask", workItems.get(0).getName());

        uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();
        processInstance.completeWorkItem(workItems.get(0).getId(), null,
                SecurityPolicy.of(new StaticIdentityProvider("john")));
        uow.end();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        events = publisher.extract();
        assertThat(events).isNotNull().hasSize(3);
        body = assertProcessInstanceEvent(events.get(0), "UserTasksProcess", "UserTasksProcess", 1);
        assertThat(body.getNodeInstances()).hasSize(2).extractingResultOf("getNodeType").contains("HumanTaskNode",
                "HumanTaskNode");
        assertThat(body.getNodeInstances()).extractingResultOf("getTriggerTime").allMatch(v -> v != null);
        assertThat(body.getNodeInstances()).extractingResultOf("getLeaveTime").containsNull();// human task is active
                                                                                              // thus null for leave
                                                                                              // time

        assertUserTaskInstanceEvent(events.get(1), "Second Task", null, "1", "Ready", "UserTasksProcess");
        assertUserTaskInstanceEvent(events.get(2), "First Task", null, "1", "Completed", "UserTasksProcess");

        workItems = processInstance.workItems(SecurityPolicy.of(new StaticIdentityProvider("john")));
        assertEquals(1, workItems.size());
        assertEquals("SecondTask", workItems.get(0).getName());

        uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();
        processInstance.completeWorkItem(workItems.get(0).getId(), null,
                SecurityPolicy.of(new StaticIdentityProvider("john")));
        uow.end();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        events = publisher.extract();
        assertThat(events).isNotNull().hasSize(2);
        body = assertProcessInstanceEvent(events.get(0), "UserTasksProcess", "UserTasksProcess", 2);
        assertThat(body.getNodeInstances()).hasSize(2).extractingResultOf("getNodeType").contains("HumanTaskNode",
                "EndNode");
        assertThat(body.getNodeInstances()).extractingResultOf("getTriggerTime").allMatch(v -> v != null);
        assertThat(body.getNodeInstances()).extractingResultOf("getLeaveTime").allMatch(v -> v != null);

        assertUserTaskInstanceEvent(events.get(1), "Second Task", null, "1", "Completed", "UserTasksProcess");
    }

    @Test
    public void testBasicCallActivityTask() throws Exception {

        Application app = generateCodeProcessesOnly("subprocess/CallActivity.bpmn2",
                "subprocess/CallActivitySubProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ParentProcess_1");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", "a");
        parameters.put("y", "b");
        m.fromMap(parameters);

        TestEventPublisher publisher = new TestEventPublisher();
        app.unitOfWorkManager().eventManager().setService("http://myhost");
        app.unitOfWorkManager().eventManager().addPublisher(publisher);

        UnitOfWork uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        uow.end();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");
        assertThat(result.toMap().get("y")).isNotNull().isEqualTo("new value");
        assertThat(result.toMap().get("x")).isNotNull().isEqualTo("a");

        List<DataEvent<?>> events = publisher.extract();
        assertThat(events).isNotNull().hasSize(2);
    }

    @Test
    public void testExclusiveGatewayStartToEnd() throws Exception {

        Application app = generateCodeProcessesOnly("gateway/ExclusiveSplit.bpmn2");
        assertThat(app).isNotNull();
        TestEventPublisher publisher = new TestEventPublisher();
        app.unitOfWorkManager().eventManager().setService("http://myhost");
        app.unitOfWorkManager().eventManager().addPublisher(publisher);

        UnitOfWork uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();

        Process<? extends Model> p = app.processes().processById("ExclusiveSplit");

        Map<String, Object> params = new HashMap<>();
        params.put("x", "First");
        params.put("y", "None");
        Model m = p.createModel();
        m.fromMap(params);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");
        uow.end();

        List<DataEvent<?>> events = publisher.extract();
        assertThat(events).isNotNull().hasSize(1);

        DataEvent<?> event = events.get(0);
        assertThat(event).isInstanceOf(ProcessInstanceDataEvent.class);

        ProcessInstanceEventBody body = assertProcessInstanceEvent(events.get(0), "ExclusiveSplit",
                "Basic process with gateway decision", 2);

        assertThat(body.getNodeInstances()).hasSize(6).extractingResultOf("getNodeType").contains("StartNode",
                "ActionNode", "Split", "Join", "EndNode", "WorkItemNode");

        assertThat(body.getNodeInstances()).extractingResultOf("getTriggerTime").allMatch(v -> v != null);
        assertThat(body.getNodeInstances()).extractingResultOf("getLeaveTime").allMatch(v -> v != null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testServiceTaskProcessWithError() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessDifferentOperations.bpmn2");
        assertThat(app).isNotNull();
        TestEventPublisher publisher = new TestEventPublisher();
        app.unitOfWorkManager().eventManager().setService("http://myhost");
        app.unitOfWorkManager().eventManager().addPublisher(publisher);

        UnitOfWork uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();

        Process<? extends Model> p = app.processes().processById("ServiceProcessDifferentOperations_1_0");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance processInstance = p.createInstance(m);
        processInstance.start();

        uow.end();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ERROR);
        List<DataEvent<?>> events = publisher.extract();
        assertThat(events).isNotNull().hasSize(1);

        ProcessInstanceEventBody body = assertProcessInstanceEvent(events.get(0), "ServiceProcessDifferentOperations",
                "Service Process", 5);
        assertThat(body.getNodeInstances()).hasSize(2).extractingResultOf("getNodeType").contains("StartNode",
                "WorkItemNode");
        assertThat(body.getNodeInstances()).extractingResultOf("getTriggerTime").allMatch(v -> v != null);
        assertThat(body.getNodeInstances()).extractingResultOf("getLeaveTime").containsNull();// human task is active
                                                                                              // thus null for leave
                                                                                              // time

        assertThat(body.getErrors()).hasSize(1);
        assertThat(body.getErrors().get(0).getNodeDefinitionId()).isEqualTo("_38E04E27-3CCA-47F9-927B-E37DC4B8CE25");

        parameters.put("s", "john");
        m.fromMap(parameters);
        uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();
        processInstance.updateVariables(m);
        uow.end();

        events = publisher.extract();
        assertThat(events).isNotNull().hasSize(1);
        body = assertProcessInstanceEvent(events.get(0), "ServiceProcessDifferentOperations", "Service Process", 5);
        assertThat(body.getErrors()).hasSize(1);
        assertThat(body.getErrors().get(0).getNodeDefinitionId()).isEqualTo("_38E04E27-3CCA-47F9-927B-E37DC4B8CE25");

        uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();
        if (processInstance.errors().isPresent()) {
            ((ProcessErrors) processInstance.errors().get()).retrigger();
        }
        uow.end();

        events = publisher.extract();
        assertThat(events).isNotNull().hasSize(1);

        body = assertProcessInstanceEvent(events.get(0), "ServiceProcessDifferentOperations", "Service Process", 2);
        assertThat(body.getErrors()).isEmpty();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("s");
        assertThat(result.toMap().get("s")).isNotNull().isEqualTo("Goodbye Hello john!!");
    }

    @Test
    public void testBasicUserTaskProcessWithSensitiveData() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcessSensitive.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "john");
        parameters.put("ssn", "123-456");
        m.fromMap(parameters);

        TestEventPublisher publisher = new TestEventPublisher();
        app.unitOfWorkManager().eventManager().setService("http://myhost");
        app.unitOfWorkManager().eventManager().addPublisher(publisher);

        UnitOfWork uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();
        uow.end();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        List<DataEvent<?>> events = publisher.extract();
        assertThat(events).isNotNull().hasSize(2);
        ProcessInstanceEventBody body = assertProcessInstanceEvent(events.get(0), "UserTasksProcess",
                "UserTasksProcess", 1);
        assertThat(body.getTags()).hasSize(0);
        assertThat(body.getVariables()).hasSize(1).containsEntry("name", "john");
        assertThat(body.getNodeInstances()).hasSize(2).extractingResultOf("getNodeType").contains("StartNode",
                "HumanTaskNode");
        assertThat(body.getNodeInstances()).extractingResultOf("getTriggerTime").allMatch(v -> v != null);
        assertThat(body.getNodeInstances()).extractingResultOf("getLeaveTime").containsNull();// human task is active
                                                                                              // thus null for leave
                                                                                              // time

        assertUserTaskInstanceEvent(events.get(1), "First Task", null, "1", "Ready", "UserTasksProcess");

        List<WorkItem> workItems = processInstance.workItems(SecurityPolicy.of(new StaticIdentityProvider("john")));
        assertEquals(1, workItems.size());
        assertEquals("FirstTask", workItems.get(0).getName());

        uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();
        processInstance.completeWorkItem(workItems.get(0).getId(), null,
                SecurityPolicy.of(new StaticIdentityProvider("john")));
        uow.end();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        events = publisher.extract();
        assertThat(events).isNotNull().hasSize(3);
        body = assertProcessInstanceEvent(events.get(0), "UserTasksProcess", "UserTasksProcess", 1);
        assertThat(body.getNodeInstances()).hasSize(2).extractingResultOf("getNodeType").contains("HumanTaskNode",
                "HumanTaskNode");
        assertThat(body.getNodeInstances()).extractingResultOf("getTriggerTime").allMatch(v -> v != null);
        assertThat(body.getNodeInstances()).extractingResultOf("getLeaveTime").containsNull();// human task is active
                                                                                              // thus null for leave
                                                                                              // time

        assertUserTaskInstanceEvent(events.get(1), "Second Task", null, "1", "Ready", "UserTasksProcess");
        assertUserTaskInstanceEvent(events.get(2), "First Task", null, "1", "Completed", "UserTasksProcess");

        workItems = processInstance.workItems(SecurityPolicy.of(new StaticIdentityProvider("john")));
        assertEquals(1, workItems.size());
        assertEquals("SecondTask", workItems.get(0).getName());

        uow = app.unitOfWorkManager().newUnitOfWork();
        uow.start();
        processInstance.completeWorkItem(workItems.get(0).getId(), null,
                SecurityPolicy.of(new StaticIdentityProvider("john")));
        uow.end();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        events = publisher.extract();
        assertThat(events).isNotNull().hasSize(2);
        body = assertProcessInstanceEvent(events.get(0), "UserTasksProcess", "UserTasksProcess", 2);
        assertThat(body.getNodeInstances()).hasSize(2).extractingResultOf("getNodeType").contains("HumanTaskNode",
                "EndNode");
        assertThat(body.getNodeInstances()).extractingResultOf("getTriggerTime").allMatch(v -> v != null);
        assertThat(body.getNodeInstances()).extractingResultOf("getLeaveTime").allMatch(v -> v != null);

        assertUserTaskInstanceEvent(events.get(1), "Second Task", null, "1", "Completed", "UserTasksProcess");
    }

    /*
     * Helper methods
     */

    protected ProcessInstanceEventBody assertProcessInstanceEvent(DataEvent<?> event, String processId,
            String processName, Integer state) {

        assertThat(event).isInstanceOf(ProcessInstanceDataEvent.class);
        ProcessInstanceEventBody body = ((ProcessInstanceDataEvent) event).getData();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getStartDate()).isNotNull();
        if (state == ProcessInstance.STATE_ACTIVE || state == ProcessInstance.STATE_ERROR) {
            assertThat(body.getEndDate()).isNull();
        } else {
            assertThat(body.getEndDate()).isNotNull();
        }
        assertThat(body.getParentInstanceId()).isNull();
        assertThat(body.getRootInstanceId()).isNull();
        assertThat(body.getProcessId()).isEqualTo(processId);
        assertThat(body.getProcessName()).isEqualTo(processName);
        assertThat(body.getState()).isEqualTo(state);

        assertThat(event.getSource()).isEqualTo("http://myhost/" + processId);
        assertThat(event.getTime()).doesNotContain("[");

        return body;
    }

    protected UserTaskInstanceEventBody assertUserTaskInstanceEvent(DataEvent<?> event, String taskName,
            String taskDescription, String taskPriority, String taskState, String processId) {
        assertThat(event).isInstanceOf(UserTaskInstanceDataEvent.class);
        UserTaskInstanceEventBody body = ((UserTaskInstanceDataEvent) event).getData();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getTaskName()).isEqualTo(taskName);
        assertThat(body.getTaskDescription()).isEqualTo(taskDescription);
        assertThat(body.getTaskPriority()).isEqualTo(taskPriority);
        assertThat(body.getStartDate()).isNotNull();
        assertThat(body.getState()).isEqualTo(taskState);
        if (taskState.equals("Completed")) {
            assertThat(body.getCompleteDate()).isNotNull();
        } else {
            assertThat(body.getCompleteDate()).isNull();
        }

        assertThat(event.getSource()).isEqualTo("http://myhost/" + processId);
        assertThat(event.getTime()).doesNotContain("[");

        return body;
    }

    protected ProcessInstanceEventBody assertProcessInstanceEventWithParentId(DataEvent<?> event, String processId,
            String processName, Integer state) {

        assertThat(event).isInstanceOf(ProcessInstanceDataEvent.class);
        ProcessInstanceEventBody body = ((ProcessInstanceDataEvent) event).getData();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getStartDate()).isNotNull();
        if (state == ProcessInstance.STATE_ACTIVE) {
            assertThat(body.getEndDate()).isNull();
        } else {
            assertThat(body.getEndDate()).isNotNull();
        }
        assertThat(body.getParentInstanceId()).isNotNull();
        assertThat(body.getRootInstanceId()).isNotNull();
        assertThat(body.getProcessId()).isEqualTo(processId);
        assertThat(body.getProcessName()).isEqualTo(processName);
        assertThat(body.getState()).isEqualTo(state);

        return body;
    }

    private class TestEventPublisher implements EventPublisher {

        private List<DataEvent<?>> events = new ArrayList<>();

        @Override
        public void publish(DataEvent<?> event) {
            this.events.add(event);
        }

        @Override
        public void publish(Collection<DataEvent<?>> events) {
            this.events.addAll(events);
        }

        public List<DataEvent<?>> extract() {
            List<DataEvent<?>> copied = new ArrayList<>(this.events);
            this.events.clear();
            return copied;
        }
    }
}
