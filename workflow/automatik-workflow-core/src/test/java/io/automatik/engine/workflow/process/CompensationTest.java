
package io.automatik.engine.workflow.process;

import static io.automatik.engine.workflow.process.test.NodeCreator.connect;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.definition.process.NodeContainer;
import io.automatik.engine.api.runtime.process.ProcessContext;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.context.exception.CompensationHandler;
import io.automatik.engine.workflow.base.core.context.exception.CompensationScope;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatik.engine.workflow.base.core.event.EventFilter;
import io.automatik.engine.workflow.base.core.event.EventTypeFilter;
import io.automatik.engine.workflow.base.core.event.NonAcceptingEventTypeFilter;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.impl.Action;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.ProcessAction;
import io.automatik.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatik.engine.workflow.process.core.node.ActionNode;
import io.automatik.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatik.engine.workflow.process.core.node.CompositeContextNode;
import io.automatik.engine.workflow.process.core.node.CompositeNode;
import io.automatik.engine.workflow.process.core.node.EndNode;
import io.automatik.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatik.engine.workflow.process.test.NodeCreator;
import io.automatik.engine.workflow.process.test.TestWorkItemHandler;
import io.automatik.engine.workflow.test.util.AbstractBaseTest;

public class CompensationTest extends AbstractBaseTest {

	public void addLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	private InternalProcessRuntime ksession;

	@AfterEach
	public void cleanUp() {
		if (ksession != null) {
			ksession.dispose();
			ksession = null;
		}
	}

	/*
	 * General HELPER methods
	 */

	private void addCompensationScope(final Node node,
			final io.automatik.engine.api.definition.process.NodeContainer parentContainer,
			final String compensationHandlerId) {
		ContextContainer contextContainer = (ContextContainer) parentContainer;
		CompensationScope scope = null;
		boolean addScope = false;
		if (contextContainer.getContexts(CompensationScope.COMPENSATION_SCOPE) == null) {
			addScope = true;
		} else {
			scope = (CompensationScope) contextContainer.getContexts(CompensationScope.COMPENSATION_SCOPE).get(0);
			if (scope == null) {
				addScope = true;
			}
		}
		if (addScope) {
			scope = new CompensationScope();
			contextContainer.addContext(scope);
			contextContainer.setDefaultContext(scope);
			scope.setContextContainer(contextContainer);
		}

		CompensationHandler handler = new CompensationHandler();
		handler.setNode(node);
		scope.setExceptionHandler(compensationHandlerId, handler);

		node.setMetaData("isForCompensation", Boolean.TRUE);
	}

	private Node findNode(ExecutableProcess process, String nodeName) {
		Node found = null;
		Queue<io.automatik.engine.api.definition.process.Node> nodes = new LinkedList<io.automatik.engine.api.definition.process.Node>();
		nodes.addAll(Arrays.asList(process.getNodes()));
		while (!nodes.isEmpty()) {
			io.automatik.engine.api.definition.process.Node node = nodes.poll();
			if (node.getName().equals(nodeName)) {
				found = (Node) node;
			}
			if (node instanceof NodeContainer) {
				nodes.addAll(Arrays.asList(((NodeContainer) node).getNodes()));
			}
		}
		assertNotNull(found, "Could not find node (" + nodeName + ").");

		return found;
	}

	/*
	 * TESTS
	 */

	@Test
	public void testCompensationBoundaryEventSpecific() throws Exception {
		String processId = "org.jbpm.process.compensation.boundary";
		String[] workItemNames = { "Don-Quixote", "Sancho", "Ricote" };
		List<String> eventList = new ArrayList<String>();
		ExecutableProcess process = createCompensationBoundaryEventProcess(processId, workItemNames, eventList);

		// run process
		ksession = createProcessRuntime(process);

		Node compensatedNode = findNode(process, "work1");
		String compensationEvent = (String) compensatedNode.getMetaData().get("UniqueId");

		runCompensationBoundaryEventSpecificTest(ksession, process, processId, workItemNames, eventList,
				compensationEvent);
	}

