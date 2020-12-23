package io.automatiko.engine.workflow.process.instance;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.workflow.base.instance.LightProcessRuntime;
import io.automatiko.engine.workflow.base.instance.LightProcessRuntimeContext;
import io.automatiko.engine.workflow.base.instance.LightProcessRuntimeServiceProvider;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcessFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LightProcessRuntimeTest {

	static class MyProcess {
		String result;
		ExecutableProcess process;
		{
			ExecutableProcessFactory factory = ExecutableProcessFactory.createProcess("org.kie.api2.MyProcessUnit");
			factory
					// Header
					.name("HelloWorldProcess").version("1.0").packageName("org.jbpm")
					// Nodes
					.startNode(1).name("Start").done().actionNode(2).name("Action").action(ctx -> {
						result = "Hello!";
					}).done().endNode(3).name("End").done()
					// Connections
					.connection(1, 2).connection(2, 3);
			process = factory.validate().getProcess();
		}
	}

	@Test
	public void testInstantiation() {
		LightProcessRuntimeServiceProvider services = new LightProcessRuntimeServiceProvider();

		MyProcess myProcess = new MyProcess();
		LightProcessRuntimeContext rtc = new LightProcessRuntimeContext(Collections.singletonList(myProcess.process));

		LightProcessRuntime rt = new LightProcessRuntime(rtc, services);

		rt.startProcess(myProcess.process.getId());

		assertEquals("Hello!", myProcess.result);

	}

}
