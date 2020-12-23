
package io.automatiko.engine.workflow.compiler.canonical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.compiler.canonical.ModelMetaData;
import io.automatiko.engine.workflow.compiler.canonical.ProcessMetaData;
import io.automatiko.engine.workflow.compiler.canonical.ProcessToExecModelGenerator;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcessFactory;

public class ProcessToExecModelGeneratorTest {

	private static final Logger logger = LoggerFactory.getLogger(ProcessToExecModelGeneratorTest.class);

	@Test
	public void testScriptAndWorkItemGeneration() {

		ExecutableProcessFactory factory = ExecutableProcessFactory.createProcess("demo.orders");
		factory.variable("order", new ObjectDataType(Integer.class))
				.variable("approver", new ObjectDataType(String.class)).name("orders").packageName("com.myspace.demo")
				.dynamic(false).version("1.0").workItemNode(1).name("Log").workName("Log").done().actionNode(2)
				.name("Dump order").action("java", "System.out.println(\"Order has been created \" + order);").done()
				.endNode(3).name("end").terminate(false).done().startNode(4).name("start").done().connection(2, 1)
				.connection(4, 2).connection(1, 3);

		WorkflowProcess process = factory.validate().getProcess();

		ProcessMetaData processMetadata = ProcessToExecModelGenerator.INSTANCE.generate(process);
		assertNotNull(processMetadata, "Dumper should return non null class for process");

		logger.debug(processMetadata.getGeneratedClassModel().toString());

		assertEquals("orders_1_0", processMetadata.getExtractedProcessId());
		assertEquals("demo.orders", processMetadata.getProcessId());
		assertEquals("orders", processMetadata.getProcessName());
		assertEquals("_1_0", processMetadata.getProcessVersion());
		assertEquals("com.myspace.demo.Orders_1_0Process", processMetadata.getProcessClassName());
		assertNotNull(processMetadata.getGeneratedClassModel());
		assertEquals(1, processMetadata.getWorkItems().size());
	}

	@Test
	public void testScriptAndWorkItemModelGeneration() {

		ExecutableProcessFactory factory = ExecutableProcessFactory.createProcess("demo.orders");
		factory.variable("order", new ObjectDataType(Integer.class))
				.variable("order", new ObjectDataType(List.class, "java.util.List<String>"))
				.variable("approver", new ObjectDataType(String.class)).name("orders").packageName("com.myspace.demo")
				.dynamic(false).version("1.0").workItemNode(1).name("Log").workName("Log").done().actionNode(2)
				.name("Dump order").action("java", "System.out.println(\"Order has been created \" + order);").done()
				.endNode(3).name("end").terminate(false).done().startNode(4).name("start").done().connection(2, 1)
				.connection(4, 2).connection(1, 3);

		Process process = factory.validate().getProcess();

		ModelMetaData modelMetadata = ProcessToExecModelGenerator.INSTANCE.generateModel((WorkflowProcess) process);
		assertNotNull(modelMetadata, "Dumper should return non null class for process");

		logger.debug(modelMetadata.generate());
		assertEquals("com.myspace.demo.Orders_1_0Model", modelMetadata.getModelClassName());
	}

}