	public static void runCompensationBoundaryEventSpecificTest(InternalProcessRuntime ksession,
			ExecutableProcess process, String processId, String[] workItemNames, List<String> eventList,
			String compensationEvent) {
		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
		for (String workItem : workItemNames) {
			ksession.getWorkItemManager().registerWorkItemHandler(workItem, workItemHandler);
		}
		ProcessInstance processInstance = ksession.startProcess(processId);

		// call compensation on the uncompleted work 1 (which should not fire)

		ksession.signalEvent("Compensation", compensationEvent, processInstance.getId());
		assertEquals(0, eventList.size(), "Compensation should not have fired yet.");

		// complete work 1
		ksession.getWorkItemManager().completeWorkItem(workItemHandler.getWorkItems().removeLast().getId(), null);
		assertEquals(ProcessInstance.STATE_ACTIVE, processInstance.getState());

		// call compensation on work 1, which should now fire
		ksession.signalEvent("Compensation", compensationEvent, processInstance.getId());
		assertEquals(1, eventList.size(), "Compensation should have fired.");

		// complete work 2 & 3
		ksession.getWorkItemManager().completeWorkItem(workItemHandler.getWorkItems().removeLast().getId(), null);
		ksession.getWorkItemManager().completeWorkItem(workItemHandler.getWorkItems().removeLast().getId(), null);
		assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
	}

	@Test
	public void testCompensationBoundaryEventGeneral() throws Exception {
		String processId = "org.jbpm.process.compensation.boundary";
		String[] workItemNames = { "Don-Quixote", "Sancho", "Ricote" };
		List<String> eventList = new ArrayList<String>();
		ExecutableProcess process = createCompensationBoundaryEventProcess(processId, workItemNames, eventList);

		// run process
		ksession = createProcessRuntime(process);

		String compensationEvent = CompensationScope.IMPLICIT_COMPENSATION_PREFIX + processId;

		runCompensationBoundaryEventGeneralTest(ksession, process, processId, workItemNames, eventList,
				compensationEvent);
	}

	public static void runCompensationBoundaryEventGeneralTest(InternalProcessRuntime ksession,
			ExecutableProcess process, String processId, String[] workItemNames, List<String> eventList,
			String compensationEvent) {
		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
		for (String workItem : workItemNames) {
			ksession.getWorkItemManager().registerWorkItemHandler(workItem, workItemHandler);
		}
		ProcessInstance processInstance = ksession.startProcess(processId);

		// general compensation should not cause anything to happen
		ksession.signalEvent("Compensation", compensationEvent, processInstance.getId());
		assertEquals(0, eventList.size(), "Compensation should not have fired yet.");

		// complete work 1 & 2
		ksession.getWorkItemManager().completeWorkItem(workItemHandler.getWorkItems().removeLast().getId(), null);
		ksession.getWorkItemManager().completeWorkItem(workItemHandler.getWorkItems().removeLast().getId(), null);
		assertEquals(ProcessInstance.STATE_ACTIVE, processInstance.getState());
		assertEquals(0, eventList.size(), "Compensation should not have fired yet.");

		// general compensation should now cause the compensation handlers to fire in
		// reverse order
		ksession.signalEvent("Compensation", compensationEvent, processInstance.getId());
		assertEquals(2, eventList.size(), "Compensation should have fired.");

		// complete work 3 and finish
		ksession.getWorkItemManager().completeWorkItem(workItemHandler.getWorkItems().removeLast().getId(), null);
		assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
	}

