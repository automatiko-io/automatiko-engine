
package io.automatiko.engine.workflow.workflow.instance.node;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatiko.engine.workflow.process.executable.instance.ExecutableProcessInstance;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceFactoryRegistry;
import io.automatiko.engine.workflow.test.util.AbstractBaseTest;

public class EndNodeInstanceTest extends AbstractBaseTest {

	public void addLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Test
	public void testEndNode() {

		MockNode mockNode = new MockNode();
		MockNodeInstanceFactory factory = new MockNodeInstanceFactory(new MockNodeInstance(mockNode));
		NodeInstanceFactoryRegistry.getInstance().register(mockNode.getClass(), factory);

		ExecutableProcess process = new ExecutableProcess();
		process.setId("test");

		InternalProcessRuntime processRuntime = createProcessRuntime(process);

		Node endNode = new EndNode();
		endNode.setId(1);
		endNode.setName("end node");

		mockNode.setId(2);
		new ConnectionImpl(mockNode, Node.CONNECTION_DEFAULT_TYPE, endNode, Node.CONNECTION_DEFAULT_TYPE);

		process.addNode(mockNode);
		process.addNode(endNode);

		ExecutableProcessInstance processInstance = new ExecutableProcessInstance();
		processInstance.setId("1223");
		processInstance.setState(ProcessInstance.STATE_ACTIVE);
		processInstance.setProcess(process);
		processInstance.setProcessRuntime(processRuntime);

		MockNodeInstance mockNodeInstance = (MockNodeInstance) processInstance.getNodeInstance(mockNode);

		mockNodeInstance.triggerCompleted();
		assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
	}
}
