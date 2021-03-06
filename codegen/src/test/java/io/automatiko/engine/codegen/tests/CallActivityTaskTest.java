
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.codegen.data.Address;
import io.automatiko.engine.codegen.data.Person;
import io.automatiko.engine.codegen.data.PersonWithAddress;
import io.automatiko.engine.services.identity.StaticIdentityProvider;
import io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskTransition;
import io.automatiko.engine.workflow.base.instance.impl.workitem.Active;
import io.automatiko.engine.workflow.base.instance.impl.workitem.Complete;

public class CallActivityTaskTest extends AbstractCodegenTest {

    private Policy<?> securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));

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

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");
        assertThat(result.toMap().get("y")).isNotNull().isEqualTo("new value");
        assertThat(result.toMap().get("x")).isNotNull().isEqualTo("a");
    }

    @Test
    public void testBasicCallActivityTaskWithTypeInfo() throws Exception {

        Application app = generateCodeProcessesOnly("subprocess/CallActivityWithTypeInfo.bpmn2",
                "subprocess/CallActivitySubProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ParentProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", "a");
        parameters.put("y", "b");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");
        assertThat(result.toMap().get("y")).isNotNull().isEqualTo("new value");
        assertThat(result.toMap().get("x")).isNotNull().isEqualTo("a");
    }

    @Test
    public void testCallActivityTaskMultiInstance() throws Exception {

        Application app = generateCodeProcessesOnly("subprocess/CallActivityMI.bpmn2",
                "subprocess/CallActivitySubProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ParentProcess");

        List<String> list = new ArrayList<String>();
        list.add("first");
        list.add("second");
        List<String> listOut = new ArrayList<String>();

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("list", list);
        parameters.put("listOut", listOut);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(4).containsKeys("x", "y", "list", "listOut");
        assertThat((List<?>) result.toMap().get("listOut")).isNotNull().hasSize(2);

    }

    @Test
    public void testCallActivityTaskWithExpressionsForIO() throws Exception {

        Application app = generateCodeProcessesOnly("subprocess/CallActivityWithIOexpression.bpmn2",
                "subprocess/CallActivitySubProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ParentProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("person", new Person("john", 0));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("person");

        Person person = (Person) result.toMap().get("person");
        assertEquals("new value", person.getName());

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        WorkItem wi = workItems.get(0);
        assertEquals("MyTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testCallActivityTaskWithExpressionsForIONested() throws Exception {

        Application app = generateCodeProcessesOnly("subprocess/CallActivityWithIOexpressionNested.bpmn2",
                "subprocess/CallActivitySubProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ParentProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        PersonWithAddress pa = new PersonWithAddress("john", 0);
        pa.setAddress(new Address("test", null, null, null));
        parameters.put("person", pa);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("person");

        PersonWithAddress person = (PersonWithAddress) result.toMap().get("person");
        assertEquals("john", person.getName());
        assertEquals("test", person.getAddress().getStreet());
        assertEquals("new value", person.getAddress().getCity());

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        WorkItem wi = workItems.get(0);
        assertEquals("MyTask", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());

        processInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBasicCallActivityTaskWithSingleVarExpression() throws Exception {

        Application app = generateCodeProcessesOnly("subprocess/CallActivityVarIOExpression.bpmn2",
                "subprocess/CallActivitySubProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ParentProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", "a");
        parameters.put("y", "b");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");
        assertThat(result.toMap().get("y")).isNotNull().isEqualTo("new value");
        assertThat(result.toMap().get("x")).isNotNull().isEqualTo("a");
    }

    @Test
    public void testCallActivityTaskWithSubprocesWait() throws Exception {

        Application app = generateCodeProcessesOnly("subprocess/CallActivity.bpmn2",
                "subprocess/CallActivitySubProcessHT.bpmn2", "subprocess/CallActivitySubProcessHT2.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ParentProcess_1");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", "a");
        parameters.put("y", "b");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance("custom-key", m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");

        assertThat(p.instances().findById("custom-key")).isPresent();
        assertThat(p.instances().findById("b")).isNotPresent();

        // check if tasks can be accessed via parent process instance even if they are
        // in subprocesses
        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        WorkItem wi = workItems.get(0);
        assertEquals("User Task 1", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        // following check shows that task(wi) comes from another process instance
        assertNotEquals(processInstance.id(), wi.getProcessInstanceId());

        Collection<ProcessInstance<? extends Model>> subprocesses = processInstance.subprocesses();
        assertThat(subprocesses).hasSize(1);

        ProcessInstance<?> childProcessInstance = subprocesses.iterator().next();

        // verify that sub processes can be access by business key defined via variable tagging
        Process<? extends Model> sp = (Process<? extends Model>) childProcessInstance.process();
        Optional<?> found = sp.instances().findById("custom-key:a");
        assertThat(found).isPresent();

        workItems = childProcessInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("User Task 1", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());

        childProcessInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("User Task 2", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        // following check shows that task(wi) comes from another process instance
        assertNotEquals(processInstance.id(), wi.getProcessInstanceId());

        subprocesses = childProcessInstance.subprocesses();
        assertThat(subprocesses).hasSize(1);

        ProcessInstance<?> subChildProcessInstance = subprocesses.iterator().next();
        subChildProcessInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testCallActivityTaskWithSubprocesWaitBusinessKeyFromVars() throws Exception {

        Application app = generateCodeProcessesOnly("subprocess/CallActivity.bpmn2",
                "subprocess/CallActivitySubProcessHT.bpmn2", "subprocess/CallActivitySubProcessHT2.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ParentProcess_1");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", "a");
        parameters.put("y", "b");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");

        assertThat(p.instances().findById("b")).isPresent();

        // check if tasks can be accessed via parent process instance even if they are
        // in subprocesses
        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        WorkItem wi = workItems.get(0);
        assertEquals("User Task 1", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        // following check shows that task(wi) comes from another process instance
        assertNotEquals(processInstance.id(), wi.getProcessInstanceId());

        Collection<ProcessInstance<? extends Model>> subprocesses = processInstance.subprocesses();
        assertThat(subprocesses).hasSize(1);

        ProcessInstance<?> childProcessInstance = subprocesses.iterator().next();

        // verify that sub processes can be access by business key defined via variable tagging
        Process<? extends Model> sp = (Process<? extends Model>) childProcessInstance.process();
        Optional<?> found = sp.instances().findById("b:a");
        assertThat(found).isPresent();

        workItems = childProcessInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("User Task 1", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());

        childProcessInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        wi = workItems.get(0);
        assertEquals("User Task 2", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        // following check shows that task(wi) comes from another process instance
        assertNotEquals(processInstance.id(), wi.getProcessInstanceId());

        subprocesses = childProcessInstance.subprocesses();
        assertThat(subprocesses).hasSize(1);

        ProcessInstance<?> subChildProcessInstance = subprocesses.iterator().next();
        subChildProcessInstance.transitionWorkItem(workItems.get(0).getId(),
                new HumanTaskTransition(Complete.ID, null, securityPolicy));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBasicCallActivityTaskVersioned() throws Exception {

        Application app = generateCodeProcessesOnly("subprocess/CallActivityVersioned.bpmn2",
                "subprocess/CallActivitySubProcessVersioned.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ParentProcess_1");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", "a");
        parameters.put("y", "b");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");
        assertThat(result.toMap().get("y")).isNotNull().isEqualTo("new value");
        assertThat(result.toMap().get("x")).isNotNull().isEqualTo("a");
    }

    @Test
    public void testCallActivityTaskWithSubprocesWaitAndAbort() throws Exception {

        Application app = generateCodeProcessesOnly("subprocess/CallActivity.bpmn2",
                "subprocess/CallActivitySubProcessHT.bpmn2", "subprocess/CallActivitySubProcessHT2.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ParentProcess_1");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", "a");
        parameters.put("y", "b");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");

        // check if tasks can be accessed via parent process instance even if they are
        // in subprocesses
        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        WorkItem wi = workItems.get(0);
        assertEquals("User Task 1", wi.getName());
        assertEquals(Active.ID, wi.getPhase());
        assertEquals(Active.STATUS, wi.getPhaseStatus());
        // following check shows that task(wi) comes from another process instance
        assertNotEquals(processInstance.id(), wi.getProcessInstanceId());

        Collection<ProcessInstance<? extends Model>> subprocesses = processInstance.subprocesses();
        assertThat(subprocesses).hasSize(1);

        ProcessInstance<?> childProcessInstance = subprocesses.iterator().next();

        processInstance.abort();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
        assertThat(childProcessInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);

        subprocesses = processInstance.subprocesses();
        assertThat(subprocesses).hasSize(0);

        assertThat(p.instances().size()).isEqualTo(0);
        assertThat(childProcessInstance.process().instances().size()).isEqualTo(0);
    }
}