	private ExecutableProcess createCompensationBoundaryEventProcess(String processId, String[] workItemNames,
			final List<String> eventList) throws Exception {
		ExecutableProcess process = new ExecutableProcess();
		process.setAutoComplete(true);
		process.setId(processId);
		process.setName("CESP Process");
		process.setMetaData("Compensation", true);

		List<Variable> variables = new ArrayList<Variable>();
		Variable variable = new Variable();
		variable.setName("event");
		ObjectDataType personDataType = new ObjectDataType(java.lang.String.class);
		variable.setType(personDataType);
		variables.add(variable);
		process.getVariableScope().setVariables(variables);

		NodeCreator<StartNode> startNodeCreator = new NodeCreator<StartNode>(process, StartNode.class);
		NodeCreator<EndNode> endNodeCreator = new NodeCreator<EndNode>(process, EndNode.class);
		NodeCreator<WorkItemNode> workItemNodeCreator = new NodeCreator<WorkItemNode>(process, WorkItemNode.class);
		NodeCreator<BoundaryEventNode> boundaryNodeCreator = new NodeCreator<BoundaryEventNode>(process,
				BoundaryEventNode.class);
		NodeCreator<ActionNode> actionNodeCreator = new NodeCreator<ActionNode>(process, ActionNode.class);

		// Create process
		StartNode startNode = startNodeCreator.createNode("start");
		Node lastNode = startNode;
		WorkItemNode[] workItemNodes = new WorkItemNode[3];
		for (int i = 0; i < 3; ++i) {
			workItemNodes[i] = workItemNodeCreator.createNode("work" + (i + 1));
			workItemNodes[i].getWork().setName(workItemNames[i]);
			connect(lastNode, workItemNodes[i]);
			lastNode = workItemNodes[i];
		}

		EndNode endNode = endNodeCreator.createNode("end");
		connect(workItemNodes[2], endNode);

		// Compensation (boundary event) handlers
		for (int i = 0; i < 3; ++i) {
			createBoundaryEventCompensationHandler(process, workItemNodes[i], eventList, "" + i + 1);
		}

		return process;
	}

	@Test
	public void testCompensationEventSubProcessSpecific() throws Exception {
		String processId = "org.jbpm.process.compensation.event.subprocess";
		String[] workItemNames = { "kwik", "kwek", "kwak" };
		List<String> eventList = new ArrayList<String>();
		ExecutableProcess process = createCompensationEventSubProcessProcess(processId, workItemNames, eventList);

		Node toCompensateNode = findNode(process, "sub0");
		String compensationEvent = (String) toCompensateNode.getMetaData().get("UniqueId");

		// run process
		ksession = createProcessRuntime(process);

		runCompensationEventSubProcessSpecificTest(ksession, process, processId, workItemNames, eventList,
				compensationEvent);
	}

	public static void runCompensationEventSubProcessSpecificTest(InternalProcessRuntime ksession,
			ExecutableProcess process, String processId, String[] workItemNames, List<String> eventList,
			String compensationEvent) {
		// run process
		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
		for (String workItem : workItemNames) {
			ksession.getWorkItemManager().registerWorkItemHandler(workItem, workItemHandler);
		}
		ProcessInstance processInstance = ksession.startProcess(processId);

		// call compensation on the uncompleted work 1 (which should not fire)
		ksession.signalEvent("Compensation", compensationEvent, processInstance.getId());
		assertEquals(0, eventList.size(), "Compensation should not have fired yet.");

		// pre work item
		ksession.getWorkItemManager().completeWorkItem(workItemHandler.getWorkItems().removeLast().getId(), null);

		// sub-process is active, but not complete
		ksession.signalEvent("Compensation", compensationEvent, processInstance.getId());
		assertEquals(0, eventList.size(), "Compensation should not have fired yet.");

		// sub process work item
		ksession.getWorkItemManager().completeWorkItem(workItemHandler.getWorkItems().removeLast().getId(), null);

		// sub-process has completed
		ksession.signalEvent("Compensation", compensationEvent, processInstance.getId());
		assertEquals(1, eventList.size(), "Compensation should have fired once.");

		// post work item
		ksession.getWorkItemManager().completeWorkItem(workItemHandler.getWorkItems().removeLast().getId(), null);
		assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
	}

	@Test
	public void testCompensationEventSubProcessGeneral() throws Exception {
		String processId = "org.jbpm.process.compensation.event.subprocess.general";
		String[] workItemNames = { "kwik", "kwek", "kwak" };
		List<String> eventList = new ArrayList<String>();
		ExecutableProcess process = createCompensationEventSubProcessProcess(processId, workItemNames, eventList);

		String compensationEvent = CompensationScope.IMPLICIT_COMPENSATION_PREFIX + process.getId();

		// run process
		ksession = createProcessRuntime(process);

		runCompensationEventSubProcessGeneralTest(ksession, process, processId, workItemNames, eventList,
				compensationEvent);
	}

