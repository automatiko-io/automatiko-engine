
package io.automatiko.engine.workflow.ruleflow.instance;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatiko.engine.workflow.test.util.AbstractBaseTest;

public class RuleFlowProcessInstanceTest extends AbstractBaseTest {

	private static String PROCESS_ID = "process.test";

	public void addLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Test
	public void testStartProcessThrowException() throws Exception {
		ExecutableProcess process = new ExecutableProcess();
		process.setId(PROCESS_ID);
		process.setName("test");
		process.setPackageName("org.mycomp.myprocess");

		InternalProcessRuntime workingMemory = createProcessRuntime(process);
		assertThatThrownBy(() -> workingMemory.startProcess(PROCESS_ID)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testStartProcessDynamic() throws Exception {
		ExecutableProcess process = new ExecutableProcess();
		process.setId(PROCESS_ID);
		process.setName("test");
		process.setPackageName("org.mycomp.myprocess");
		process.setDynamic(true);

		InternalProcessRuntime workingMemory = createProcessRuntime(process);
		ProcessInstance instance = workingMemory.startProcess(PROCESS_ID);
		assertNotNull(instance);
	}

}
