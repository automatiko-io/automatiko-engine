
package io.automatiko.engine.workflow.workflow.instance.node;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceFactoryRegistry;
import io.automatiko.engine.workflow.test.util.AbstractBaseTest;

public class ProcessNodeInstanceFactoryTest extends AbstractBaseTest {

	public void addLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Test
	public void testDefaultEntries() throws Exception {
		Node node = new ActionNode();
		assertNotNull(NodeInstanceFactoryRegistry.getInstance().getProcessNodeInstanceFactory(node).getClass());
	}

}
