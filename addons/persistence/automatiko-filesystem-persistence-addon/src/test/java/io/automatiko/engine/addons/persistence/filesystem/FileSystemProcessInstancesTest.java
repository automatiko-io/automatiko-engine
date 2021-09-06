
package io.automatiko.engine.addons.persistence.filesystem;

import static io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED;
import static io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory;
import io.automatiko.engine.addons.persistence.data.Address;
import io.automatiko.engine.addons.persistence.data.Person;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.api.uow.UnitOfWork;
import io.automatiko.engine.api.uow.UnitOfWorkManager;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessConfig;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.ProcessInstances;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.services.identity.StaticIdentityProvider;
import io.automatiko.engine.services.io.ClassPathResource;
import io.automatiko.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatiko.engine.services.uow.DefaultUnitOfWorkManager;
import io.automatiko.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatiko.engine.workflow.DefaultWorkItemHandlerConfig;
import io.automatiko.engine.workflow.StaticProcessConfig;
import io.automatiko.engine.workflow.base.instance.context.variable.DefaultVariableInitializer;
import io.automatiko.engine.workflow.base.instance.impl.Action;
import io.automatiko.engine.workflow.bpmn2.BpmnProcess;
import io.automatiko.engine.workflow.bpmn2.BpmnVariables;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.node.ActionNode;

public class FileSystemProcessInstancesTest {

    private static final String PERSISTENCE_FOLDER = "target" + File.separator + "persistence-test";

