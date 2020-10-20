
package io.automatik.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.Path;

import org.junit.jupiter.api.Test;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.Model;
import io.automatik.engine.api.auth.IdentityProvider;
import io.automatik.engine.api.auth.SecurityPolicy;
import io.automatik.engine.api.event.process.DefaultProcessEventListener;
import io.automatik.engine.api.event.process.ProcessWorkItemTransitionEvent;
import io.automatik.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatik.engine.api.workflow.Tag;
import io.automatik.engine.api.workflow.VariableViolationException;
import io.automatik.engine.api.workflow.WorkItem;
import io.automatik.engine.api.workflow.workitem.InvalidTransitionException;
import io.automatik.engine.api.workflow.workitem.NotAuthorizedException;
import io.automatik.engine.api.workflow.workitem.Policy;
import io.automatik.engine.codegen.AbstractCodegenTest;
import io.automatik.engine.codegen.data.Person;
import io.automatik.engine.services.identity.StaticIdentityProvider;
import io.automatik.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatik.engine.workflow.base.instance.impl.humantask.HumanTaskTransition;
import io.automatik.engine.workflow.base.instance.impl.humantask.phases.Claim;
import io.automatik.engine.workflow.base.instance.impl.humantask.phases.Release;
import io.automatik.engine.workflow.base.instance.impl.workitem.Active;
import io.automatik.engine.workflow.base.instance.impl.workitem.Complete;

public class UserTaskTest extends AbstractCodegenTest {

