
package io.automatik.engine.addons.persistence.filesystem;

import static io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static io.automatik.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.automatik.engine.addons.persistence.AbstractProcessInstancesFactory;
import io.automatik.engine.api.auth.SecurityPolicy;
import io.automatik.engine.api.uow.UnitOfWork;
import io.automatik.engine.api.uow.UnitOfWorkManager;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessConfig;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.WorkItem;
import io.automatik.engine.services.identity.StaticIdentityProvider;
import io.automatik.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatik.engine.services.uow.DefaultUnitOfWorkManager;
import io.automatik.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatik.engine.workflow.DefaultWorkItemHandlerConfig;
import io.automatik.engine.workflow.StaticProcessConfig;
import io.automatik.engine.workflow.base.core.resources.ClassPathResource;
import io.automatik.engine.workflow.bpmn2.BpmnProcess;
import io.automatik.engine.workflow.bpmn2.BpmnVariables;

public class FileSystemProcessInstancesTest {

	private SecurityPolicy securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));

	@Test
	public void testBasicFlow() {

		BpmnProcess process = (BpmnProcess) BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2")).get(0);
		process.setProcessInstancesFactory(new FileSystemProcessInstancesFactory());
		process.configure();

		ProcessInstance<BpmnVariables> processInstance = process
				.createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

		processInstance.start();

		assertThat(processInstance.status()).isEqualTo(STATE_ACTIVE);
		assertThat(processInstance.description()).isEqualTo("User Task");

		assertThat(process.instances().values()).hasSize(1);

		FileSystemProcessInstances fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
		verify(fileSystemBasedStorage, times(1)).create(any(), any());
		verify(fileSystemBasedStorage, times(1)).setMetadata(any(), eq(FileSystemProcessInstances.PI_DESCRIPTION),
				eq("User Task"));
		verify(fileSystemBasedStorage, times(1)).setMetadata(any(), eq(FileSystemProcessInstances.PI_STATUS), eq("1"));

		String testVar = (String) processInstance.variables().get("test");
		assertThat(testVar).isEqualTo("test");

		assertThat(processInstance.description()).isEqualTo("User Task");

		WorkItem workItem = processInstance.workItems(securityPolicy).get(0);
		assertThat(workItem).isNotNull();
		assertThat(workItem.getParameters().get("ActorId")).isEqualTo("john");
		processInstance.completeWorkItem(workItem.getId(), null, securityPolicy);
		assertThat(processInstance.status()).isEqualTo(STATE_COMPLETED);

		fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
		verify(fileSystemBasedStorage, times(2)).remove(any());
	}

	@Test
	public void testBasicFlowWithStartFrom() {

		BpmnProcess process = (BpmnProcess) BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2")).get(0);
		process.setProcessInstancesFactory(new FileSystemProcessInstancesFactory());
		process.configure();

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
		verify(fileSystemBasedStorage, times(2)).remove(any());
	}

	@Test
	public void testBasicFlowControlledByUnitOfWork() {

		UnitOfWorkManager uowManager = new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory());
		ProcessConfig config = new StaticProcessConfig(new DefaultWorkItemHandlerConfig(),
				new DefaultProcessEventListenerConfig(), uowManager, null);
		BpmnProcess process = (BpmnProcess) BpmnProcess.from(config, new ClassPathResource("BPMN2-UserTask.bpmn2"))
				.get(0);
		process.setProcessInstancesFactory(new FileSystemProcessInstancesFactory());
		process.configure();

		ProcessInstance<BpmnVariables> processInstance = process
				.createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

		UnitOfWork uow = uowManager.newUnitOfWork();
		uow.start();

		processInstance.start();

		uow.end();
		assertThat(processInstance.status()).isEqualTo(STATE_ACTIVE);
		assertThat(processInstance.description()).isEqualTo("User Task");

		assertThat(process.instances().values()).hasSize(1);

		FileSystemProcessInstances fileSystemBasedStorage = (FileSystemProcessInstances) process.instances();
		verify(fileSystemBasedStorage, times(1)).create(any(), any());
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
		verify(fileSystemBasedStorage, times(1)).remove(any());
	}

	private class FileSystemProcessInstancesFactory extends AbstractProcessInstancesFactory {

		@Override
		public FileSystemProcessInstances createProcessInstances(Process<?> process) {
			FileSystemProcessInstances instances = spy(super.createProcessInstances(process));
			return instances;
		}

		@Override
		public String path() {
			return "target";
		}
	}
}
