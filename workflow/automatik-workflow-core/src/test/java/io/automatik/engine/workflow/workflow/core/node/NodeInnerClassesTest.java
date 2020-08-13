
package io.automatik.engine.workflow.workflow.core.node;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.process.core.node.CompositeNode;
import io.automatik.engine.workflow.process.core.node.ForEachNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatik.engine.workflow.process.test.Person;
import io.automatik.engine.workflow.process.test.TestProcessEventListener;
import io.automatik.engine.workflow.test.util.AbstractBaseTest;

public class NodeInnerClassesTest extends AbstractBaseTest {

	@Override
	public void addLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Test
	public void testNodeReading() {

		ExecutableProcess process = new ExecutableProcess();
		process.setId("org.company.core.process.event");
		process.setName("Event Process");

		List<Variable> variables = new ArrayList<Variable>();
		Variable variable = new Variable();
		variable.setName("event");
		ObjectDataType personDataType = new ObjectDataType(Person.class);
		variable.setType(personDataType);
		variables.add(variable);
		process.getVariableScope().setVariables(variables);

		process.setDynamic(true);
		CompositeNode compositeNode = new CompositeNode();
		compositeNode.setName("CompositeNode");
		compositeNode.setId(2);

		ForEachNode forEachNode = new ForEachNode();
		ForEachNode.ForEachSplitNode split = new ForEachNode.ForEachSplitNode();
		split.setName("ForEachSplit");
		split.setMetaData("hidden", true);
		split.setMetaData("UniqueId", forEachNode.getMetaData("Uniqueid") + ":foreach:split");
		forEachNode.internalAddNode(split);
		forEachNode.linkIncomingConnections(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE,
				new CompositeNode.NodeAndType(split,
						io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE));

		process.addNode(forEachNode);
		InternalProcessRuntime ksession = createProcessRuntime(process);
		TestProcessEventListener procEventListener = new TestProcessEventListener();
		ksession.addEventListener(procEventListener);

		ProcessInstance processInstance = ksession.startProcess("org.company.core.process.event");
		assertNotNull(processInstance);
	}

}
