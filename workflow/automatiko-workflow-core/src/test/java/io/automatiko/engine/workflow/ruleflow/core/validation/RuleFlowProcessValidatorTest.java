
package io.automatiko.engine.workflow.ruleflow.core.validation;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.automatiko.engine.workflow.base.core.validation.ProcessValidationError;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.CompositeNode;
import io.automatiko.engine.workflow.process.core.node.DynamicNode;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatiko.engine.workflow.process.executable.core.validation.ExecutableProcessValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RuleFlowProcessValidatorTest {

	private ExecutableProcessValidator validator;

	private List<ProcessValidationError> errors;

	private ExecutableProcess process = mock(ExecutableProcess.class);

	private Node node = mock(Node.class);

	@BeforeEach
	public void setUp() {
		errors = new ArrayList<>();
		validator = ExecutableProcessValidator.getInstance();
	}

	@Test
	void testAddErrorMessage() {
		when(node.getName()).thenReturn("nodeName");
		when(node.getId()).thenReturn(Long.MAX_VALUE);
		validator.addErrorMessage(process, node, errors, "any message");
		assertEquals(1, errors.size());
		assertEquals("Node 'nodeName' [" + Long.MAX_VALUE + "] any message", errors.get(0).getMessage());
	}

	@Test
	void testDynamicNodeValidationInNotDynamicProcess() {
		ExecutableProcess process = new ExecutableProcess();
		process.setId("org.company.core.process");
		process.setName("Dynamic Node Process");
		process.setPackageName("org.mycomp.myprocess");
		process.setDynamic(false);

		DynamicNode dynamicNode = new DynamicNode();
		dynamicNode.setName("MyDynamicNode");
		dynamicNode.setId(1);
		dynamicNode.setAutoComplete(false);
		// empty completion expression to trigger validation error
		process.addNode(dynamicNode);

		ProcessValidationError[] errors = validator.validateProcess(process);
		assertNotNull(errors);
		// in non-dynamic processes all check should be triggered
		// they should also include process level checks (start node, end node etc)
		assertEquals(6, errors.length);
		assertEquals("Process has no start node.", errors[0].getMessage());
		assertEquals("Process has no end node.", errors[1].getMessage());
		assertEquals("Node 'MyDynamicNode' [1] Dynamic has no incoming connection", errors[2].getMessage());
		assertEquals("Node 'MyDynamicNode' [1] Dynamic has no outgoing connection", errors[3].getMessage());
		assertEquals("Node 'MyDynamicNode' [1] Dynamic has no completion condition set", errors[4].getMessage());
		assertEquals("Node 'MyDynamicNode' [1] Has no connection to the start node.", errors[5].getMessage());
	}

	@Test
	void testDynamicNodeValidationInDynamicProcess() {
		ExecutableProcess process = new ExecutableProcess();
		process.setId("org.company.core.process");
		process.setName("Dynamic Node Process");
		process.setPackageName("org.mycomp.myprocess");
		process.setDynamic(true);

		DynamicNode dynamicNode = new DynamicNode();
		dynamicNode.setName("MyDynamicNode");
		dynamicNode.setId(1);
		dynamicNode.setAutoComplete(false);
		dynamicNode.setCompletionExpression(kcontext -> true);
		process.addNode(dynamicNode);

		ProcessValidationError[] errors = validator.validateProcess(process);
		assertNotNull(errors);
		// if dynamic process no longer triggering incoming / outgoing connection errors
		// for dynamic nodes
		assertEquals(0, errors.length);

		// empty completion expression to trigger validation error
		process.removeNode(dynamicNode);
		DynamicNode dynamicNode2 = new DynamicNode();
		dynamicNode2.setName("MyDynamicNode");
		dynamicNode2.setId(1);
		dynamicNode2.setAutoComplete(false);
		process.addNode(dynamicNode2);

		ProcessValidationError[] errors2 = validator.validateProcess(process);
		assertNotNull(errors2);
		// autocomplete set to false and empty completion condition triggers error
		assertEquals(1, errors2.length);
		assertEquals("Node 'MyDynamicNode' [1] Dynamic has no completion condition set", errors2[0].getMessage());
	}

	@Test
	void testEmptyPackageName() {
		ExecutableProcess process = new ExecutableProcess();
		process.setId("org.company.core.process");
		process.setName("Empty Package Name Process");
		process.setPackageName("");
		process.setDynamic(true);

		ProcessValidationError[] errors = validator.validateProcess(process);
		assertNotNull(errors);
		assertEquals(0, errors.length);
	}

	@Test
	void testNoPackageName() {
		ExecutableProcess process = new ExecutableProcess();
		process.setId("org.company.core.process");
		process.setName("No Package Name Process");
		process.setDynamic(true);

		ProcessValidationError[] errors = validator.validateProcess(process);
		assertNotNull(errors);
		assertEquals(0, errors.length);
	}

	@Test
	void testCompositeNodeNoStart() {
		ExecutableProcess process = new ExecutableProcess();
		process.setId("org.company.core.process.process");
		process.setName("Process");

		StartNode startNode = new StartNode();
		startNode.setName("Start");
		startNode.setId(1);
		process.addNode(startNode);
		EndNode endNode = new EndNode();
		endNode.setName("EndNode");
		endNode.setId(2);
		process.addNode(endNode);
		CompositeNode compositeNode = new CompositeNode();
		compositeNode.setName("CompositeNode");
		compositeNode.setId(3);
		process.addNode(compositeNode);
		new io.automatiko.engine.workflow.process.core.impl.ConnectionImpl(startNode, Node.CONNECTION_DEFAULT_TYPE,
				compositeNode, Node.CONNECTION_DEFAULT_TYPE);
		new io.automatiko.engine.workflow.process.core.impl.ConnectionImpl(compositeNode, Node.CONNECTION_DEFAULT_TYPE,
				endNode, Node.CONNECTION_DEFAULT_TYPE);

		ProcessValidationError[] errors = validator.validateProcess(process);
		assertNotNull(errors);
		assertEquals(1, errors.length);
		assertEquals("Node 'CompositeNode' [3] Composite has no start node defined.", errors[0].getMessage());
	}
}