	public static void runCompensationEventSubProcessGeneralTest(InternalProcessRuntime ksession,
			ExecutableProcess process, String processId, String[] workItemNames, List<String> eventList,
			String compensationEvent) {
		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
		for (String workItem : workItemNames) {
			ksession.getWorkItemManager().registerWorkItemHandler(workItem, workItemHandler);
		}
		ProcessInstance processInstance = ksession.startProcess(processId);

		// pre and sub process work item
		ksession.getWorkItemManager().completeWorkItem(workItemHandler.getWorkItems().removeLast().getId(), null);
		ksession.getWorkItemManager().completeWorkItem(workItemHandler.getWorkItems().removeLast().getId(), null);

		// Call general compensation

		ksession.signalEvent("Compensation", compensationEvent, processInstance.getId());
		assertEquals(1, eventList.size(), "Compensation should have fired once.");

		// post work item
		ksession.getWorkItemManager().completeWorkItem(workItemHandler.getWorkItems().removeLast().getId(), null);
		assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
	}

	private ExecutableProcess createCompensationEventSubProcessProcess(String processId, String[] workItemNames,
			final List<String> eventList) throws Exception {
		ExecutableProcess process = new ExecutableProcess();
		process.setAutoComplete(true);
		process.setId(processId);
		process.setName("CESP Process");
		process.setMetaData("Compensation", true);

		NodeCreator<StartNode> startNodeCreator = new NodeCreator<StartNode>(process, StartNode.class);
		NodeCreator<WorkItemNode> workItemNodeCreator = new NodeCreator<WorkItemNode>(process, WorkItemNode.class);
		NodeCreator<CompositeContextNode> compNodeCreator = new NodeCreator<CompositeContextNode>(process,
				CompositeContextNode.class);
		NodeCreator<EndNode> endNodeCreator = new NodeCreator<EndNode>(process, EndNode.class);

		// outer process
		StartNode startNode = startNodeCreator.createNode("start0");
		WorkItemNode workItemNode = workItemNodeCreator.createNode("work0-pre");
		workItemNode.getWork().setName(workItemNames[0]);
		connect(startNode, workItemNode);

		CompositeNode compositeNode = compNodeCreator.createNode("sub0");
		connect(workItemNode, compositeNode);

		workItemNode = workItemNodeCreator.createNode("work0-post");
		workItemNode.getWork().setName(workItemNames[2]);
		connect(compositeNode, workItemNode);

		EndNode endNode = endNodeCreator.createNode("end0");
		connect(workItemNode, endNode);

		// 1rst level nested subprocess
		startNodeCreator.setNodeContainer(compositeNode);
		workItemNodeCreator.setNodeContainer(compositeNode);
		endNodeCreator.setNodeContainer(compositeNode);

		startNode = startNodeCreator.createNode("start1");
		workItemNode = workItemNodeCreator.createNode("work1");
		workItemNode.getWork().setName(workItemNames[1]);
		connect(startNode, workItemNode);

		endNode = endNodeCreator.createNode("end1");
		connect(workItemNode, endNode);

		// 2nd level nested event subprocess in 1rst level subprocess
		NodeCreator<EventSubProcessNode> espNodeCreator = new NodeCreator<EventSubProcessNode>(compositeNode,
				EventSubProcessNode.class);
		EventSubProcessNode espNode = espNodeCreator.createNode("eventSub1");
		EventTypeFilter eventFilter = new NonAcceptingEventTypeFilter();
		eventFilter.setType("Compensation");
		espNode.addEvent(eventFilter);

		addCompensationScope(espNode, process, (String) compositeNode.getMetaData("UniqueId"));

		startNodeCreator.setNodeContainer(espNode);
		endNodeCreator.setNodeContainer(espNode);
		NodeCreator<ActionNode> actionNodeCreator = new NodeCreator<ActionNode>(espNode, ActionNode.class);

		startNode = startNodeCreator.createNode("start1*");
		ActionNode actionNode = actionNodeCreator.createNode("action1*");
		actionNode.setName("Execute");
		ProcessAction action = new ConsequenceAction("java", null);
		action.setMetaData("Action", new Action() {
			public void execute(ProcessContext context) throws Exception {
				eventList.add("Executed action");
			}
		});
		actionNode.setAction(action);
		connect(startNode, actionNode);

		endNode = endNodeCreator.createNode("end1*");
		connect(actionNode, endNode);

		return process;
	}

