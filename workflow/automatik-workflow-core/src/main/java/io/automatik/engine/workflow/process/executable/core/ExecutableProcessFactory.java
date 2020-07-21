
package io.automatik.engine.workflow.process.executable.core;

import static io.automatik.engine.workflow.process.core.impl.ExtendedNodeImpl.EVENT_NODE_EXIT;
import static io.automatik.engine.workflow.process.executable.core.Metadata.ACTION;
import static io.automatik.engine.workflow.process.executable.core.Metadata.ATTACHED_TO;
import static io.automatik.engine.workflow.process.executable.core.Metadata.CANCEL_ACTIVITY;
import static io.automatik.engine.workflow.process.executable.core.Metadata.SIGNAL_NAME;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TIME_CYCLE;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TIME_DATE;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TIME_DURATION;
import static io.automatik.engine.workflow.process.executable.core.Metadata.UNIQUE_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.definition.process.NodeContainer;
import io.automatik.engine.workflow.base.core.context.exception.ActionExceptionHandler;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionHandler;
import io.automatik.engine.workflow.base.core.context.swimlane.Swimlane;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.datatype.DataType;
import io.automatik.engine.workflow.base.core.event.EventFilter;
import io.automatik.engine.workflow.base.core.event.EventTypeFilter;
import io.automatik.engine.workflow.base.core.timer.Timer;
import io.automatik.engine.workflow.base.core.validation.ProcessValidationError;
import io.automatik.engine.workflow.base.instance.impl.Action;
import io.automatik.engine.workflow.base.instance.impl.actions.CancelNodeInstanceAction;
import io.automatik.engine.workflow.process.core.ProcessAction;
import io.automatik.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatik.engine.workflow.process.core.node.CompositeNode;
import io.automatik.engine.workflow.process.core.node.EventNode;
import io.automatik.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatik.engine.workflow.process.core.node.EventTrigger;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.core.node.StateBasedNode;
import io.automatik.engine.workflow.process.core.node.Trigger;
import io.automatik.engine.workflow.process.executable.core.validation.ExecutableProcessValidator;

public class ExecutableProcessFactory extends ExecutableNodeContainerFactory {

	public static final String METHOD_NAME = "name";
	public static final String METHOD_PACKAGE_NAME = "packageName";
	public static final String METHOD_DYNAMIC = "dynamic";
	public static final String METHOD_VERSION = "version";
	public static final String METHOD_VISIBILITY = "visibility";
	public static final String METHOD_VALIDATE = "validate";
	public static final String METHOD_IMPORTS = "imports";
	public static final String METHOD_GLOBAL = "global";
	public static final String METHOD_VARIABLE = "variable";

	private static final Logger logger = LoggerFactory.getLogger(ExecutableProcessFactory.class);

	public static ExecutableProcessFactory createProcess(String id) {
		return new ExecutableProcessFactory(id);
	}

	protected ExecutableProcessFactory(String id) {
		ExecutableProcess process = new ExecutableProcess();
		process.setId(id);
		process.setAutoComplete(true);
		setNodeContainer(process);
	}

	protected ExecutableProcess getRuleFlowProcess() {
		return (ExecutableProcess) getNodeContainer();
	}

	public ExecutableProcessFactory name(String name) {
		getRuleFlowProcess().setName(name);
		return this;
	}

	public ExecutableProcessFactory visibility(String visibility) {
		getRuleFlowProcess().setVisibility(visibility);
		return this;
	}

	public ExecutableProcessFactory dynamic(boolean dynamic) {
		getRuleFlowProcess().setDynamic(dynamic);
		if (dynamic) {
			getRuleFlowProcess().setAutoComplete(false);
		}
		return this;
	}

	public ExecutableProcessFactory version(String version) {
		getRuleFlowProcess().setVersion(version);
		return this;
	}

	public ExecutableProcessFactory packageName(String packageName) {
		getRuleFlowProcess().setPackageName(packageName);
		return this;
	}

	public ExecutableProcessFactory imports(String... imports) {
		getRuleFlowProcess().addImports(Arrays.asList(imports));
		return this;
	}

	public ExecutableProcessFactory functionImports(String... functionImports) {
		getRuleFlowProcess().addFunctionImports(Arrays.asList(functionImports));
		return this;
	}

	public ExecutableProcessFactory globals(Map<String, String> globals) {
		getRuleFlowProcess().setGlobals(globals);
		return this;
	}

