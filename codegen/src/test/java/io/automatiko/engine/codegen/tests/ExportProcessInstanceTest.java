
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.workflow.ArchiveBuilder;
import io.automatiko.engine.api.workflow.ArchivedProcessInstance;
import io.automatiko.engine.api.workflow.ArchivedVariable;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.services.identity.StaticIdentityProvider;
import io.automatiko.engine.workflow.marshalling.ProcessInstanceMarshaller;

public class ExportProcessInstanceTest extends AbstractCodegenTest {

    private Policy<?> securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));

    @Test
    public void testBasicUserTaskProcess() throws Exception {

        ProcessInstanceMarshaller marshaller = new ProcessInstanceMarshaller();

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
        assertEquals("FirstTask", workItems.get(0).getName());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        // exprt process instance
        ExportedProcessInstance exported = marshaller.exportProcessInstance(processInstance);
        assertThat(exported).isNotNull();

        ProcessInstance<?> imported = marshaller.importProcessInstance(exported, p);
        workItems = imported.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("SecondTask", workItems.get(0).getName());

        imported.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(imported.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testBasicUserTaskProcessArchive() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "John");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("FirstTask", workItems.get(0).getName());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        // archive active process instance
        ArchivedProcessInstance archived = processInstance.archive(new ArchiveBuilder() {

            @Override
            public ArchivedVariable variable(String name, Object value) {
                return new ArchivedVariable(name, value) {

                    @Override
                    public byte[] data() {
                        return getValue().toString().getBytes();
                    }
                };
            }

            @Override
            public ArchivedProcessInstance instance(String id, ExportedProcessInstance<?> exported) {
                return new ArchivedProcessInstance(id, exported);
            }
        });
        assertThat(archived).isNotNull();
        assertThat(archived.getExport()).isNotNull();
        assertThat(archived.getVariables()).isNotNull();
        assertThat(archived.getSubInstances()).isNotNull();
        assertThat(archived.getVariables()).hasSize(1);
        assertThat(archived.getSubInstances()).hasSize(0);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("SecondTask", workItems.get(0).getName());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        // and now let's export completed instance
        archived = processInstance.archive(new ArchiveBuilder() {

            @Override
            public ArchivedVariable variable(String name, Object value) {
                return new ArchivedVariable(name, value) {

                    @Override
                    public byte[] data() {
                        return getValue().toString().getBytes();
                    }
                };
            }

            @Override
            public ArchivedProcessInstance instance(String id, ExportedProcessInstance<?> exported) {
                return new ArchivedProcessInstance(id, exported);
            }
        });
        assertThat(archived).isNotNull();
        assertThat(archived.getExport()).isNotNull();
        assertThat(archived.getVariables()).isNotNull();
        assertThat(archived.getSubInstances()).isNotNull();
        assertThat(archived.getVariables()).hasSize(1);
        assertThat(archived.getSubInstances()).hasSize(0);

    }

}
