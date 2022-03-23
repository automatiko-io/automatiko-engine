
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.codegen.process.ProcessNodeLocator;
import io.automatiko.engine.workflow.AbstractProcess;
import io.automatiko.engine.workflow.Sig;
import io.automatiko.engine.workflow.process.core.node.FaultNode;

public class ErrorEndTest extends AbstractCodegenTest {

    @Test
    public void testBasicErrorEndEvent() throws Exception {

        Application app = generateCodeProcessesOnly("end-events/BasicErrorProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("samples");

        Collection<FaultNode> errors = ProcessNodeLocator.findFaultNodes(((AbstractProcess<?>) p).process(),
                ((WorkflowProcess) ((AbstractProcess<?>) p).process()).getNodes()[0]);
        assertThat(errors).hasSize(1);

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
        assertThat(processInstance.abortCode()).isEqualTo("409");
        assertThat(processInstance.abortData()).isEqualTo("error");

    }

    @Test
    public void testAfterEmbeddedSubprocessErrorEndEvent() throws Exception {

        Application app = generateCodeProcessesOnly("end-events/AfterSubprocessErrorProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("samples");

        Collection<FaultNode> errors = ProcessNodeLocator.findFaultNodes(((AbstractProcess<?>) p).process(),
                ((WorkflowProcess) ((AbstractProcess<?>) p).process()).getNodes()[0]);
        assertThat(errors).hasSize(1);

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
        assertThat(processInstance.abortCode()).isEqualTo("409");
        assertThat(processInstance.abortData()).isEqualTo("error");

    }

    @Test
    public void testBasicErrorEndEventWithWaitState() throws Exception {

        Application app = generateCodeProcessesOnly("end-events/BasicErrorProcessWithWaitState.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("samples");

        Collection<FaultNode> errors = ProcessNodeLocator.findFaultNodes(((AbstractProcess<?>) p).process(),
                ((WorkflowProcess) ((AbstractProcess<?>) p).process()).getNodes()[0]);
        assertThat(errors).hasSize(0);

        errors = ProcessNodeLocator.findFaultNodes(((AbstractProcess<?>) p).process(),
                ((WorkflowProcess) ((AbstractProcess<?>) p).process()).getNodesRecursively().stream()
                        .filter(n -> n.getName().equals("wait")).findFirst().get());
        assertThat(errors).hasSize(1);

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> tasks = processInstance.workItems();
        assertThat(tasks).hasSize(1);

        processInstance.completeWorkItem(tasks.get(0).getId(), null);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
        assertThat(processInstance.abortCode()).isEqualTo("409");
        assertThat(processInstance.abortData()).isEqualTo("error");

    }

    @Test
    public void testInsideEmbeddedSubprocessErrorEndEvent() throws Exception {

        Application app = generateCodeProcessesOnly("end-events/InsideSubprocessErrorProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("samples");

        Collection<FaultNode> errors = ProcessNodeLocator.findFaultNodes(((AbstractProcess<?>) p).process(),
                ((WorkflowProcess) ((AbstractProcess<?>) p).process()).getNodes()[0]);
        assertThat(errors).hasSize(1);

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
        assertThat(processInstance.abortCode()).isEqualTo("409");
        assertThat(processInstance.abortData()).isEqualTo("error");

    }

    @Test
    public void testBasicErrorEndEventWithSignalWaitState() throws Exception {

        Application app = generateCodeProcessesOnly("end-events/BasicErrorProcessWithSignalWaitState.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("samples");

        Collection<FaultNode> errors = ProcessNodeLocator.findFaultNodes(((AbstractProcess<?>) p).process(),
                ((WorkflowProcess) ((AbstractProcess<?>) p).process()).getNodes()[0]);
        assertThat(errors).hasSize(0);

        errors = ProcessNodeLocator.findFaultNodes(((AbstractProcess<?>) p).process(),
                ((WorkflowProcess) ((AbstractProcess<?>) p).process()).getNodesRecursively().stream()
                        .filter(n -> n.getName().equals("wait")).findFirst().get());
        assertThat(errors).hasSize(1);

        Model m = p.createModel();

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        processInstance.send(Sig.of("test", "updated"));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
        assertThat(processInstance.abortCode()).isEqualTo("409");
        assertThat(processInstance.abortData()).isEqualTo("updated");

    }
}