	@Test
	public void testNestedCompensationEventSubProcessSpecific() throws Exception {
		String processId = "org.jbpm.process.compensation.event.nested.subprocess";
		String[] workItemNames = { "kwik", "kwek", "kwak" };
		List<String> eventList = new ArrayList<String>();
		ExecutableProcess process = createNestedCompensationEventSubProcessProcess(processId, workItemNames, eventList);

		Node toCompensateNode = findNode(process, "sub1");
		String compensationEvent = (String) toCompensateNode.getMetaData().get("UniqueId");

		ksession = createProcessRuntime(process);

		runCompensationEventSubProcessSpecificTest(ksession, process, processId, workItemNames, eventList,
				compensationEvent);
	}

	@Test
	public void testNestedCompensationEventSubProcessGeneral() throws Exception {
		String processId = "org.jbpm.process.compensation.event.subprocess.general";
		String[] workItemNames = { "apple", "banana", "orange" };
		List<String> eventList = new ArrayList<String>();
		ExecutableProcess process = createNestedCompensationEventSubProcessProcess(processId, workItemNames, eventList);

		Node toCompensateNode = findNode(process, "sub0");
		String compensationEvent = CompensationScope.IMPLICIT_COMPENSATION_PREFIX
				+ toCompensateNode.getMetaData().get("UniqueId");

		ksession = createProcessRuntime(process);

		runCompensationEventSubProcessGeneralTest(ksession, process, processId, workItemNames, eventList,
				compensationEvent);
	}

	private ExecutableProcess createNestedCompensationEventSubProcessProcess(String processId, String[] workItemNames,
			final List<String> eventList) throws Exception {
		ExecutableProcess process = new ExecutableProcess();
		process.setAutoComplete(true);
		process.setId(processId);
		process.setName("CESP Process");
		process.setMetaData("Compensation", true);

		NodeCreator<StartNode> startNodeCreator = new NodeCreator<StartNode>(process, StartNode.class);
		NodeCreator<WorkItemNode> workItemNodeCreator = new NodeCreator<WorkItemNode>(process, WorkItemNode.class);
		NodeCreator<CompositeContextNode> compNodeCreator = new NodeCreator<CompositeContextNode>(process,
				CompositeContextNode.class);
		NodeCreator<EndNode> endNodeCreator = new NodeCreator<EndNode>(process, EndNode.class);

		// outer process
		CompositeContextNode compositeNode = compNodeCreator.createNode("sub0");
		{
			StartNode startNode = startNodeCreator.createNode("start0");
			WorkItemNode workItemNode = workItemNodeCreator.createNode("work0-pre");
			workItemNode.getWork().setName(workItemNames[0]);
			connect(startNode, workItemNode);

			connect(workItemNode, compositeNode);

			EndNode endNode = endNodeCreator.createNode("end0");
			connect(compositeNode, endNode);
		}

		// 1rst level nested subprocess (contains compensation visibility scope)
		CompositeContextNode compensationScopeContainerNode = compositeNode;
		{
			startNodeCreator.setNodeContainer(compositeNode);
			workItemNodeCreator.setNodeContainer(compositeNode);
			compNodeCreator.setNodeContainer(compositeNode);
			endNodeCreator.setNodeContainer(compositeNode);

			StartNode startNode = startNodeCreator.createNode("start1");
			CompositeContextNode subCompNode = compNodeCreator.createNode("sub1");
			connect(startNode, subCompNode);

			WorkItemNode workItemNode = workItemNodeCreator.createNode("work1-post");
			workItemNode.getWork().setName(workItemNames[2]);
			connect(subCompNode, workItemNode);

			EndNode endNode = endNodeCreator.createNode("end1");
			connect(workItemNode, endNode);

			compositeNode = subCompNode;
		}

		// 2nd level nested subprocess
		{
			startNodeCreator.setNodeContainer(compositeNode);
			workItemNodeCreator.setNodeContainer(compositeNode);
			endNodeCreator.setNodeContainer(compositeNode);

			StartNode startNode = startNodeCreator.createNode("start2");
			WorkItemNode workItemNode = workItemNodeCreator.createNode("work2");
			workItemNode.getWork().setName(workItemNames[1]);
			connect(startNode, workItemNode);

			EndNode endNode = endNodeCreator.createNode("end2");
			connect(workItemNode, endNode);
		}

		// 3nd level nested event subprocess in 2nd level subprocess
		{
			NodeCreator<EventSubProcessNode> espNodeCreator = new NodeCreator<EventSubProcessNode>(compositeNode,
					EventSubProcessNode.class);
			EventSubProcessNode espNode = espNodeCreator.createNode("eventSub2");

			startNodeCreator.setNodeContainer(espNode);
			endNodeCreator.setNodeContainer(espNode);
			NodeCreator<ActionNode> actionNodeCreator = new NodeCreator<ActionNode>(espNode, ActionNode.class);

			EventTypeFilter eventFilter = new NonAcceptingEventTypeFilter();
			eventFilter.setType("Compensation");
			espNode.addEvent(eventFilter);

			addCompensationScope(espNode, compensationScopeContainerNode,
					(String) compositeNode.getMetaData("UniqueId"));

			StartNode startNode = startNodeCreator.createNode("start3*");
			ActionNode actionNode = actionNodeCreator.createNode("action3*");
			actionNode.setName("Execute");
			ProcessAction action = new ConsequenceAction("java", null);
			action.setMetaData("Action", new Action() {
				public void execute(ProcessContext context) throws Exception {
					eventList.add("Executed action");
				}
			});
			actionNode.setAction(action);
			connect(startNode, actionNode);

			EndNode endNode = endNodeCreator.createNode("end3*");
			connect(actionNode, endNode);
		}

		return process;
	}

