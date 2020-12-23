
package io.automatiko.engine.workflow.event.process;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.event.process.ProcessCompletedEvent;
import io.automatiko.engine.api.event.process.ProcessEvent;
import io.automatiko.engine.api.event.process.ProcessEventListener;
import io.automatiko.engine.api.event.process.ProcessNodeLeftEvent;
import io.automatiko.engine.api.event.process.ProcessNodeTriggeredEvent;
import io.automatiko.engine.api.event.process.ProcessStartedEvent;
import io.automatiko.engine.api.event.process.ProcessVariableChangedEvent;
import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.workflow.base.core.event.EventFilter;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.base.instance.ProcessRuntimeImpl;
import io.automatiko.engine.workflow.base.instance.impl.Action;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatiko.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.EventTrigger;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatiko.engine.workflow.test.util.AbstractBaseTest;

public class ProcessEventSupportTest extends AbstractBaseTest {

	public void addLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Test
	public void testProcessEventListener() throws Exception {

		// create a simple package with one process to test the events

		ExecutableProcess process = new ExecutableProcess();
		process.setId("org.company.core.process.event");
		process.setName("Event Process");

		StartNode startNode = new StartNode();
		startNode.setName("Start");
		startNode.setId(1);
		process.addNode(startNode);

		ActionNode actionNode = new ActionNode();
		actionNode.setName("Print");
		ProcessAction action = new ConsequenceAction("java", null);
		action.setMetaData("Action", new Action() {
			public void execute(ProcessContext context) throws Exception {
				logger.info("Executed action");
			}
		});
		actionNode.setAction(action);
		actionNode.setId(2);
		process.addNode(actionNode);
		new ConnectionImpl(startNode, Node.CONNECTION_DEFAULT_TYPE, actionNode, Node.CONNECTION_DEFAULT_TYPE);

		EndNode endNode = new EndNode();
		endNode.setName("End");
		endNode.setId(3);
		process.addNode(endNode);
		new ConnectionImpl(actionNode, Node.CONNECTION_DEFAULT_TYPE, endNode, Node.CONNECTION_DEFAULT_TYPE);

		InternalProcessRuntime processRuntime = new ProcessRuntimeImpl(
				Collections.singletonMap(process.getId(), process));
		final List<ProcessEvent> processEventList = new ArrayList<ProcessEvent>();
		final ProcessEventListener processEventListener = new ProcessEventListener() {

			public void afterNodeLeft(ProcessNodeLeftEvent event) {
				processEventList.add(event);
			}

			public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
				processEventList.add(event);
			}

			public void afterProcessCompleted(ProcessCompletedEvent event) {
				processEventList.add(event);
			}

			public void afterProcessStarted(ProcessStartedEvent event) {
				processEventList.add(event);
			}

			public void beforeNodeLeft(ProcessNodeLeftEvent event) {
				processEventList.add(event);
			}

			public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
				processEventList.add(event);
			}

			public void beforeProcessCompleted(ProcessCompletedEvent event) {
				processEventList.add(event);
			}

			public void beforeProcessStarted(ProcessStartedEvent event) {
				processEventList.add(event);
			}

			public void beforeVariableChanged(ProcessVariableChangedEvent event) {
				processEventList.add(event);
			}