	public ExecutableProcessFactory global(String name, String type) {
		Map<String, String> globals = getRuleFlowProcess().getGlobals();
		if (globals == null) {
			globals = new HashMap<String, String>();
			getRuleFlowProcess().setGlobals(globals);
		}
		globals.put(name, type);
		return this;
	}

	public ExecutableProcessFactory variable(String name, DataType type) {
		return variable(name, type, null);
	}

	public ExecutableProcessFactory variable(String name, DataType type, Object value) {
		return variable(name, type, value, null, null);
	}

	public ExecutableProcessFactory variable(String name, DataType type, String metaDataName, Object metaDataValue) {
		return variable(name, type, null, metaDataName, metaDataValue);
	}

	public ExecutableProcessFactory variable(String name, DataType type, Object value, String metaDataName,
			Object metaDataValue) {
		Variable variable = new Variable();
		variable.setName(name);
		variable.setType(type);
		variable.setValue(value);
		if (metaDataName != null && metaDataValue != null) {
			variable.setMetaData(metaDataName, metaDataValue);
		}
		getRuleFlowProcess().getVariableScope().getVariables().add(variable);
		return this;
	}

	public ExecutableProcessFactory swimlane(String name) {
		Swimlane swimlane = new Swimlane();
		swimlane.setName(name);
		getRuleFlowProcess().getSwimlaneContext().addSwimlane(swimlane);
		return this;
	}

	public ExecutableProcessFactory exceptionHandler(String exception, ExceptionHandler exceptionHandler) {
		getRuleFlowProcess().getExceptionScope().setExceptionHandler(exception, exceptionHandler);
		return this;
	}

	public ExecutableProcessFactory exceptionHandler(String exception, String dialect, String action) {
		ActionExceptionHandler exceptionHandler = new ActionExceptionHandler();
		exceptionHandler.setAction(new ConsequenceAction(dialect, action));
		return exceptionHandler(exception, exceptionHandler);
	}

	public ExecutableProcessFactory metaData(String name, Object value) {
		getRuleFlowProcess().setMetaData(name, value);
		return this;
	}

	public ExecutableProcessFactory validate() {
		link();
		ProcessValidationError[] errors = ExecutableProcessValidator.getInstance()
				.validateProcess(getRuleFlowProcess());
		for (ProcessValidationError error : errors) {
			logger.error(error.toString());
		}
		if (errors.length > 0) {
			throw new RuntimeException("Process could not be validated !");
		}
		return this;
	}

	public ExecutableProcessFactory link() {
		ExecutableProcess process = getRuleFlowProcess();
		linkBoundaryEvents(process);
		postProcessNodes(process, process);
		return this;
	}

	public ExecutableProcessFactory done() {
		throw new IllegalArgumentException("Already on the top-level.");
	}

	public ExecutableProcess getProcess() {
		return getRuleFlowProcess();
	}

	@Override
	public ExecutableProcessFactory connection(long fromId, long toId) {
		super.connection(fromId, toId);
		return this;
	}

	@Override
	public ExecutableProcessFactory connection(long fromId, long toId, String uniqueId) {
		super.connection(fromId, toId, uniqueId);
		return this;
	}

	protected void linkBoundaryEvents(NodeContainer nodeContainer) {
		for (Node node : nodeContainer.getNodes()) {
			if (node instanceof CompositeNode) {
				CompositeNode compositeNode = (CompositeNode) node;
				linkBoundaryEvents(compositeNode.getNodeContainer());
			}
			if (node instanceof EventNode) {
				final String attachedTo = (String) node.getMetaData().get(ATTACHED_TO);
				if (attachedTo != null) {
					Node attachedNode = findNodeByIdOrUniqueIdInMetadata(nodeContainer, attachedTo,
							"Could not find node to attach to: " + attachedTo);
					for (EventFilter filter : ((EventNode) node).getEventFilters()) {
						String type = ((EventTypeFilter) filter).getType();
						if (type.startsWith("Timer-")) {
							linkBoundaryTimerEvent(node, attachedTo, attachedNode);
						} else if (node.getMetaData().get(SIGNAL_NAME) != null || type.startsWith("Message-")) {
							linkBoundarySignalEvent(node, attachedTo);
						}
					}
				}
			}
		}
	}