	@Test
	public void testNestedCompensationBoundaryEventSpecific() throws Exception {
		String processId = "org.jbpm.process.compensation.boundary.nested";
		String[] workItemNames = { "Don-Quixote", "Sancho", "Ricote" };
		List<String> eventList = new ArrayList<String>();
		ExecutableProcess process = createNestedCompensationBoundaryEventProcess(processId, workItemNames, eventList);

		// run process
		ksession = createProcessRuntime(process);

		Node compensatedNode = findNode(process, "work-comp-1");
		String compensationEvent = (String) compensatedNode.getMetaData().get("UniqueId");

		runCompensationBoundaryEventSpecificTest(ksession, process, processId, workItemNames, eventList,
				compensationEvent);
	}

	@Test
	public void testNestedCompensationBoundaryEventGeneral() throws Exception {
		String processId = "org.jbpm.process.compensation.boundary.general.nested";
		String[] workItemNames = { "Jip", "Janneke", "Takkie" };
		List<String> eventList = new ArrayList<String>();
		ExecutableProcess process = createNestedCompensationBoundaryEventProcess(processId, workItemNames, eventList);

		// run process
		ksession = createProcessRuntime(process);

		Node toCompensateNode = findNode(process, "sub2");
		String compensationEvent = CompensationScope.IMPLICIT_COMPENSATION_PREFIX
				+ (String) toCompensateNode.getMetaData().get("UniqueId");

		runCompensationBoundaryEventGeneralTest(ksession, process, processId, workItemNames, eventList,
				compensationEvent);
	}

