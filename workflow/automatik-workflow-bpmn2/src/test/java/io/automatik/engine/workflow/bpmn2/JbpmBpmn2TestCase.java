
package io.automatik.engine.workflow.bpmn2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.event.process.ProcessEventListener;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.NodeInstanceContainer;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.WorkItemHandler;
import io.automatik.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatik.engine.api.workflow.ProcessConfig;
import io.automatik.engine.services.io.ClassPathResource;
import io.automatik.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatik.engine.services.uow.DefaultUnitOfWorkManager;
import io.automatik.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatik.engine.workflow.DefaultWorkItemHandlerConfig;
import io.automatik.engine.workflow.StaticProcessConfig;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.ProcessRuntimeImpl;
import io.automatik.engine.workflow.base.instance.context.variable.DefaultVariableInitializer;
import io.automatik.engine.workflow.base.instance.impl.actions.SignalProcessInstanceAction;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

/**
 * Base test case for the jbpm-bpmn2 module.
 */
@Timeout(value = 3000, unit = TimeUnit.SECONDS)
public abstract class JbpmBpmn2TestCase {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	private BpmnProcessCompiler compiler = new BpmnProcessCompiler();

	@BeforeEach
	protected void logTestStartAndSetup(TestInfo testInfo) {
		logger.info(" >>> {} <<<", testInfo.getDisplayName());
		// this is to preserve the same behavior when executing over ksession
		System.setProperty("org.jbpm.signals.defaultscope", SignalProcessInstanceAction.DEFAULT_SCOPE);
	}

	@AfterEach
	protected void logTestEndAndSetup(TestInfo testInfo) {
		logger.info("Finished {}", testInfo.getDisplayName());
		System.clearProperty("org.jbpm.signals.defaultscope");
	}

	protected InternalProcessRuntime createProcessRuntime(String string) {

		List<io.automatik.engine.api.definition.process.Process> processes = compiler
				.parse(new ClassPathResource(string));
		return new ProcessRuntimeImpl(processes.stream()
				.collect(Collectors.toMap(io.automatik.engine.api.definition.process.Process::getId, p -> p)));
	}

	protected BpmnProcess create(String path) {
		return create(null, path);
	}

	protected BpmnProcess create(ProcessConfig config, String path) {
		List<BpmnProcess> processes = BpmnProcess.from(config, new ClassPathResource(path));
		return processes.get(0);
	}

	protected ProcessConfig config(WorkItemHandler... handlers) {
		return config(Arrays.asList(handlers), Collections.emptyList());
	}

	protected ProcessConfig config(ProcessEventListener... listeners) {
		return config(Collections.emptyList(), Arrays.asList(listeners));
	}

	protected ProcessConfig config(List<WorkItemHandler> handlers, List<ProcessEventListener> listeners) {
		DefaultWorkItemHandlerConfig handlerConfig = new DefaultWorkItemHandlerConfig();
		handlers.forEach(h -> handlerConfig.register(h.getName(), h));
		DefaultProcessEventListenerConfig listenerConfig = new DefaultProcessEventListenerConfig();
		listeners.forEach(h -> listenerConfig.register(h));
		ProcessConfig config = new StaticProcessConfig(handlerConfig, listenerConfig,
				new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()), null,
				new DefaultVariableInitializer());