			public void afterVariableChanged(ProcessVariableChangedEvent event) {
				processEventList.add(event);
			}

		};
		processRuntime.addEventListener(processEventListener);

		// execute the process
		processRuntime.startProcess("org.company.core.process.event");
		assertEquals(16, processEventList.size());
		assertEquals("org.company.core.process.event",
				((ProcessStartedEvent) processEventList.get(0)).getProcessInstance().getProcessId());
		assertEquals("Start", ((ProcessNodeTriggeredEvent) processEventList.get(1)).getNodeInstance().getNodeName());
		assertEquals("Start", ((ProcessNodeLeftEvent) processEventList.get(2)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeTriggeredEvent) processEventList.get(3)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeLeftEvent) processEventList.get(4)).getNodeInstance().getNodeName());
		assertEquals("End", ((ProcessNodeTriggeredEvent) processEventList.get(5)).getNodeInstance().getNodeName());
		assertEquals("End", ((ProcessNodeLeftEvent) processEventList.get(6)).getNodeInstance().getNodeName());
		assertEquals("org.company.core.process.event",
				((ProcessCompletedEvent) processEventList.get(7)).getProcessInstance().getProcessId());
		assertEquals("org.company.core.process.event",
				((ProcessCompletedEvent) processEventList.get(8)).getProcessInstance().getProcessId());
		assertEquals("End", ((ProcessNodeLeftEvent) processEventList.get(9)).getNodeInstance().getNodeName());
		assertEquals("End", ((ProcessNodeTriggeredEvent) processEventList.get(10)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeLeftEvent) processEventList.get(11)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeTriggeredEvent) processEventList.get(12)).getNodeInstance().getNodeName());
		assertEquals("Start", ((ProcessNodeLeftEvent) processEventList.get(13)).getNodeInstance().getNodeName());
		assertEquals("Start", ((ProcessNodeTriggeredEvent) processEventList.get(14)).getNodeInstance().getNodeName());
		assertEquals("org.company.core.process.event",
				((ProcessStartedEvent) processEventList.get(15)).getProcessInstance().getProcessId());
	}

	@Test
	public void testProcessEventListenerProcessState() throws Exception {

		ExecutableProcess process = new ExecutableProcess();
		process.setId("org.company.core.process.event");
		process.setName("Event Process");

		StartNode startNode = new StartNode();
		startNode.setName("Start");
		startNode.setId(1);
		process.addNode(startNode);

		ActionNode actionNode = new ActionNode();
		actionNode.setName("Print");
		ProcessAction action = new ConsequenceAction("java", null);
		action.setMetaData("Action", new Action() {

			public void execute(ProcessContext context) throws Exception {
				logger.info("Executed action");
			}
		});
		actionNode.setAction(action);
		actionNode.setId(2);
		process.addNode(actionNode);
		new ConnectionImpl(startNode, Node.CONNECTION_DEFAULT_TYPE, actionNode, Node.CONNECTION_DEFAULT_TYPE);

		EndNode endNode = new EndNode();
		endNode.setName("End");
		endNode.setId(3);
		process.addNode(endNode);
		new ConnectionImpl(actionNode, Node.CONNECTION_DEFAULT_TYPE, endNode, Node.CONNECTION_DEFAULT_TYPE);

		InternalProcessRuntime processRuntime = new ProcessRuntimeImpl(
				Collections.singletonMap(process.getId(), process));

		final List<Integer> processEventStatusList = new ArrayList<Integer>();
		final ProcessEventListener processEventListener = new ProcessEventListener() {

			public void afterNodeLeft(ProcessNodeLeftEvent event) {
			}

			public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
			}

			public void afterProcessCompleted(ProcessCompletedEvent event) {
				processEventStatusList.add(new Integer(event.getProcessInstance().getState()));
			}

			public void afterProcessStarted(ProcessStartedEvent event) {
			}

			public void beforeNodeLeft(ProcessNodeLeftEvent event) {
			}

			public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
			}

			public void beforeProcessCompleted(ProcessCompletedEvent event) {
				processEventStatusList.add(new Integer(event.getProcessInstance().getState()));
			}

			public void beforeProcessStarted(ProcessStartedEvent event) {
			}

			public void beforeVariableChanged(ProcessVariableChangedEvent event) {
			}

			public void afterVariableChanged(ProcessVariableChangedEvent event) {
			}

		};
		processRuntime.addEventListener(processEventListener);

		// execute the process
		processRuntime.startProcess("org.company.core.process.event");
		assertEquals(2, processEventStatusList.size());
		assertEquals(new Integer(ProcessInstance.STATE_ACTIVE), processEventStatusList.get(0));
		assertEquals(new Integer(ProcessInstance.STATE_COMPLETED), processEventStatusList.get(1));
	}

	@Test
	public void testProcessEventListenerWithEvent() throws Exception {

		ExecutableProcess process = new ExecutableProcess();
		process.setId("org.company.core.process.event");
		process.setName("Event Process");

		StartNode startNode = new StartNode();
		startNode.setName("Start");
		startNode.setId(1);
		process.addNode(startNode);

		ActionNode actionNode = new ActionNode();
		actionNode.setName("Print");
		ProcessAction action = new ConsequenceAction("java", null);
		action.setMetaData("Action", new Action() {
			public void execute(ProcessContext context) throws Exception {
				logger.info("Executed action");
			}
		});
		actionNode.setAction(action);
		actionNode.setId(2);
		process.addNode(actionNode);
		new ConnectionImpl(startNode, Node.CONNECTION_DEFAULT_TYPE, actionNode, Node.CONNECTION_DEFAULT_TYPE);

		EventNode eventNode = new EventNode();
		eventNode.setName("Event");
		eventNode.setId(3);

		List<EventFilter> filters = new ArrayList<EventFilter>();
		EventTypeFilter filter = new EventTypeFilter();
		filter.setType("signal");
		filters.add(filter);
		eventNode.setEventFilters(filters);
		process.addNode(eventNode);
		new ConnectionImpl(actionNode, Node.CONNECTION_DEFAULT_TYPE, eventNode, Node.CONNECTION_DEFAULT_TYPE);

		EndNode endNode = new EndNode();
		endNode.setName("End");
		endNode.setId(4);
		process.addNode(endNode);
		new ConnectionImpl(eventNode, Node.CONNECTION_DEFAULT_TYPE, endNode, Node.CONNECTION_DEFAULT_TYPE);

		InternalProcessRuntime processRuntime = new ProcessRuntimeImpl(
				Collections.singletonMap(process.getId(), process));
		final List<ProcessEvent> processEventList = new ArrayList<ProcessEvent>();
		final ProcessEventListener processEventListener = new ProcessEventListener() {

			public void afterNodeLeft(ProcessNodeLeftEvent event) {
				processEventList.add(event);
			}

			public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
				processEventList.add(event);
			}

			public void afterProcessCompleted(ProcessCompletedEvent event) {
				processEventList.add(event);
			}

			public void afterProcessStarted(ProcessStartedEvent event) {
				processEventList.add(event);
			}

			public void beforeNodeLeft(ProcessNodeLeftEvent event) {
				processEventList.add(event);
			}

			public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
				processEventList.add(event);
			}

			public void beforeProcessCompleted(ProcessCompletedEvent event) {
				processEventList.add(event);
			}

			public void beforeProcessStarted(ProcessStartedEvent event) {
				processEventList.add(event);
			}

			public void beforeVariableChanged(ProcessVariableChangedEvent event) {
				processEventList.add(event);
			}

			public void afterVariableChanged(ProcessVariableChangedEvent event) {
				processEventList.add(event);
			}

		};
		processRuntime.addEventListener(processEventListener);

		// execute the process
		ProcessInstance pi = processRuntime.startProcess("org.company.core.process.event");
		pi.signalEvent("signal", null);
		assertEquals(20, processEventList.size());
		assertEquals("org.company.core.process.event",
				((ProcessStartedEvent) processEventList.get(0)).getProcessInstance().getProcessId());

		assertEquals("Start", ((ProcessNodeTriggeredEvent) processEventList.get(1)).getNodeInstance().getNodeName());
		assertEquals("Start", ((ProcessNodeLeftEvent) processEventList.get(2)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeTriggeredEvent) processEventList.get(3)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeLeftEvent) processEventList.get(4)).getNodeInstance().getNodeName());
		assertEquals("Event", ((ProcessNodeTriggeredEvent) processEventList.get(5)).getNodeInstance().getNodeName());
		assertEquals("Event", ((ProcessNodeTriggeredEvent) processEventList.get(6)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeLeftEvent) processEventList.get(7)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeTriggeredEvent) processEventList.get(8)).getNodeInstance().getNodeName());
		assertEquals("Start", ((ProcessNodeLeftEvent) processEventList.get(9)).getNodeInstance().getNodeName());
		assertEquals("Start", ((ProcessNodeTriggeredEvent) processEventList.get(10)).getNodeInstance().getNodeName());
		assertEquals("org.company.core.process.event",
				((ProcessStartedEvent) processEventList.get(11)).getProcessInstance().getProcessId());
		assertEquals("Event", ((ProcessNodeLeftEvent) processEventList.get(12)).getNodeInstance().getNodeName());
		assertEquals("End", ((ProcessNodeTriggeredEvent) processEventList.get(13)).getNodeInstance().getNodeName());
		assertEquals("End", ((ProcessNodeLeftEvent) processEventList.get(14)).getNodeInstance().getNodeName());
		assertEquals("org.company.core.process.event",
				((ProcessCompletedEvent) processEventList.get(15)).getProcessInstance().getProcessId());
		assertEquals("org.company.core.process.event",
				((ProcessCompletedEvent) processEventList.get(16)).getProcessInstance().getProcessId());
		assertEquals("End", ((ProcessNodeLeftEvent) processEventList.get(17)).getNodeInstance().getNodeName());
		assertEquals("Event", ((ProcessNodeLeftEvent) processEventList.get(19)).getNodeInstance().getNodeName());
		assertEquals("End", ((ProcessNodeTriggeredEvent) processEventList.get(18)).getNodeInstance().getNodeName());

	}

	@Test
	public void testProcessEventListenerWithEndEvent() throws Exception {

		ExecutableProcess process = new ExecutableProcess();
		process.setId("org.company.core.process.event");
		process.setName("Event Process");

		StartNode startNode = new StartNode();
		startNode.setName("Start");
		startNode.setId(1);
		process.addNode(startNode);

		ActionNode actionNode = new ActionNode();
		actionNode.setName("Print");
		ProcessAction action = new ConsequenceAction("java", null);
		action.setMetaData("Action", new Action() {
			public void execute(ProcessContext context) throws Exception {
				logger.info("Executed action");
			}
		});
		actionNode.setAction(action);
		actionNode.setId(2);
		process.addNode(actionNode);
		new ConnectionImpl(startNode, Node.CONNECTION_DEFAULT_TYPE, actionNode, Node.CONNECTION_DEFAULT_TYPE);

		EndNode endNode = new EndNode();
		endNode.setName("End");
		endNode.setId(3);
		endNode.setTerminate(false);
		process.addNode(endNode);
		new ConnectionImpl(actionNode, Node.CONNECTION_DEFAULT_TYPE, endNode, Node.CONNECTION_DEFAULT_TYPE);

		InternalProcessRuntime processRuntime = new ProcessRuntimeImpl(
				Collections.singletonMap(process.getId(), process));
		final List<ProcessEvent> processEventList = new ArrayList<ProcessEvent>();
		final ProcessEventListener processEventListener = new ProcessEventListener() {

			public void afterNodeLeft(ProcessNodeLeftEvent event) {
				processEventList.add(event);
			}

			public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
				processEventList.add(event);
			}

			public void afterProcessCompleted(ProcessCompletedEvent event) {
				processEventList.add(event);
			}

			public void afterProcessStarted(ProcessStartedEvent event) {
				processEventList.add(event);
			}

			public void beforeNodeLeft(ProcessNodeLeftEvent event) {
				processEventList.add(event);
			}

			public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
				processEventList.add(event);
			}

			public void beforeProcessCompleted(ProcessCompletedEvent event) {
				processEventList.add(event);
			}

			public void beforeProcessStarted(ProcessStartedEvent event) {
				processEventList.add(event);
			}

			public void beforeVariableChanged(ProcessVariableChangedEvent event) {
				processEventList.add(event);
			}

			public void afterVariableChanged(ProcessVariableChangedEvent event) {
				processEventList.add(event);
			}

		};
		processRuntime.addEventListener(processEventListener);

		// execute the process
		processRuntime.startProcess("org.company.core.process.event");
		assertEquals(14, processEventList.size());
		assertEquals("org.company.core.process.event",
				((ProcessStartedEvent) processEventList.get(0)).getProcessInstance().getProcessId());
		assertEquals("Start", ((ProcessNodeTriggeredEvent) processEventList.get(1)).getNodeInstance().getNodeName());
		assertEquals("Start", ((ProcessNodeLeftEvent) processEventList.get(2)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeTriggeredEvent) processEventList.get(3)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeLeftEvent) processEventList.get(4)).getNodeInstance().getNodeName());
		assertEquals("End", ((ProcessNodeTriggeredEvent) processEventList.get(5)).getNodeInstance().getNodeName());
		assertEquals("End", ((ProcessNodeLeftEvent) processEventList.get(6)).getNodeInstance().getNodeName());
		assertEquals("End", ((ProcessNodeLeftEvent) processEventList.get(7)).getNodeInstance().getNodeName());
		assertEquals("End", ((ProcessNodeTriggeredEvent) processEventList.get(8)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeLeftEvent) processEventList.get(9)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeTriggeredEvent) processEventList.get(10)).getNodeInstance().getNodeName());
		assertEquals("Start", ((ProcessNodeLeftEvent) processEventList.get(11)).getNodeInstance().getNodeName());
		assertEquals("Start", ((ProcessNodeTriggeredEvent) processEventList.get(12)).getNodeInstance().getNodeName());
		assertEquals("org.company.core.process.event",
				((ProcessStartedEvent) processEventList.get(13)).getProcessInstance().getProcessId());
	}

	@Test
	public void testProcessEventListenerWithStartEvent() throws Exception {

		ExecutableProcess process = new ExecutableProcess();
		process.setId("org.company.core.process.event");
		process.setName("Event Process");

		StartNode startNode = new StartNode();
		startNode.setName("Start");
		startNode.setId(1);
		EventTrigger trigger = new EventTrigger();
		EventTypeFilter eventFilter = new EventTypeFilter();
		eventFilter.setType("signal");
		trigger.addEventFilter(eventFilter);
		startNode.addTrigger(trigger);
		process.addNode(startNode);

		ActionNode actionNode = new ActionNode();
		actionNode.setName("Print");
		ProcessAction action = new ConsequenceAction("java", null);
		action.setMetaData("Action", new Action() {
			public void execute(ProcessContext context) throws Exception {
				logger.info("Executed action");
			}
		});
		actionNode.setAction(action);
		actionNode.setId(2);
		process.addNode(actionNode);
		new ConnectionImpl(startNode, Node.CONNECTION_DEFAULT_TYPE, actionNode, Node.CONNECTION_DEFAULT_TYPE);

		EndNode endNode = new EndNode();
		endNode.setName("End");
		endNode.setId(3);
		process.addNode(endNode);
		new ConnectionImpl(actionNode, Node.CONNECTION_DEFAULT_TYPE, endNode, Node.CONNECTION_DEFAULT_TYPE);

		InternalProcessRuntime processRuntime = new ProcessRuntimeImpl(
				Collections.singletonMap(process.getId(), process));
		final List<ProcessEvent> processEventList = new ArrayList<ProcessEvent>();
		final ProcessEventListener processEventListener = new ProcessEventListener() {

			public void afterNodeLeft(ProcessNodeLeftEvent event) {
				processEventList.add(event);
			}

			public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
				processEventList.add(event);
			}

			public void afterProcessCompleted(ProcessCompletedEvent event) {
				processEventList.add(event);
			}

			public void afterProcessStarted(ProcessStartedEvent event) {
				processEventList.add(event);
			}

			public void beforeNodeLeft(ProcessNodeLeftEvent event) {
				processEventList.add(event);
			}

			public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
				processEventList.add(event);
			}

			public void beforeProcessCompleted(ProcessCompletedEvent event) {
				processEventList.add(event);
			}

			public void beforeProcessStarted(ProcessStartedEvent event) {
				processEventList.add(event);
			}

			public void beforeVariableChanged(ProcessVariableChangedEvent event) {
				processEventList.add(event);
			}

			public void afterVariableChanged(ProcessVariableChangedEvent event) {
				processEventList.add(event);
			}

		};
		processRuntime.addEventListener(processEventListener);

		// execute the process
