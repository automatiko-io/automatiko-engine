
package org.kie.kogito.process.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatik.engine.api.Model;
import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.ProcessRuntime;
import io.automatik.engine.api.workflow.MutableProcessInstances;
import io.automatik.engine.workflow.AbstractProcess;
import io.automatik.engine.workflow.AbstractProcessInstance;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.ProcessInstanceManager;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

public class AbstractProcessInstanceTest {

	@Test
	public void testCreteProcessInstance() {
		AbstractProcess process = mock(AbstractProcess.class);
		when(process.process()).thenReturn(mock(Process.class));
		when(process.instances()).thenReturn(mock(MutableProcessInstances.class));
		InternalProcessRuntime pr = mock(InternalProcessRuntime.class);
		WorkflowProcessInstanceImpl wpi = mock(WorkflowProcessInstanceImpl.class);
		when(pr.createProcessInstance(any(), any(), any())).thenReturn(wpi);
		ProcessInstanceManager pim = mock(ProcessInstanceManager.class);
		when(pr.getProcessInstanceManager()).thenReturn(pim);
		AbstractProcessInstance pi = new TestProcessInstance(process, new TestModel(), pr);

		assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_PENDING);
		assertThat(pi.id()).isNull();
		assertThat(pi.businessKey()).isNull();

		verify(pim, never()).addProcessInstance(any(), any());
	}

	static class TestProcessInstance extends AbstractProcessInstance<TestModel> {

		public TestProcessInstance(AbstractProcess process, TestModel variables, ProcessRuntime rt) {
			super(process, variables, rt);
		}
	}

	static class TestModel implements Model {

		@Override
		public Map<String, Object> toMap() {
			return null;
		}

		@Override
		public void fromMap(Map<String, Object> params) {

		}
	}
}