		return config;
	}

	@AfterEach
	public void clear() {
		clearHistory();
	}

	public void assertProcessInstanceCompleted(ProcessInstance processInstance) {
		assertTrue(assertProcessInstanceState(ProcessInstance.STATE_COMPLETED, processInstance),
				"Process instance has not been completed.");
	}

	public void assertProcessInstanceAborted(ProcessInstance processInstance) {
		assertTrue(assertProcessInstanceState(ProcessInstance.STATE_ABORTED, processInstance),
				"Process instance has not been aborted.");
	}

	public void assertProcessInstanceActive(ProcessInstance processInstance) {
		assertTrue(
				assertProcessInstanceState(ProcessInstance.STATE_ACTIVE, processInstance)
						|| assertProcessInstanceState(ProcessInstance.STATE_PENDING, processInstance),
				"Process instance is not active.");
	}

	public void assertProcessInstanceFinished(ProcessInstance processInstance, InternalProcessRuntime processRuntime) {
		assertNull(processRuntime.getProcessInstance(processInstance.getId()),
				"Process instance has not been finished.");
	}

	public void assertNodeActive(String processInstanceId, InternalProcessRuntime processRuntime, String... name) {
		List<String> names = new ArrayList<String>();
		for (String n : name) {
			names.add(n);
		}
		ProcessInstance processInstance = processRuntime.getProcessInstance(processInstanceId);
		if (processInstance instanceof WorkflowProcessInstance) {
			assertNodeActive((WorkflowProcessInstance) processInstance, names);
		}
		if (!names.isEmpty()) {
			String s = names.get(0);
			for (int i = 1; i < names.size(); i++) {
				s += ", " + names.get(i);
			}
			fail("Node(s) not active: " + s);
		}
	}

	private void assertNodeActive(NodeInstanceContainer container, List<String> names) {
		for (NodeInstance nodeInstance : container.getNodeInstances()) {
			String nodeName = nodeInstance.getNodeName();
			if (names.contains(nodeName)) {
				names.remove(nodeName);
			}
			if (nodeInstance instanceof NodeInstanceContainer) {
				assertNodeActive((NodeInstanceContainer) nodeInstance, names);
			}
		}
	}

	public void assertNodeTriggered(String processInstanceId, String... nodeNames) {
		List<String> names = getNotTriggeredNodes(processInstanceId, nodeNames);
		if (!names.isEmpty()) {
			String s = names.get(0);
			for (int i = 1; i < names.size(); i++) {
				s += ", " + names.get(i);
			}
			fail("Node(s) not executed: " + s);
		}
	}

	public void assertNotNodeTriggered(String processInstanceId, String... nodeNames) {
		List<String> names = getNotTriggeredNodes(processInstanceId, nodeNames);
		assertTrue(Arrays.equals(names.toArray(), nodeNames));
	}

	public int getNumberOfNodeTriggered(long processInstanceId, String node) {
		int counter = 0;

		return counter;
	}

	public int getNumberOfProcessInstances(String processId) {
		int counter = 0;

		return counter;
	}

	protected boolean assertProcessInstanceState(int state, ProcessInstance processInstance) {

		return processInstance.getState() == state;
	}

	private List<String> getNotTriggeredNodes(String processInstanceId, String... nodeNames) {
		List<String> names = new ArrayList<String>();
		for (String nodeName : nodeNames) {
			names.add(nodeName);
		}

		return names;
	}

	protected List<String> getCompletedNodes(long processInstanceId) {
		List<String> names = new ArrayList<String>();

		return names;
	}

	protected void clearHistory() {

	}

	public void assertProcessVarExists(ProcessInstance process, String... processVarNames) {
		WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
		List<String> names = new ArrayList<String>();
		for (String nodeName : processVarNames) {
			names.add(nodeName);
		}

		for (String pvar : instance.getVariables().keySet()) {
			if (names.contains(pvar)) {
				names.remove(pvar);
			}
		}

		if (!names.isEmpty()) {
			String s = names.get(0);
			for (int i = 1; i < names.size(); i++) {
				s += ", " + names.get(i);
			}
			fail("Process Variable(s) do not exist: " + s);
		}

	}

	public String getProcessVarValue(ProcessInstance processInstance, String varName) {
		String actualValue = null;
		Object value = ((WorkflowProcessInstanceImpl) processInstance).getVariable(varName);
		if (value != null) {
			actualValue = value.toString();
		}

		return actualValue;
	}

	public void assertProcessVarValue(ProcessInstance processInstance, String varName, Object varValue) {
		String actualValue = getProcessVarValue(processInstance, varName);
		assertEquals(varValue, actualValue, "Variable " + varName + " value misatch!");
	}

	public void assertNodeExists(ProcessInstance process, String... nodeNames) {
		WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
		List<String> names = new ArrayList<String>();
		for (String nodeName : nodeNames) {
			names.add(nodeName);
		}

		for (Node node : instance.getNodeContainer().getNodes()) {
			if (names.contains(node.getName())) {
				names.remove(node.getName());
			}
		}

		if (!names.isEmpty()) {
			String s = names.get(0);
			for (int i = 1; i < names.size(); i++) {
				s += ", " + names.get(i);
			}
			fail("Node(s) do not exist: " + s);
		}
	}

	public void assertNumOfIncommingConnections(ProcessInstance process, String nodeName, int num) {
		assertNodeExists(process, nodeName);
		WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
		for (Node node : instance.getNodeContainer().getNodes()) {
			if (node.getName().equals(nodeName)) {
				if (node.getIncomingConnections().size() != num) {
					fail("Expected incomming connections: " + num + " - found " + node.getIncomingConnections().size());
				} else {
					break;
				}
			}
		}
	}

	public void assertNumOfOutgoingConnections(ProcessInstance process, String nodeName, int num) {
		assertNodeExists(process, nodeName);
		WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
		for (Node node : instance.getNodeContainer().getNodes()) {
			if (node.getName().equals(nodeName)) {
				if (node.getOutgoingConnections().size() != num) {
					fail("Expected outgoing connections: " + num + " - found " + node.getOutgoingConnections().size());
				} else {
					break;
				}
			}
		}
	}

	public void assertVersionEquals(ProcessInstance process, String version) {
		WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
		if (!instance.getWorkflowProcess().getVersion().equals(version)) {
			fail("Expected version: " + version + " - found " + instance.getWorkflowProcess().getVersion());
		}
	}

	public void assertProcessNameEquals(ProcessInstance process, String name) {
		WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
		if (!instance.getWorkflowProcess().getName().equals(name)) {
			fail("Expected name: " + name + " - found " + instance.getWorkflowProcess().getName());
		}
	}

	public void assertPackageNameEquals(ProcessInstance process, String packageName) {
		WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
		if (!instance.getWorkflowProcess().getPackageName().equals(packageName)) {
			fail("Expected package name: " + packageName + " - found "
					+ instance.getWorkflowProcess().getPackageName());
		}
	}

	public Object eval(Reader reader, Map vars) {
		try {
			return eval(toString(reader), vars);
		} catch (IOException e) {
			throw new RuntimeException("Exception Thrown", e);
		}
	}

	private String toString(Reader reader) throws IOException {
		StringBuilder sb = new StringBuilder(1024);
		int charValue;

		while ((charValue = reader.read()) != -1) {
			sb.append((char) charValue);
		}
		return sb.toString();
	}

	public Object eval(String str, Map vars) {

		ParserContext context = new ParserContext();
		context.addPackageImport("org.jbpm.task");
		context.addPackageImport("org.jbpm.task.service");
		context.addPackageImport("org.jbpm.task.query");
		context.addPackageImport("java.util");

		vars.put("now", new Date());
		return MVEL.executeExpression(MVEL.compileExpression(str, context), vars);
	}

	protected void assertProcessInstanceCompleted(String processInstanceId, InternalProcessRuntime processRuntime) {
		ProcessInstance processInstance = processRuntime.getProcessInstance(processInstanceId);
		assertNull(processInstance, "Process instance has not completed.");
	}

	protected void assertProcessInstanceAborted(String processInstanceId, InternalProcessRuntime processRuntime) {
		assertNull(processRuntime.getProcessInstance(processInstanceId));
	}

	protected void assertProcessInstanceActive(String processInstanceId, InternalProcessRuntime processRuntime) {
		assertNotNull(processRuntime.getProcessInstance(processInstanceId));
	}

}
