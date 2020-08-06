
package io.automatik.engine.addons.persistence.infinispan;

import static io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static io.automatik.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED;
import static io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import io.automatik.engine.addons.persistence.AbstractProcessInstancesFactory;
import io.automatik.engine.api.auth.SecurityPolicy;
import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.runtime.process.ProcessContext;
import io.automatik.engine.api.workflow.ProcessError;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceNotFoundException;
import io.automatik.engine.api.workflow.WorkItem;
import io.automatik.engine.services.identity.StaticIdentityProvider;
import io.automatik.engine.services.io.ClassPathResource;
import io.automatik.engine.workflow.base.instance.impl.Action;
import io.automatik.engine.workflow.bpmn2.BpmnProcess;
import io.automatik.engine.workflow.bpmn2.BpmnVariables;
import io.automatik.engine.workflow.process.core.ProcessAction;
import io.automatik.engine.workflow.process.core.WorkflowProcess;
import io.automatik.engine.workflow.process.core.node.ActionNode;

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
			processInstance.error().orElseThrow(() -> new IllegalStateException("Process instance not in error"))
					.retrigger();
		});
	}

	@Test
	public void testBasicFlowWithErrorAndSkip() {

		testBasicFlowWithError((processInstance) -> {
			processInstance.updateVariables(BpmnVariables.create(Collections.singletonMap("s", "test")));
			processInstance.error().orElseThrow(() -> new IllegalStateException("Process instance not in error"))
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

		Optional<ProcessError> errorOp = processInstance.error();
		assertThat(errorOp).isPresent();
		assertThat(errorOp.get().failedNodeId()).isEqualTo("ScriptTask_1");
		assertThat(errorOp.get().errorMessage()).isNotNull().contains("java.lang.NullPointerException - null");

		op.accept(processInstance);

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