    private SecurityPolicy securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));

    @BeforeEach
    public void setup() throws IOException {
        Path path = Paths.get(PERSISTENCE_FOLDER);

        if (Files.exists(path)) {
            Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    private BpmnProcess createProcess(ProcessConfig config, String fileName) {
        BpmnProcess process = BpmnProcess.from(config, new ClassPathResource(fileName)).get(0);

        process.setProcessInstancesFactory(new FileSystemProcessInstancesFactory());
        process.configure();
        process.instances().values(ProcessInstanceReadMode.MUTABLE, 1, 10).forEach(p -> p.abort());
        return process;
    }

    @Test
    void testFindByIdReadMode() {
        BpmnProcess process = createProcess(null, "BPMN2-UserTask-Script.bpmn2");
        // workaround as BpmnProcess does not compile the scripts but just reads the xml
        for (io.automatiko.engine.api.definition.process.Node node : ((WorkflowProcess) process.process()).getNodes()) {
            if (node instanceof ActionNode) {
                ProcessAction a = ((ActionNode) node).getAction();
                a.removeMetaData("Action");
                a.setMetaData("Action", new Action() {

                    @Override
                    public void execute(ProcessContext kcontext) throws Exception {
                        System.out.println(
                                "The variable value is " + kcontext.getVariable("s") + " about to call toString on it");
                        kcontext.getVariable("s").toString();
                    }
                });
            }
        }
        ProcessInstance<BpmnVariables> mutablePi = process
                .createInstance(BpmnVariables.create(Collections.singletonMap("var", "value")));

        mutablePi.start();
        assertThat(mutablePi.status()).isEqualTo(STATE_ERROR);
        assertThat(mutablePi.errors()).hasValueSatisfying(errors -> {
            assertThat(errors.errorMessages()).isEqualTo("null");
            assertThat(errors.failedNodeIds()).isEqualTo("ScriptTask_1");
        });
        assertThat(mutablePi.variables().toMap()).containsExactly(entry("var", "value"));

        ProcessInstances<BpmnVariables> instances = process.instances();
        assertThat(instances.size()).isOne();
        ProcessInstance<BpmnVariables> pi = instances
                .findById(mutablePi.id(), ProcessInstance.STATE_ERROR, ProcessInstanceReadMode.READ_ONLY).get();
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> pi.abort());

        ProcessInstance<BpmnVariables> readOnlyPi = instances
                .findById(mutablePi.id(), ProcessInstance.STATE_ERROR, ProcessInstanceReadMode.READ_ONLY).get();
        assertThat(readOnlyPi.status()).isEqualTo(STATE_ERROR);
        assertThat(readOnlyPi.errors()).hasValueSatisfying(errors -> {
            assertThat(errors.errorMessages()).isEqualTo("");
            assertThat(errors.failedNodeIds()).isEqualTo("ScriptTask_1");
        });
        assertThat(readOnlyPi.variables().toMap()).containsExactly(entry("var", "value"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> readOnlyPi.abort());

        instances.findById(mutablePi.id(), ProcessInstance.STATE_ERROR, ProcessInstanceReadMode.MUTABLE).get().abort();
        assertThat(instances.size()).isZero();
    }

    @Test
    void testValuesReadMode() {
        BpmnProcess process = createProcess(null, "BPMN2-UserTask.bpmn2");
        ProcessInstance<BpmnVariables> processInstance = process
                .createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));
        processInstance.start();

        ProcessInstances<BpmnVariables> instances = process.instances();
        assertThat(instances.size()).isOne();
        ProcessInstance<BpmnVariables> pi = instances.values(1, 10).stream().findFirst().get();
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> pi.abort());
        instances.values(ProcessInstanceReadMode.MUTABLE, 1, 10).stream().findFirst().get().abort();
        assertThat(instances.size()).isZero();
    }

    @Test
    void testBasicFlow() {
        BpmnProcess process = createProcess(null, "BPMN2-UserTask.bpmn2");
        ProcessInstance<BpmnVariables> processInstance = process
                .createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(STATE_ACTIVE);
        assertThat(processInstance.description()).isEqualTo("User Task");

        FileSystemProcessInstances fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
        assertThat(fileSystemBasedStorage.size()).isOne();

        Collection findByTag = fileSystemBasedStorage.findByIdOrTag(ProcessInstanceReadMode.READ_ONLY, "important");
        assertThat(findByTag).hasSize(1);

        verify(fileSystemBasedStorage, times(2)).create(any(), any());
        verify(fileSystemBasedStorage, times(1)).setMetadata(any(), eq(FileSystemProcessInstances.PI_DESCRIPTION),
                eq("User Task"));
        verify(fileSystemBasedStorage, times(1)).setMetadata(any(), eq(FileSystemProcessInstances.PI_STATUS), eq("1"));

        String testVar = (String) processInstance.variables().get("test");
        assertThat(testVar).isEqualTo("test");

        assertThat(processInstance.description()).isEqualTo("User Task");

        assertThat(process.instances().values(1, 10).iterator().next().workItems(securityPolicy)).hasSize(1);

        WorkItem workItem = processInstance.workItems(securityPolicy).get(0);
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameters().get("ActorId")).isEqualTo("john");
        processInstance.completeWorkItem(workItem.getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(STATE_COMPLETED);

        fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
        verify(fileSystemBasedStorage, times(2)).remove(any(), any());

        assertThat(fileSystemBasedStorage.size()).isZero();
    }

    @Test
    void testBasicFlowWithStartFrom() {
        BpmnProcess process = createProcess(null, "BPMN2-UserTask.bpmn2");

        ProcessInstance<BpmnVariables> processInstance = process
                .createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

        processInstance.startFrom("_2");

        assertThat(processInstance.status()).isEqualTo(STATE_ACTIVE);
        assertThat(processInstance.description()).isEqualTo("User Task");

        FileSystemProcessInstances fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
        verify(fileSystemBasedStorage, times(1)).update(any(), any());

        String testVar = (String) processInstance.variables().get("test");
        assertThat(testVar).isEqualTo("test");

        assertThat(processInstance.description()).isEqualTo("User Task");

        WorkItem workItem = processInstance.workItems(securityPolicy).get(0);
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameters().get("ActorId")).isEqualTo("john");
        processInstance.completeWorkItem(workItem.getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(STATE_COMPLETED);

        fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
        verify(fileSystemBasedStorage, times(2)).remove(any(), any());

        assertThat(fileSystemBasedStorage.size()).isZero();
    }

    @Test
    void testBasicFlowControlledByUnitOfWork() {

        UnitOfWorkManager uowManager = new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory());
        ProcessConfig config = new StaticProcessConfig(new DefaultWorkItemHandlerConfig(),
                new DefaultProcessEventListenerConfig(), uowManager, null, new DefaultVariableInitializer(),
                new FileSystemProcessInstancesFactory());
        BpmnProcess process = createProcess(config, "BPMN2-UserTask.bpmn2");
        process.configure();

        ProcessInstance<BpmnVariables> processInstance = process
                .createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

        UnitOfWork uow = uowManager.newUnitOfWork();
        uow.start();

        processInstance.start();

        uow.end();
        assertThat(processInstance.status()).isEqualTo(STATE_ACTIVE);
        assertThat(processInstance.description()).isEqualTo("User Task");

        assertThat(process.instances().values(1, 10)).hasSize(1);

        FileSystemProcessInstances fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
        verify(fileSystemBasedStorage, times(2)).create(any(), any());
        verify(fileSystemBasedStorage, times(1)).setMetadata(any(), eq(FileSystemProcessInstances.PI_DESCRIPTION),
                eq("User Task"));
        verify(fileSystemBasedStorage, times(1)).setMetadata(any(), eq(FileSystemProcessInstances.PI_STATUS), eq("1"));

        String testVar = (String) processInstance.variables().get("test");
        assertThat(testVar).isEqualTo("test");

        assertThat(processInstance.description()).isEqualTo("User Task");

        WorkItem workItem = processInstance.workItems(securityPolicy).get(0);
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameters().get("ActorId")).isEqualTo("john");

        uow = uowManager.newUnitOfWork();
        uow.start();
        processInstance.completeWorkItem(workItem.getId(), null, securityPolicy);
        uow.end();

        assertThat(processInstance.status()).isEqualTo(STATE_COMPLETED);

        fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
        verify(fileSystemBasedStorage, times(1)).remove(any(), any());

        assertThat(fileSystemBasedStorage.size()).isZero();
    }

    @Test
    public void testComplexVariableFlow() {

        BpmnProcess process = (BpmnProcess) BpmnProcess.from(new ClassPathResource("BPMN2-PersonUserTask.bpmn2"))
                .get(0);
        process.setProcessInstancesFactory(new FileSystemProcessInstancesFactory());
        process.configure();

        Person person = new Person("John", 30);
        Address mainAddress = new Address("first", "Brisbane", "00000", "Australia", true);
        Address secondaryAddress = new Address("second", "Syndey", "11111", "Australia", false);

        person.addAddress(mainAddress);
        person.addAddress(secondaryAddress);

        ProcessInstance<BpmnVariables> processInstance = process
                .createInstance(BpmnVariables.create(Collections.singletonMap("person", person)));

        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(STATE_ACTIVE);
        assertThat(processInstance.description()).isEqualTo("User Task");

        assertThat(process.instances().values(1, 10)).hasSize(1);

        FileSystemProcessInstances fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
        verify(fileSystemBasedStorage, times(2)).create(any(), any());
        verify(fileSystemBasedStorage, times(1)).setMetadata(any(), eq(FileSystemProcessInstances.PI_DESCRIPTION),
                eq("User Task"));
        verify(fileSystemBasedStorage, times(1)).setMetadata(any(), eq(FileSystemProcessInstances.PI_STATUS), eq("1"));

        Person testVar = (Person) processInstance.variables().get("person");
        assertThat(testVar).isEqualTo(person);

        assertThat(processInstance.description()).isEqualTo("User Task");

        WorkItem workItem = processInstance.workItems(securityPolicy).get(0);
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameters().get("ActorId")).isEqualTo("john");
        processInstance.completeWorkItem(workItem.getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(STATE_COMPLETED);

        fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
        verify(fileSystemBasedStorage, times(2)).remove(any(), any());
    }

    @Test
    void testBasicFlowVersionedProcess() {
        BpmnProcess process = createProcess(null, "BPMN2-UserTaskVersioned.bpmn2");
        ProcessInstance<BpmnVariables> processInstance = process
                .createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(STATE_ACTIVE);
        assertThat(processInstance.description()).isEqualTo("User Task");

        FileSystemProcessInstances fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
        assertThat(fileSystemBasedStorage.size()).isOne();

        verify(fileSystemBasedStorage, times(2)).create(any(), any());
        verify(fileSystemBasedStorage, times(1)).setMetadata(any(), eq(FileSystemProcessInstances.PI_DESCRIPTION),
                eq("User Task"));
        verify(fileSystemBasedStorage, times(1)).setMetadata(any(), eq(FileSystemProcessInstances.PI_STATUS), eq("1"));

        String testVar = (String) processInstance.variables().get("test");
        assertThat(testVar).isEqualTo("test");

        assertThat(processInstance.description()).isEqualTo("User Task");

        assertThat(process.instances().values(1, 10).iterator().next().workItems(securityPolicy)).hasSize(1);

        WorkItem workItem = processInstance.workItems(securityPolicy).get(0);
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameters().get("ActorId")).isEqualTo("john");
        processInstance.completeWorkItem(workItem.getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(STATE_COMPLETED);

        fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
        verify(fileSystemBasedStorage, times(2)).remove(any(), any());

        assertThat(fileSystemBasedStorage.size()).isZero();
    }

    @Test
    void testBasicFlowWithTransientVariable() {
        BpmnProcess process = createProcess(null, "BPMN2-UserTask.bpmn2");
        ProcessInstance<BpmnVariables> processInstance = process
                .createInstance(BpmnVariables.create(Collections.singletonMap("temp", "test")));

        processInstance.start();

        assertThat(processInstance.variables().get("temp")).isEqualTo("test");

        assertThat(processInstance.status()).isEqualTo(STATE_ACTIVE);
        assertThat(processInstance.description()).isEqualTo("User Task");

        FileSystemProcessInstances fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
        assertThat(fileSystemBasedStorage.size()).isOne();

        ProcessInstance<BpmnVariables> processInstanceLoaded = (ProcessInstance<BpmnVariables>) fileSystemBasedStorage
                .findById(processInstance.id()).get();
        assertThat(processInstanceLoaded.variables().get("temp")).isNull();

        WorkItem workItem = processInstance.workItems(securityPolicy).get(0);
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameters().get("ActorId")).isEqualTo("john");
        processInstance.completeWorkItem(workItem.getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(STATE_COMPLETED);

        fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
        verify(fileSystemBasedStorage, times(2)).remove(any(), any());

        assertThat(fileSystemBasedStorage.size()).isZero();
    }

    private class FileSystemProcessInstancesFactory extends AbstractProcessInstancesFactory {

        @Override
        public FileSystemProcessInstances createProcessInstances(Process<?> process) {
            FileSystemProcessInstances instances = spy(super.createProcessInstances(process));
            return instances;
        }

        @Override
        public String path() {
            return PERSISTENCE_FOLDER;
        }
    }
}