	protected void linkBoundaryTimerEvent(Node node, String attachedTo, Node attachedNode) {
		boolean cancelActivity = (Boolean) node.getMetaData().get(CANCEL_ACTIVITY);
		StateBasedNode compositeNode = (StateBasedNode) attachedNode;
		String timeDuration = (String) node.getMetaData().get(TIME_DURATION);
		String timeCycle = (String) node.getMetaData().get(TIME_CYCLE);
		String timeDate = (String) node.getMetaData().get(TIME_DATE);
		Timer timer = new Timer();
		if (timeDuration != null) {
			timer.setDelay(timeDuration);
			timer.setTimeType(Timer.TIME_DURATION);
			compositeNode.addTimer(timer, timerAction("Timer-" + attachedTo + "-" + timeDuration + "-" + node.getId()));
		} else if (timeCycle != null) {
			int index = timeCycle.indexOf("###");
			if (index != -1) {
				String period = timeCycle.substring(index + 3);
				timeCycle = timeCycle.substring(0, index);
				timer.setPeriod(period);
			}
			timer.setDelay(timeCycle);
			timer.setTimeType(Timer.TIME_CYCLE);
			compositeNode.addTimer(timer, timerAction("Timer-" + attachedTo + "-" + timeCycle
					+ (timer.getPeriod() == null ? "" : "###" + timer.getPeriod()) + "-" + node.getId()));
		} else if (timeDate != null) {
			timer.setDate(timeDate);
			timer.setTimeType(Timer.TIME_DATE);
			compositeNode.addTimer(timer, timerAction("Timer-" + attachedTo + "-" + timeDate + "-" + node.getId()));
		}

		if (cancelActivity) {
			List<ProcessAction> actions = ((EventNode) node).getActions(EVENT_NODE_EXIT);
			if (actions == null) {
				actions = new ArrayList<>();
			}
			ConsequenceAction cancelAction = new ConsequenceAction("java", null);
			cancelAction.setMetaData(ACTION, new CancelNodeInstanceAction(attachedTo));
			actions.add(cancelAction);
			((EventNode) node).setActions(EVENT_NODE_EXIT, actions);
		}
	}

	protected void linkBoundarySignalEvent(Node node, String attachedTo) {
		boolean cancelActivity = (Boolean) node.getMetaData().get(CANCEL_ACTIVITY);
		if (cancelActivity) {
			List<ProcessAction> actions = ((EventNode) node).getActions(EVENT_NODE_EXIT);
			if (actions == null) {
				actions = new ArrayList<>();
			}
			ConsequenceAction action = new ConsequenceAction("java", null);
			action.setMetaData(ACTION, new CancelNodeInstanceAction(attachedTo));
			actions.add(action);
			((EventNode) node).setActions(EVENT_NODE_EXIT, actions);
		}
	}

	protected ProcessAction timerAction(String type) {
		ProcessAction signal = new ProcessAction();

		Action action = kcontext -> kcontext.getProcessInstance().signalEvent(type, kcontext.getNodeInstance().getId());
		signal.wire(action);

		return signal;
	}

	protected Node findNodeByIdOrUniqueIdInMetadata(NodeContainer nodeContainer, final String nodeRef,
			String errorMsg) {
		Node node = null;
		// try looking for a node with same "UniqueId" (in metadata)
		for (Node containerNode : nodeContainer.getNodes()) {
			if (nodeRef.equals(containerNode.getMetaData().get(UNIQUE_ID))) {
				node = containerNode;
				break;
			}
		}
		if (node == null) {
			throw new IllegalArgumentException(errorMsg);
		}
		return node;
	}

	private void postProcessNodes(ExecutableProcess process, NodeContainer container) {

		for (Node node : container.getNodes()) {
			if (node instanceof NodeContainer) {
				// prepare event sub process
				if (node instanceof EventSubProcessNode) {
					EventSubProcessNode eventSubProcessNode = (EventSubProcessNode) node;

					Node[] nodes = eventSubProcessNode.getNodes();
					for (Node subNode : nodes) {
						// avoids cyclomatic complexity
						if (subNode instanceof StartNode) {

							processEventSubprocessStartNode(((StartNode) subNode), eventSubProcessNode);
						}
					}
				}
				postProcessNodes(process, (NodeContainer) node);
			}
		}
	}

	private void processEventSubprocessStartNode(StartNode subNode, EventSubProcessNode eventSubProcessNode) {
		List<Trigger> triggers = subNode.getTriggers();
		if (triggers != null) {

			for (Trigger trigger : triggers) {
				if (trigger instanceof EventTrigger) {
					final List<EventFilter> filters = ((EventTrigger) trigger).getEventFilters();

					for (EventFilter filter : filters) {
						eventSubProcessNode.addEvent((EventTypeFilter) filter);
					}
				}
			}
		}
	}
}
