package io.automatiko.engine.decision.dmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.kie.dmn.api.core.DMNRuntime;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;
import io.automatiko.engine.decision.dmn.DmnDecisionModel;
import io.automatiko.engine.decision.dmn.DmnRuntimeProvider;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.BooleanDataType;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.base.instance.ProcessRuntimeImpl;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.RuleSetNode;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

public class DmnDecisionInProcessTest {
	@Test
	public void testDmn() {
		String namespace = "https://kiegroup.org/dmn/_52CEF9FD-9943-4A89-96D5-6F66810CA4C1";
		String modelName = "PersonDecisions";
		String decisionName = "isAdult";

		ExecutableProcess process = createProcess(namespace, modelName, decisionName);

		Map<String, Object> parameters = new HashMap<>();
		Person person = new Person("John", 25);

		parameters.put("person", person);
		parameters.put("isAdult", false);

		ProcessRuntime runtime = createProcessRuntime(process);

		ProcessInstance pi = runtime.startProcess("process", parameters);
		assertEquals(ProcessInstance.STATE_COMPLETED, pi.getState());

		boolean result = (boolean) pi.getVariables().get("isAdult");

		assertEquals(true, result);
	}

	@Test
	public void testModelNotFound() {
		String namespace = "wrong-namespace";
		String modelName = "wrong-name";
		String decisionName = "isAdult";

		IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
				() -> createProcess(namespace, modelName, decisionName));
		assertTrue(illegalStateException.getMessage().contains(namespace));
		assertTrue(illegalStateException.getMessage().contains(modelName));
	}

	private ExecutableProcess createProcess(String namespace, String modelName, String decisionName) {
		DMNRuntime dmnRuntime = DmnRuntimeProvider.fromClassPath("PersonDecisions.dmn");
		DmnDecisionModel dmnDecisionModel = new DmnDecisionModel(dmnRuntime, namespace, modelName);

		ExecutableProcess process = new ExecutableProcess();
		process.setId("process");
		process.setName("Process");

		List<Variable> variables = new ArrayList<Variable>();
		Variable variable1 = new Variable();
		variable1.setName("person");
		variable1.setType(new ObjectDataType(Person.class));
		variables.add(variable1);

		Variable variable2 = new Variable();
		variable2.setName("isAdult");
		variable2.setType(new BooleanDataType());
		variables.add(variable2);
		process.getVariableScope().setVariables(variables);

		StartNode startNode = new StartNode();
		startNode.setName("Start");
		startNode.setId(1);

		RuleSetNode ruleSetNode = new RuleSetNode();
		ruleSetNode.setName("RuleSetNode");
		ruleSetNode.setId(2);
		ruleSetNode.setRuleType(RuleSetNode.RuleType.decision(namespace, modelName, null));
		ruleSetNode.setLanguage(RuleSetNode.DMN_LANG);
		ruleSetNode.setDecisionModel(() -> dmnDecisionModel);
		ruleSetNode.addInMapping("Person", "person");
		ruleSetNode.addOutMapping("isAdult", "isAdult");

		EndNode endNode = new EndNode();
		endNode.setName("End");
		endNode.setId(3);

		connect(startNode, ruleSetNode);
		connect(ruleSetNode, endNode);

		process.addNode(startNode);
		process.addNode(ruleSetNode);
		process.addNode(endNode);
		return process;
	}

	protected void connect(Node sourceNode, Node targetNode) {
		new ConnectionImpl(sourceNode, Node.CONNECTION_DEFAULT_TYPE, targetNode, Node.CONNECTION_DEFAULT_TYPE);
	}

	protected InternalProcessRuntime createProcessRuntime(Process... process) {
		Map<String, Process> mappedProcesses = Stream.of(process).collect(Collectors.toMap(Process::getId, p -> p));

		InternalProcessRuntime processRuntime = new ProcessRuntimeImpl(mappedProcesses);
		return processRuntime;
	}
}