	private ExecutableProcess createNestedCompensationBoundaryEventProcess(String processId, String[] workItemNames,
			final List<String> eventList) throws Exception {
		ExecutableProcess process = new ExecutableProcess();
		process.setAutoComplete(true);
		process.setId(processId);
		process.setName("CESP Process");
		process.setMetaData("Compensation", true);

		List<Variable> variables = new ArrayList<Variable>();
		Variable variable = new Variable();
		variable.setName("event");
		ObjectDataType personDataType = new ObjectDataType(java.lang.String.class);
		variable.setType(personDataType);
		variables.add(variable);
		process.getVariableScope().setVariables(variables);

		NodeCreator<StartNode> startNodeCreator = new NodeCreator<StartNode>(process, StartNode.class);
		NodeCreator<EndNode> endNodeCreator = new NodeCreator<EndNode>(process, EndNode.class);
		NodeCreator<CompositeContextNode> compNodeCreator = new NodeCreator<CompositeContextNode>(process,
				CompositeContextNode.class);

		// process level
		CompositeContextNode compositeNode = compNodeCreator.createNode("sub0");
		{
			StartNode startNode = startNodeCreator.createNode("start0");
			connect(startNode, compositeNode);

			EndNode endNode = endNodeCreator.createNode("end0");
			connect(compositeNode, endNode);
		}

		// 1rst level nested subprocess (contains compensation visibility scope)
		{
			startNodeCreator.setNodeContainer(compositeNode);
			compNodeCreator.setNodeContainer(compositeNode);
			endNodeCreator.setNodeContainer(compositeNode);

			StartNode startNode = startNodeCreator.createNode("start1");
			CompositeContextNode subCompNode = compNodeCreator.createNode("sub1");
			connect(startNode, subCompNode);

			EndNode endNode = endNodeCreator.createNode("end1");
			connect(subCompNode, endNode);

			compositeNode = subCompNode;
		}

		// 2nd level nested subprocess (contains compensation visibility scope)
		NodeCreator<WorkItemNode> workItemNodeCreator = new NodeCreator<WorkItemNode>(compositeNode,
				WorkItemNode.class);
		{
			startNodeCreator.setNodeContainer(compositeNode);
			compNodeCreator.setNodeContainer(compositeNode);
			endNodeCreator.setNodeContainer(compositeNode);

			StartNode startNode = startNodeCreator.createNode("start2");
			CompositeContextNode subCompNode = compNodeCreator.createNode("sub2");
			connect(startNode, subCompNode);

			WorkItemNode workItemNode = workItemNodeCreator.createNode("work2");
			workItemNode.getWork().setName(workItemNames[2]);
			connect(subCompNode, workItemNode);

			EndNode endNode = endNodeCreator.createNode("end2");
			connect(workItemNode, endNode);

			createBoundaryEventCompensationHandler(compositeNode, workItemNode, eventList, "2");

			compositeNode = subCompNode;
		}

		// Fill 3rd level with process with compensation
		{
			startNodeCreator.setNodeContainer(compositeNode);
			workItemNodeCreator.setNodeContainer(compositeNode);
			endNodeCreator.setNodeContainer(compositeNode);

			StartNode startNode = startNodeCreator.createNode("start");
			Node lastNode = startNode;
			WorkItemNode[] workItemNodes = new WorkItemNode[3];
			for (int i = 0; i < 2; ++i) {
				workItemNodes[i] = workItemNodeCreator.createNode("work-comp-" + (i + 1));
				workItemNodes[i].getWork().setName(workItemNames[i]);
				connect(lastNode, workItemNodes[i]);
				lastNode = workItemNodes[i];
			}

			EndNode endNode = endNodeCreator.createNode("end");
			connect(workItemNodes[1], endNode);

			// Compensation (boundary event) handlers
			for (int i = 0; i < 2; ++i) {
				createBoundaryEventCompensationHandler(compositeNode, workItemNodes[i], eventList, "" + i + 1);
			}
		}
		return process;
	}

	private void createBoundaryEventCompensationHandler(
			io.automatik.engine.workflow.process.core.NodeContainer nodeContainer, Node attachedToNode,
			final List<String> eventList, final String id) throws Exception {

		NodeCreator<BoundaryEventNode> boundaryNodeCreator = new NodeCreator<BoundaryEventNode>(nodeContainer,
				BoundaryEventNode.class);
		BoundaryEventNode boundaryNode = boundaryNodeCreator.createNode("boundary" + id);
		String attachedTo = (String) attachedToNode.getMetaData().get("UniqueId");
		boundaryNode.setMetaData("AttachedTo", attachedTo);
		boundaryNode.setAttachedToNodeId(attachedTo);

		EventTypeFilter eventFilter = new NonAcceptingEventTypeFilter();
		eventFilter.setType("Compensation");
		List<EventFilter> eventFilters = new ArrayList<EventFilter>();
		boundaryNode.setEventFilters(eventFilters);
		eventFilters.add(eventFilter);

		addCompensationScope(boundaryNode, nodeContainer, attachedTo);

		NodeCreator<ActionNode> actionNodeCreator = new NodeCreator<ActionNode>(nodeContainer, ActionNode.class);
		ActionNode actionNode = actionNodeCreator.createNode("handlerAction" + id);
		actionNode.setMetaData("isForCompensation", true);
		actionNode.setName("Execute");
		ProcessAction action = new ConsequenceAction("java", null);
		action.setMetaData("Action", new Action() {
			public void execute(ProcessContext context) throws Exception {
				eventList.add("action" + id);
			}
		});
		actionNode.setAction(action);
		connect(boundaryNode, actionNode);
	}
}