//        session.startProcess("org.company.core.process.event");
		processRuntime.signalEvent("signal", null);
		assertEquals(16, processEventList.size());
		assertEquals("org.company.core.process.event",
				((ProcessStartedEvent) processEventList.get(0)).getProcessInstance().getProcessId());
		assertEquals("Start", ((ProcessNodeTriggeredEvent) processEventList.get(1)).getNodeInstance().getNodeName());
		assertEquals("Start", ((ProcessNodeLeftEvent) processEventList.get(2)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeTriggeredEvent) processEventList.get(3)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeLeftEvent) processEventList.get(4)).getNodeInstance().getNodeName());
		assertEquals("End", ((ProcessNodeTriggeredEvent) processEventList.get(5)).getNodeInstance().getNodeName());
		assertEquals("End", ((ProcessNodeLeftEvent) processEventList.get(6)).getNodeInstance().getNodeName());
		assertEquals("org.company.core.process.event",
				((ProcessCompletedEvent) processEventList.get(7)).getProcessInstance().getProcessId());
		assertEquals("org.company.core.process.event",
				((ProcessCompletedEvent) processEventList.get(8)).getProcessInstance().getProcessId());
		assertEquals("End", ((ProcessNodeLeftEvent) processEventList.get(9)).getNodeInstance().getNodeName());
		assertEquals("End", ((ProcessNodeTriggeredEvent) processEventList.get(10)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeLeftEvent) processEventList.get(11)).getNodeInstance().getNodeName());
		assertEquals("Print", ((ProcessNodeTriggeredEvent) processEventList.get(12)).getNodeInstance().getNodeName());
		assertEquals("Start", ((ProcessNodeLeftEvent) processEventList.get(13)).getNodeInstance().getNodeName());
		assertEquals("Start", ((ProcessNodeTriggeredEvent) processEventList.get(14)).getNodeInstance().getNodeName());
		assertEquals("org.company.core.process.event",
				((ProcessStartedEvent) processEventList.get(15)).getProcessInstance().getProcessId());
	}

}