    private Policy<?> securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));

    @Test
    public void testBasicUserTaskProcess() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();
        final List<String> workItemTransitionEvents = new ArrayList<>();
        app.config().process().processEventListeners().listeners().add(new DefaultProcessEventListener() {

            @Override
            public void beforeWorkItemTransition(ProcessWorkItemTransitionEvent event) {
                workItemTransitionEvents.add("BEFORE:: " + event);
            }

            @Override
            public void afterWorkItemTransition(ProcessWorkItemTransitionEvent event) {
                workItemTransitionEvents.add("AFTER:: " + event);
            }
        });

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("FirstTask", workItems.get(0).getName());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("SecondTask", workItems.get(0).getName());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        assertThat(workItemTransitionEvents).hasSize(8);
    }

    @Test
    public void testBasicUserTaskProcessPhases() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        WorkItem wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("SecondTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBasicUserTaskProcessClaimAndCompletePhases() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        WorkItem wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        assertEquals(0, wi.getResults().size());

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Claim.ID, Collections.singletonMap("test", "value"), securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Claim.ID, wi.getPhase());
        assertEquals(Claim.STATUS, wi.getPhaseStatus());
        assertEquals(2, wi.getResults().size());
        assertEquals("value", wi.getResults().get("test"));
        assertEquals("john", wi.getResults().get("ActorId"));

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("SecondTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        assertEquals(0, wi.getResults().size());

        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testBasicUserTaskProcessReleaseAndCompletePhases() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        WorkItem wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        assertEquals(0, wi.getResults().size());

        final String wiId = wi.getId();

        assertThrows(InvalidTransitionException.class, () -> processInstance.transitionWorkItem(wiId,
                new HumanTaskTransition(Release.ID, null, securityPolicy)));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        assertEquals(0, wi.getResults().size());

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("SecondTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        assertEquals(0, wi.getResults().size());

        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testBasicUserTaskProcessClaimAndCompletePhasesWithIdentity() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();
        final List<String> workItemTransitionEvents = new ArrayList<>();
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).listeners()
                .add(new DefaultProcessEventListener() {

                    @Override
                    public void beforeWorkItemTransition(ProcessWorkItemTransitionEvent event) {
                        workItemTransitionEvents.add("BEFORE:: " + event);
                    }

                    @Override
                    public void afterWorkItemTransition(ProcessWorkItemTransitionEvent event) {
                        workItemTransitionEvents.add("AFTER:: " + event);
                    }
                });

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        WorkItem wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        assertEquals(0, wi.getResults().size());

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Claim.ID, Collections.singletonMap("test", "value"), securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Claim.ID, wi.getPhase());
        assertEquals(Claim.STATUS, wi.getPhaseStatus());
        assertEquals(2, wi.getResults().size());
        assertEquals("value", wi.getResults().get("test"));
        assertEquals("john", wi.getResults().get("ActorId"));

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("SecondTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        assertEquals(0, wi.getResults().size());

        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);

        assertThat(workItemTransitionEvents).hasSize(10);
    }

    @Test
    public void testBasicUserTaskProcessClaimAndCompleteWrongUser() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        WorkItem wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        assertEquals(0, wi.getResults().size());

        final String wiId = wi.getId();
        IdentityProvider identity = new StaticIdentityProvider("kelly");

        // if user that is not authorized to work on work item both listing and getting
        // by id should apply it
        List<WorkItem> securedWorkItems = processInstance.workItems(SecurityPolicy.of(identity));
        assertEquals(0, securedWorkItems.size());
        assertThrows(WorkItemNotFoundException.class,
                () -> processInstance.workItem(wiId, SecurityPolicy.of(identity)));

        assertThrows(NotAuthorizedException.class,
                () -> processInstance.transitionWorkItem(wiId, new HumanTaskTransition(Claim.ID, null, identity)));

        assertThrows(NotAuthorizedException.class,
                () -> processInstance.completeWorkItem(wiId, null, SecurityPolicy.of(identity)));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        assertEquals(0, wi.getResults().size());

        IdentityProvider identityCorrect = new StaticIdentityProvider("john");

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, identityCorrect));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("SecondTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        assertEquals(0, wi.getResults().size());

        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testApprovalWithExcludedOwnerViaPhases() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/approval.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("approvals");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();
        assertEquals(io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ACTIVE, processInstance.status());

        StaticIdentityProvider identity = new StaticIdentityProvider("admin", Collections.singletonList("managers"));
        SecurityPolicy policy = SecurityPolicy.of(identity);

        processInstance.workItems(policy);

        List<WorkItem> workItems = processInstance.workItems(policy);
        assertEquals(1, workItems.size());
        HumanTaskTransition transition = new HumanTaskTransition(Complete.ID, null, identity);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);
        // actual owner of the first task is excluded owner on the second task so won't
        // find it
        workItems = processInstance.workItems(policy);
        assertEquals(0, workItems.size());

        identity = new StaticIdentityProvider("john", Collections.singletonList("managers"));
        policy = SecurityPolicy.of(identity);

        workItems = processInstance.workItems(policy);
        assertEquals(1, workItems.size());

        transition = new HumanTaskTransition(Complete.ID, null, identity);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        assertEquals(io.automatik.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED, processInstance.status());
    }

    @Test
    public void testApprovalWithExcludedOwner() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/approval.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("approvals");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();
        assertEquals(io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ACTIVE, processInstance.status());

        StaticIdentityProvider identity = new StaticIdentityProvider("admin", Collections.singletonList("managers"));
        SecurityPolicy policy = SecurityPolicy.of(identity);

        processInstance.workItems(policy);

        List<WorkItem> workItems = processInstance.workItems(policy);
        assertEquals(1, workItems.size());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, policy);
        // actual owner of the first task is excluded owner on the second task so won't
        // find it
        workItems = processInstance.workItems(policy);
        assertEquals(0, workItems.size());

        identity = new StaticIdentityProvider("john", Collections.singletonList("managers"));
        policy = SecurityPolicy.of(identity);

        workItems = processInstance.workItems(policy);
        assertEquals(1, workItems.size());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, policy);

        assertEquals(io.automatik.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED, processInstance.status());
    }

    @Test
    public void testRESTApiForUserTasks() throws Exception {
        class Dummy {

            @Path("/{id}/FirstTask/{workItemId}")
            void post1() {

            }

            @Path("/{id}/SecondTask/{workItemId}")
            void post2() {

            }
        }

        Annotation firstTask = Dummy.class.getDeclaredMethod("post1").getAnnotation(Path.class);
        Annotation secondTask = Dummy.class.getDeclaredMethod("post2").getAnnotation(Path.class);
        testRESTApiForUserTasks("Path", firstTask, secondTask);
    }

    public final void testRESTApiForUserTasks(String postPathAnnotation, Annotation firstTask, Annotation secondTask)
            throws Exception {
        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Class<?> resourceClazz = Class.forName("org.kie.kogito.test.UserTasksProcessResource", true, testClassLoader());
        assertNotNull(resourceClazz);
        List<Annotation> completeTaskPaths = new ArrayList<>();
        Method[] methods = resourceClazz.getMethods();
        Stream.of(methods).filter(m -> m.getName().startsWith("completeTask")).map(Method::getAnnotations)
                .flatMap(Stream::of).filter(a -> a.annotationType().getSimpleName().equals(postPathAnnotation))
                .forEach(completeTaskPaths::add);

        // there must be two distinct paths for user tasks
        assertThat(completeTaskPaths).hasSize(2).containsOnly(firstTask, secondTask);
    }

    @Test
    public void testBasicUserTaskProcessCancelAndTriggerNode() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        WorkItem wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("SecondTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());

        String firstSecondTaskNodeInstanceId = wi.getNodeInstanceId();

        processInstance.cancelNodeInstance(wi.getNodeInstanceId());
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        processInstance.triggerNode("UserTask_2");
        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("SecondTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        // since it was triggered again it must have different node instance id
        assertNotEquals(firstSecondTaskNodeInstanceId, wi.getNodeInstanceId());
        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBasicUserTaskProcessCancelAndRetriggerNode() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        WorkItem wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("SecondTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());

        String firstSecondTaskNodeInstanceId = wi.getNodeInstanceId();

        processInstance.retriggerNodeInstance(wi.getNodeInstanceId());
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("SecondTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        // since it was retriggered it must have different node instance id
        assertNotEquals(firstSecondTaskNodeInstanceId, wi.getNodeInstanceId());
        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBasicUserTaskProcessClaimReleaseClaimAndCompletePhases() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        WorkItem wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        assertEquals(0, wi.getResults().size());

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Claim.ID, Collections.singletonMap("test", "value"), securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Claim.ID, wi.getPhase());
        assertEquals(Claim.STATUS, wi.getPhaseStatus());
        assertEquals(2, wi.getResults().size());
        assertEquals("value", wi.getResults().get("test"));
        assertEquals("john", wi.getResults().get("ActorId"));

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Release.ID, null, securityPolicy));

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Release.ID, wi.getPhase());
        assertEquals(Release.STATUS, wi.getPhaseStatus());
        assertEquals(2, wi.getResults().size());
        assertEquals("value", wi.getResults().get("test"));
        assertEquals("john", wi.getResults().get("ActorId"));

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Claim.ID, Collections.singletonMap("test", "value"), securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("FirstTask", wi.getName());
        assertEquals(Claim.ID, wi.getPhase());
        assertEquals(Claim.STATUS, wi.getPhaseStatus());
        assertEquals(2, wi.getResults().size());
        assertEquals("value", wi.getResults().get("test"));
        assertEquals("john", wi.getResults().get("ActorId"));

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("SecondTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        assertEquals(0, wi.getResults().size());

        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testApprovalWithReadonlyVariableTags() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/approval-with-readonly-variable-tags.bpmn2");
        assertThat(app).isNotNull();

        Class<?> resourceClazz = Class.forName("org.acme.travels.ApprovalsModel", true, testClassLoader());
        assertNotNull(resourceClazz);

        Field approverField = resourceClazz.getDeclaredField("approver");
        assertThat(approverField).isNotNull();
        assertThat(approverField.getType().getCanonicalName()).isEqualTo(String.class.getCanonicalName());

        Process<? extends Model> p = app.processes().processById("approvals");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("approver", "john");
        m.fromMap(parameters);

        ProcessInstance processInstance = p.createInstance(m);
        processInstance.start();
        assertEquals(io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ACTIVE, processInstance.status());

        final Model updates = p.createModel();
        parameters = new HashMap<>();
        parameters.put("approver", "mary");
        updates.fromMap(parameters);
        // updating readonly variable should fail
        assertThrows(VariableViolationException.class, () -> processInstance.updateVariables(updates));

        processInstance.abort();

        assertEquals(io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ABORTED, processInstance.status());
    }

    @Test
    public void testApprovalWithInternalVariableTags() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/approval-with-internal-variable-tags.bpmn2");
        assertThat(app).isNotNull();

        Class<?> resourceClazz = Class.forName("org.acme.travels.ApprovalsModel", true, testClassLoader());
        assertNotNull(resourceClazz);
        // internal variables are not exposed on the model
        assertThrows(NoSuchFieldException.class, () -> resourceClazz.getDeclaredField("approver"));

        Process<? extends Model> p = app.processes().processById("approvals");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();
        assertEquals(io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ACTIVE, processInstance.status());

        processInstance.abort();

        assertEquals(io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ABORTED, processInstance.status());
    }

    @Test
    public void testApprovalWithRequiredVariableTags() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/approval-with-required-variable-tags.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("approvals");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        assertThrows(VariableViolationException.class, () -> {
            ProcessInstance<?> processInstance = p.createInstance(m);
            processInstance.start();
        });
    }

    @Test
    public void testApprovalWithIOVariableTags() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/approval-with-io-variable-tags.bpmn2");
        assertThat(app).isNotNull();

        Class<?> modelClazz = Class.forName("org.acme.travels.ApprovalsModel", true, testClassLoader());
        assertNotNull(modelClazz);
        assertNotNull(modelClazz.getDeclaredField("decision"));
        assertNotNull(modelClazz.getDeclaredField("approver"));

        Class<?> inputModelClazz = Class.forName("org.acme.travels.ApprovalsModelInput", true, testClassLoader());
        assertNotNull(inputModelClazz);
        assertNotNull(inputModelClazz.getDeclaredField("approver"));
        assertThrows(NoSuchFieldException.class, () -> inputModelClazz.getDeclaredField("decision"));
        assertThrows(NoSuchFieldException.class, () -> inputModelClazz.getDeclaredField("id"));

        Class<?> outputModelClazz = Class.forName("org.acme.travels.ApprovalsModelOutput", true, testClassLoader());
        assertNotNull(outputModelClazz);
        assertNotNull(outputModelClazz.getDeclaredField("decision"));
        assertNotNull(outputModelClazz.getDeclaredField("id"));
        assertThrows(NoSuchFieldException.class, () -> outputModelClazz.getDeclaredField("approver"));

        Process<? extends Model> p = app.processes().processById("approvals");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("approver", "mary");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();
        assertEquals(io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ACTIVE, processInstance.status());

        processInstance.abort();

        assertEquals(io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ABORTED, processInstance.status());
    }

    @Test
    public void testUserTaskWithIOexpressionProcess() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTaskWithIOexpression.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTask");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("person", new Person("john", 0));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("Hello", workItems.get(0).getName());
        assertEquals("john", workItems.get(0).getParameters().get("personName"));

        processInstance.completeWorkItem(workItems.get(0).getId(), Collections.singletonMap("personAge", 50),
                securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        Model output = (Model) processInstance.variables();
        Person person = (Person) output.toMap().get("person");
        assertEquals(50, person.getAge());
    }

    @Test
    public void testBasicUserTaskProcessWithBusinessKey() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);
        // assign custom business key for process instance
        ProcessInstance<?> processInstance = p.createInstance("custom id", m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        // verify that custom business key is assigned properly
        assertThat(processInstance.businessKey()).isEqualTo("custom id");

        // find the process instance by business key
        Optional<?> processInstanceByBussinesKey = p.instances().findById("custom id");
        assertThat(processInstanceByBussinesKey.isPresent()).isTrue();

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("FirstTask", workItems.get(0).getName());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("SecondTask", workItems.get(0).getName());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBasicUserTaskProcessWithDuplicatedBusinessKey() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);
        // assign custom business key for process instance
        ProcessInstance<?> processInstance = p.createInstance("custom id", m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        // verify that custom business key is assigned properly
        assertThat(processInstance.businessKey()).isEqualTo("custom id");

        // start another process instance with assigned duplicated business key of
        // already active instance
        assertThrows(ProcessInstanceDuplicatedException.class, () -> p.createInstance("custom id", m).start());

        // abort first one
        processInstance.abort();

        // now it should be possible to start second one with same business key
        ProcessInstance<?> processInstance2 = p.createInstance("custom id", m);
        processInstance2.start();
        assertThat(processInstance2.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        // verify that custom business key is assigned properly
        assertThat(processInstance2.businessKey()).isEqualTo("custom id");

        // find the process instance by business key
        Optional<?> processInstanceByBussinesKey = p.instances().findById("custom id");
        assertThat(processInstanceByBussinesKey.isPresent()).isTrue();
        processInstance2 = (ProcessInstance<?>) processInstanceByBussinesKey.get();
        processInstance2.abort();
    }

    @Test
    public void testBasicUserTaskProcessVersioned() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcessVersion.bpmn2",
                "usertask/UserTasksProcessVersion2.bpmn2");
        assertThat(app).isNotNull();
        final List<String> workItemTransitionEvents = new ArrayList<>();
        app.config().process().processEventListeners().listeners().add(new DefaultProcessEventListener() {

            @Override
            public void beforeWorkItemTransition(ProcessWorkItemTransitionEvent event) {
                workItemTransitionEvents.add("BEFORE:: " + event);
            }

            @Override
            public void afterWorkItemTransition(ProcessWorkItemTransitionEvent event) {
                workItemTransitionEvents.add("AFTER:: " + event);
            }
        });
        // verify that both versions have corresponding process definitions
        assertThat(app.processes().processById("UserTasksProcess_2")).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess_1");
        assertThat(p).isNotNull();

        // verify that both version have corresponding data model classes
        Class<?> model1Class = Class.forName("org.kie.kogito.test.UserTasksProcess_1Model", true, testClassLoader());
        assertNotNull(model1Class);
        Class<?> model2Class = Class.forName("org.kie.kogito.test.UserTasksProcess_2Model", true, testClassLoader());
        assertNotNull(model2Class);

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("FirstTask", workItems.get(0).getName());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("SecondTask", workItems.get(0).getName());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        assertThat(workItemTransitionEvents).hasSize(8);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testBasicUserTaskProcessWithTags() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcessTags.bpmn2");
        assertThat(app).isNotNull();
        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(processInstance.tags()).isNotNull();
        assertThat(processInstance.tags().values()).hasSize(2).contains("important", "john");

        processInstance.tags().add("custom tag");

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("FirstTask", workItems.get(0).getName());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        assertThat(processInstance.tags().values()).hasSize(3).contains("important", "john", "custom tag");

        Tag delete = processInstance.tags().get().stream().filter(t -> t.getValue().equals("custom tag")).findFirst().get();
        processInstance.tags().remove(delete.getId());

        m = p.createModel();
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("name", "mary");
        m.fromMap(updates);
        ((ProcessInstance) processInstance).updateVariables(m);

        assertThat(processInstance.tags().values()).hasSize(2).contains("important", "mary");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("SecondTask", workItems.get(0).getName());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        assertThat(processInstance.tags().values()).hasSize(2).doesNotContain(delete.getValue());
    }
}
