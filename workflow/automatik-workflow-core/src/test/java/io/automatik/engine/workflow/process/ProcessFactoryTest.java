
package io.automatik.engine.workflow.process;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.automatik.engine.workflow.process.executable.core.ExecutableProcessFactory;
import io.automatik.engine.workflow.test.util.AbstractBaseTest;

public class ProcessFactoryTest extends AbstractBaseTest {

	public void addLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Test
	public void testProcessFactory() throws Exception {
		ExecutableProcessFactory factory = ExecutableProcessFactory.createProcess("org.company.core.process");
		factory
				// header
				.name("My process").packageName("org.company")
				// nodes
				.startNode(1).name("Start").done().actionNode(2).name("Action")
				.action("java", "System.out.println(\"Action\");").done().endNode(3).name("End").done()
				// connections
				.connection(1, 2).connection(2, 3);
		factory.validate().getProcess();
	}
}
