
package io.automatik.engine.workflow.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.workflow.base.core.ParameterDefinition;
import io.automatik.engine.workflow.base.core.Work;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.datatype.impl.type.IntegerDataType;
import io.automatik.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatik.engine.workflow.base.core.datatype.impl.type.StringDataType;
import io.automatik.engine.workflow.base.core.impl.ParameterDefinitionImpl;
import io.automatik.engine.workflow.base.core.impl.WorkImpl;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.impl.demo.DoNothingWorkItemHandler;
import io.automatik.engine.workflow.base.instance.impl.demo.MockDataWorkItemHandler;
import io.automatik.engine.workflow.base.instance.impl.workitem.WorkItemHandlerNotFoundException;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatik.engine.workflow.process.core.node.EndNode;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatik.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatik.engine.workflow.process.test.Person;
import io.automatik.engine.workflow.test.util.AbstractBaseTest;

public class WorkItemTest extends AbstractBaseTest {

	public void addLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Test
	public void testReachNonRegisteredWorkItemHandler() {
		String processId = "org.company.actions";
		String workName = "Unnexistent Task";
		ExecutableProcess process = getWorkItemProcess(processId, workName);
		InternalProcessRuntime ksession = createProcessRuntime(process);

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("UserName", "John Doe");
		parameters.put("Person", new Person("John Doe"));

		ProcessInstance processInstance = null;
		try {
			processInstance = ksession.startProcess("org.company.actions", parameters);
			fail("should fail if WorkItemHandler for" + workName + "is not registered");
		} catch (Throwable e) {

		}
		assertEquals(ProcessInstance.STATE_ERROR, processInstance.getState());
	}

	@Test
	public void testCancelNonRegisteredWorkItemHandler() {
		String processId = "org.company.actions";
		String workName = "Unnexistent Task";
		ExecutableProcess process = getWorkItemProcess(processId, workName);
		InternalProcessRuntime ksession = createProcessRuntime(process);

		ksession.getWorkItemManager().registerWorkItemHandler(workName, new DoNothingWorkItemHandler());

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("UserName", "John Doe");
		parameters.put("Person", new Person("John Doe"));

		ProcessInstance processInstance = ksession.startProcess("org.company.actions", parameters);
		String processInstanceId = processInstance.getId();
		assertEquals(ProcessInstance.STATE_ACTIVE, processInstance.getState());
		ksession.getWorkItemManager().registerWorkItemHandler(workName, null);

		try {
			ksession.abortProcessInstance(processInstanceId);
			fail("should fail if WorkItemHandler for" + workName + "is not registered");
		} catch (WorkItemHandlerNotFoundException wihnfe) {

		}

		assertEquals(ProcessInstance.STATE_ABORTED, processInstance.getState());
	}

	@Test
	public void testMockDataWorkItemHandler() {
		String processId = "org.company.actions";
		String workName = "Unnexistent Task";
		ExecutableProcess process = getWorkItemProcess(processId, workName);
		InternalProcessRuntime ksession = createProcessRuntime(process);

		Map<String, Object> output = new HashMap<String, Object>();
		output.put("Result", "test");

		ksession.getWorkItemManager().registerWorkItemHandler(workName, new MockDataWorkItemHandler(output));

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("UserName", "John Doe");
		parameters.put("Person", new Person("John Doe"));

		ProcessInstance processInstance = ksession.startProcess("org.company.actions", parameters);

		Object numberVariable = ((WorkflowProcessInstance) processInstance).getVariable("MyObject");
		assertNotNull(numberVariable);
		assertEquals("test", numberVariable);

		assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
	}

	@Test
	public void testMockDataWorkItemHandlerCustomFunction() {
		String processId = "org.company.actions";
		String workName = "Unnexistent Task";
		ExecutableProcess process = getWorkItemProcess(processId, workName);
		InternalProcessRuntime ksession = createProcessRuntime(process);

		ksession.getWorkItemManager().registerWorkItemHandler(workName, new MockDataWorkItemHandler((input) -> {
			Map<String, Object> output = new HashMap<String, Object>();
			if ("John Doe".equals(input.get("Comment"))) {
				output.put("Result", "one");
			} else {
				output.put("Result", "two");

			}
			return output;
		}));

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("UserName", "John Doe");
		parameters.put("Person", new Person("John Doe"));

		ProcessInstance processInstance = ksession.startProcess("org.company.actions", parameters);

		Object numberVariable = ((WorkflowProcessInstance) processInstance).getVariable("MyObject");
		assertNotNull(numberVariable);
		assertEquals("one", numberVariable);

		assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());

		parameters = new HashMap<String, Object>();
		parameters.put("UserName", "John Doe");
		parameters.put("Person", new Person("John Deen"));

		processInstance = ksession.startProcess("org.company.actions", parameters);

		numberVariable = ((WorkflowProcessInstance) processInstance).getVariable("MyObject");
		assertNotNull(numberVariable);
		assertEquals("two", numberVariable);

		assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
	}

	private ExecutableProcess getWorkItemProcess(String processId, String workName) {
		ExecutableProcess process = new ExecutableProcess();
		process.setId(processId);

		List<Variable> variables = new ArrayList<Variable>();
		Variable variable = new Variable();
		variable.setName("UserName");
		variable.setType(new StringDataType());
		variables.add(variable);
		variable = new Variable();
		variable.setName("Person");
		variable.setType(new ObjectDataType(Person.class.getName()));
		variables.add(variable);
		variable = new Variable();
		variable.setName("MyObject");
		variable.setType(new ObjectDataType());
		variables.add(variable);
		variable = new Variable();
		variable.setName("Number");
		variable.setType(new IntegerDataType());
		variables.add(variable);
		process.getVariableScope().setVariables(variables);

		StartNode startNode = new StartNode();
		startNode.setName("Start");
		startNode.setId(1);

		WorkItemNode workItemNode = new WorkItemNode();
		workItemNode.setName("workItemNode");
		workItemNode.setId(2);
		workItemNode.addInMapping("Comment", "Person.name");
		workItemNode.addInMapping("Attachment", "MyObject");
		workItemNode.addOutMapping("Result", "MyObject");
		workItemNode.addOutMapping("Result.length()", "Number");
		Work work = new WorkImpl();
		work.setName(workName);
		Set<ParameterDefinition> parameterDefinitions = new HashSet<ParameterDefinition>();
		ParameterDefinition parameterDefinition = new ParameterDefinitionImpl("ActorId", new StringDataType());
		parameterDefinitions.add(parameterDefinition);
		parameterDefinition = new ParameterDefinitionImpl("Content", new StringDataType());
		parameterDefinitions.add(parameterDefinition);
		parameterDefinition = new ParameterDefinitionImpl("Comment", new StringDataType());
		parameterDefinitions.add(parameterDefinition);
		work.setParameterDefinitions(parameterDefinitions);
		work.setParameter("ActorId", "#{UserName}");
		work.setParameter("Content", "#{Person.name}");
		workItemNode.setWork(work);

		EndNode endNode = new EndNode();
		endNode.setName("End");
		endNode.setId(3);

		connect(startNode, workItemNode);
		connect(workItemNode, endNode);

		process.addNode(startNode);
		process.addNode(workItemNode);
		process.addNode(endNode);

		return process;
	}

	private void connect(Node sourceNode, Node targetNode) {
		new ConnectionImpl(sourceNode, Node.CONNECTION_DEFAULT_TYPE, targetNode, Node.CONNECTION_DEFAULT_TYPE);
	}

}
