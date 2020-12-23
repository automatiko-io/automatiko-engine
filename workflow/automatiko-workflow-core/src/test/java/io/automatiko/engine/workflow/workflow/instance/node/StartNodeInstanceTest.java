
package io.automatiko.engine.workflow.workflow.instance.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatiko.engine.workflow.process.executable.instance.ExecutableProcessInstance;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceFactoryRegistry;
import io.automatiko.engine.workflow.test.util.AbstractBaseTest;

public class StartNodeInstanceTest extends AbstractBaseTest {

	public void addLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Test
	public void testStartNode() {

		MockNode mockNode = new MockNode();
		MockNodeInstanceFactory mockNodeFactory = new MockNodeInstanceFactory(new MockNodeInstance(mockNode));
		NodeInstanceFactoryRegistry.getInstance().register(mockNode.getClass(), mockNodeFactory);

		ExecutableProcess process = new ExecutableProcess();
		process.setId("test");

		InternalProcessRuntime processRuntime = createProcessRuntime(process);

		StartNode startNode = new StartNode();
		startNode.setId(1);
		startNode.setName("start node");

		mockNode.setId(2);
		new ConnectionImpl(startNode, io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, mockNode,
				io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);

		process.addNode(startNode);
		process.addNode(mockNode);

		ExecutableProcessInstance processInstance = new ExecutableProcessInstance();
		processInstance.setProcess(process);
		processInstance.setProcessRuntime(processRuntime);

		assertEquals(ProcessInstance.STATE_PENDING, processInstance.getState());
		processInstance.start();
		assertEquals(ProcessInstance.STATE_ACTIVE, processInstance.getState());

		MockNodeInstance mockNodeInstance = mockNodeFactory.getMockNodeInstance();
		List<NodeInstance> triggeredBy = mockNodeInstance.getTriggers()
				.get(io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
		assertNotNull(triggeredBy);
		assertEquals(1, triggeredBy.size());
		assertSame(startNode.getId(), triggeredBy.get(0).getNodeId());
	}
}
