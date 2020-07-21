
package io.automatik.engine.workflow.process;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatik.engine.workflow.process.core.node.EndNode;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.core.node.SubProcessNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatik.engine.workflow.test.util.AbstractBaseTest;

public class SubProcessTest extends AbstractBaseTest {

	public void addLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Test
	public void testNonExistentSubProcess() {
		String nonExistentSubProcessName = "nonexistent.process";
		ExecutableProcess process = new ExecutableProcess();
		process.setId("org.company.core.process.process");
		process.setName("Process");
		StartNode startNode = new StartNode();
		startNode.setName("Start");
		startNode.setId(1);
		SubProcessNode subProcessNode = new SubProcessNode();
		subProcessNode.setName("SubProcessNode");
		subProcessNode.setId(2);
		subProcessNode.setProcessId(nonExistentSubProcessName);
		EndNode endNode = new EndNode();
		endNode.setName("End");
		endNode.setId(3);

		connect(startNode, subProcessNode);
		connect(subProcessNode, endNode);

		process.addNode(startNode);
		process.addNode(subProcessNode);
		process.addNode(endNode);

		InternalProcessRuntime ksession = createProcessRuntime(process);

		ProcessInstance pi = ksession.startProcess("org.company.core.process.process");
		assertEquals(ProcessInstance.STATE_ERROR, pi.getState());
	}

	private void connect(Node sourceNode, Node targetNode) {
		new ConnectionImpl(sourceNode, Node.CONNECTION_DEFAULT_TYPE, targetNode, Node.CONNECTION_DEFAULT_TYPE);
	}

}
