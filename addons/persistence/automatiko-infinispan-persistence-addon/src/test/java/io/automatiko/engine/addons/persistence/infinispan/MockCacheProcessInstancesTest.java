
package io.automatiko.engine.addons.persistence.infinispan;

import static io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED;
import static io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCacheManagerAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.api.workflow.ProcessErrors;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceNotFoundException;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.ProcessInstances;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.services.identity.StaticIdentityProvider;
import io.automatiko.engine.services.io.ClassPathResource;
import io.automatiko.engine.workflow.base.instance.impl.Action;
import io.automatiko.engine.workflow.bpmn2.BpmnProcess;
import io.automatiko.engine.workflow.bpmn2.BpmnVariables;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;
import io.automatiko.engine.workflow.process.core.node.ActionNode;

public class MockCacheProcessInstancesTest {

    private final ConcurrentHashMap<Object, Object> mockCache = new ConcurrentHashMap<>();
    private RemoteCacheManager cacheManager;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setup() {
        mockCache.clear();
        cacheManager = mock(RemoteCacheManager.class);
        RemoteCacheManagerAdmin admin = mock(RemoteCacheManagerAdmin.class);
        RemoteCache<Object, Object> cache = mock(RemoteCache.class);

        when(cacheManager.administration()).thenReturn(admin);
        when(admin.getOrCreateCache(any(), (String) any())).thenReturn(cache);

        when(cache.put(any(), any())).then(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object key = invocation.getArgument(0, Object.class);
                Object value = invocation.getArgument(1, Object.class);
                return mockCache.put(key, value);
            }
        });
        when(cache.putIfAbsent(any(), any())).then(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object key = invocation.getArgument(0, Object.class);
                Object value = invocation.getArgument(1, Object.class);
                return mockCache.put(key, value);
            }
        });

        when(cache.get(any())).then(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object key = invocation.getArgument(0, Object.class);

                return mockCache.get(key);
            }
        });
        when(cache.remove(any())).then(invocation -> {
            Object key = invocation.getArgument(0, Object.class);
            return mockCache.remove(key);
        });
        when(cache.size()).then(invocation -> mockCache.size());

        when(cache.containsKey(any())).thenReturn(true);
    }

    @Test
    void testFindByIdReadMode() {
        BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-UserTask-Script.bpmn2")).get(0);
        // workaround as BpmnProcess does not compile the scripts but just reads the xml
        for (Node node : ((WorkflowProcess) process.process()).getNodes()) {
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
        process.setProcessInstancesFactory(new CacheProcessInstancesFactory(cacheManager));
        process.configure();

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
        ProcessInstance<BpmnVariables> pi = instances.findById(mutablePi.id(), ProcessInstanceReadMode.READ_ONLY).get();
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> pi.abort());

        ProcessInstance<BpmnVariables> readOnlyPi = instances
                .findById(mutablePi.id(), ProcessInstanceReadMode.READ_ONLY).get();
        assertThat(readOnlyPi.status()).isEqualTo(STATE_ERROR);
        assertThat(readOnlyPi.errors()).hasValueSatisfying(errors -> {
            assertThat(errors.errorMessages()).isEqualTo("");
            assertThat(errors.failedNodeIds()).isEqualTo("ScriptTask_1");
        });
        assertThat(readOnlyPi.variables().toMap()).containsExactly(entry("var", "value"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> readOnlyPi.abort());

        instances.findById(mutablePi.id()).get().abort();
        assertThat(instances.size()).isZero();
    }

    @Test
    public void testBasicFlow() {

        BpmnProcess process = (BpmnProcess) BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2")).get(0);
        process.setProcessInstancesFactory(new CacheProcessInstancesFactory(cacheManager));
        process.configure();

        ProcessInstance<BpmnVariables> processInstance = process
                .createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

        processInstance.start();
        assertThat(processInstance.status()).isEqualTo(STATE_ACTIVE);

        WorkItem workItem = processInstance.workItems(SecurityPolicy.of(new StaticIdentityProvider("john"))).get(0);
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameters().get("ActorId")).isEqualTo("john");
        processInstance.completeWorkItem(workItem.getId(), null, SecurityPolicy.of(new StaticIdentityProvider("john")));
        assertThat(processInstance.status()).isEqualTo(STATE_COMPLETED);
    }

    @Test
    public void testBasicFlowNoActors() {

        BpmnProcess process = (BpmnProcess) BpmnProcess.from(new ClassPathResource("BPMN2-UserTask-NoActors.bpmn2"))
                .get(0);
        process.setProcessInstancesFactory(new CacheProcessInstancesFactory(cacheManager));
        process.configure();

        ProcessInstance<BpmnVariables> processInstance = process
                .createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

        processInstance.start();
        assertThat(processInstance.status()).isEqualTo(STATE_ACTIVE);

        WorkItem workItem = processInstance.workItems().get(0);
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameters().get("ActorId")).isNull();

        List<WorkItem> workItems = processInstance.workItems(SecurityPolicy.of(new StaticIdentityProvider("john")));
        assertThat(workItems).hasSize(1);

        processInstance.completeWorkItem(workItem.getId(), null);
        assertThat(processInstance.status()).isEqualTo(STATE_COMPLETED);
    }

    @Test
    public void testProcessInstanceNotFound() {

        BpmnProcess process = (BpmnProcess) BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2")).get(0);
        process.setProcessInstancesFactory(new CacheProcessInstancesFactory(cacheManager));
        process.configure();

        ProcessInstance<BpmnVariables> processInstance = process
                .createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

        processInstance.start();
        assertThat(processInstance.status()).isEqualTo(STATE_ACTIVE);
        mockCache.clear();

        assertThatThrownBy(() -> processInstance.workItems().get(0))
                .isInstanceOf(ProcessInstanceNotFoundException.class);

        Optional<? extends ProcessInstance<BpmnVariables>> loaded = process.instances().findById(processInstance.id());
        assertThat(loaded).isNotPresent();
    }

    @Test
    public void testBasicFlowWithErrorAndRetry() {

        testBasicFlowWithError((processInstance) -> {
            processInstance.updateVariables(BpmnVariables.create(Collections.singletonMap("s", "test")));
            processInstance.errors().orElseThrow(() -> new IllegalStateException("Process instance not in error"))
                    .retrigger();
        });
    }

    @Test
    public void testBasicFlowWithErrorAndSkip() {

        testBasicFlowWithError((processInstance) -> {
            processInstance.updateVariables(BpmnVariables.create(Collections.singletonMap("s", "test")));
            processInstance.errors().orElseThrow(() -> new IllegalStateException("Process instance not in error"))
                    .skip();
        });
    }

    private void testBasicFlowWithError(Consumer<ProcessInstance<BpmnVariables>> op) {

        BpmnProcess process = (BpmnProcess) BpmnProcess.from(new ClassPathResource("BPMN2-UserTask-Script.bpmn2"))
                .get(0);
        // workaround as BpmnProcess does not compile the scripts but just reads the xml
        for (Node node : ((WorkflowProcess) process.process()).getNodes()) {
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
        process.setProcessInstancesFactory(new CacheProcessInstancesFactory(cacheManager));
        process.configure();

        ProcessInstance<BpmnVariables> processInstance = process.createInstance(BpmnVariables.create());

        processInstance.start();
        assertThat(processInstance.status()).isEqualTo(STATE_ERROR);

        Optional<ProcessErrors> errorOp = processInstance.errors();
        assertThat(errorOp).isPresent();
        assertThat(errorOp.get().failedNodeIds()).isEqualTo("ScriptTask_1");
        assertThat(errorOp.get().errorMessages()).isEqualTo("null");

        op.accept(processInstance);
        processInstance.errors().ifPresent(e -> e.retrigger());

        WorkItem workItem = processInstance.workItems(SecurityPolicy.of(new StaticIdentityProvider("john"))).get(0);
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameters().get("ActorId")).isEqualTo("john");
        processInstance.completeWorkItem(workItem.getId(), null, SecurityPolicy.of(new StaticIdentityProvider("john")));
        assertThat(processInstance.status()).isEqualTo(STATE_COMPLETED);
    }

    private class CacheProcessInstancesFactory extends AbstractProcessInstancesFactory {

        CacheProcessInstancesFactory(RemoteCacheManager cacheManager) {
            super(cacheManager);
        }

        @Override
        public String proto() {
            return null;
        }

        @Override
        public List<?> marshallers() {
            return Collections.emptyList();
        }
    }
}
