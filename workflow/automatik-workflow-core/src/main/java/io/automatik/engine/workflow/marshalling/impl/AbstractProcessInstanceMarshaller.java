
package io.automatik.engine.workflow.marshalling.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.marshalling.ObjectMarshallingStrategy;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.NodeInstanceContainer;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatik.engine.workflow.base.core.Context;
import io.automatik.engine.workflow.base.core.context.exclusive.ExclusiveGroup;
import io.automatik.engine.workflow.base.core.context.swimlane.SwimlaneContext;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.instance.ContextInstance;
import io.automatik.engine.workflow.base.instance.context.exclusive.ExclusiveGroupInstance;
import io.automatik.engine.workflow.base.instance.context.swimlane.SwimlaneContextInstance;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceImpl;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatik.engine.workflow.process.instance.node.CompositeContextNodeInstance;
import io.automatik.engine.workflow.process.instance.node.DynamicNodeInstance;
import io.automatik.engine.workflow.process.instance.node.EventNodeInstance;
import io.automatik.engine.workflow.process.instance.node.ForEachNodeInstance;
import io.automatik.engine.workflow.process.instance.node.HumanTaskNodeInstance;
import io.automatik.engine.workflow.process.instance.node.JoinInstance;
import io.automatik.engine.workflow.process.instance.node.MilestoneNodeInstance;
import io.automatik.engine.workflow.process.instance.node.RuleSetNodeInstance;
import io.automatik.engine.workflow.process.instance.node.StateNodeInstance;
import io.automatik.engine.workflow.process.instance.node.SubProcessNodeInstance;
import io.automatik.engine.workflow.process.instance.node.TimerNodeInstance;
import io.automatik.engine.workflow.process.instance.node.WorkItemNodeInstance;
import io.automatik.engine.workflow.util.StringUtils;

/**
 * Default implementation of a process instance marshaller.
 * 
 */
public abstract class AbstractProcessInstanceMarshaller implements ProcessInstanceMarshaller {

	// Output methods
	public Object writeProcessInstance(MarshallerWriteContext context, ProcessInstance processInstance)
			throws IOException {
		WorkflowProcessInstanceImpl workFlow = (WorkflowProcessInstanceImpl) processInstance;
		ObjectOutputStream stream = context.stream;
		stream.writeUTF(workFlow.getId());
		stream.writeUTF(workFlow.getProcessId());
		stream.writeInt(workFlow.getState());

		SwimlaneContextInstance swimlaneContextInstance = (SwimlaneContextInstance) workFlow
				.getContextInstance(SwimlaneContext.SWIMLANE_SCOPE);
		if (swimlaneContextInstance != null) {
			Map<String, String> swimlaneActors = swimlaneContextInstance.getSwimlaneActors();
			stream.writeInt(swimlaneActors.size());
			for (Map.Entry<String, String> entry : swimlaneActors.entrySet()) {
				stream.writeUTF(entry.getKey());
				stream.writeUTF(entry.getValue());
			}
		} else {
			stream.writeInt(0);
		}

		List<NodeInstance> nodeInstances = new ArrayList<NodeInstance>(workFlow.getNodeInstances());
		Collections.sort(nodeInstances, new Comparator<NodeInstance>() {

			public int compare(NodeInstance o1, NodeInstance o2) {
				return (int) (o1.getId().compareTo(o2.getId()));
			}
		});
		for (NodeInstance nodeInstance : nodeInstances) {
			stream.writeShort(PersisterEnums.NODE_INSTANCE);
			writeNodeInstance(context, nodeInstance);
		}
		stream.writeShort(PersisterEnums.END);

		List<ContextInstance> exclusiveGroupInstances = workFlow.getContextInstances(ExclusiveGroup.EXCLUSIVE_GROUP);
		if (exclusiveGroupInstances == null) {
			stream.writeInt(0);
		} else {
			stream.writeInt(exclusiveGroupInstances.size());
			for (ContextInstance contextInstance : exclusiveGroupInstances) {
				ExclusiveGroupInstance exclusiveGroupInstance = (ExclusiveGroupInstance) contextInstance;
				Collection<NodeInstance> groupNodeInstances = exclusiveGroupInstance.getNodeInstances();
				stream.writeInt(groupNodeInstances.size());
				for (NodeInstance nodeInstance : groupNodeInstances) {
					stream.writeUTF(nodeInstance.getId());
				}
			}
		}
		VariableScopeInstance variableScopeInstance = (VariableScopeInstance) workFlow
				.getContextInstance(VariableScope.VARIABLE_SCOPE);
		Map<String, Object> variables = variableScopeInstance.getVariables();
		List<String> keys = new ArrayList<String>(variables.keySet());
		Collection<Object> values = variables.values();

		Collections.sort(keys, new Comparator<String>() {

			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});
		// Process Variables
		// - Number of non null Variables = nonnullvariables.size()
		// For Each Variable
		// - Variable Key
		// - Marshalling Strategy Index
		// - Marshalled Object

		Collection<Object> notNullValues = new ArrayList<Object>();
		for (Object value : values) {
			if (value != null) {
				notNullValues.add(value);
			}
		}

		stream.writeInt(notNullValues.size());
		for (String key : keys) {
			Object object = variables.get(key);
			if (object != null) {
				stream.writeUTF(key);
				// New marshalling algorithm when using strategies
				int useNewMarshallingStrategyAlgorithm = -2;
				stream.writeInt(useNewMarshallingStrategyAlgorithm);
				// Choose first strategy that accepts the object (what was always done)
				ObjectMarshallingStrategy strategy = context.objectMarshallingStrategyStore.getStrategyObject(object);
				stream.writeUTF(strategy.getClass().getName());
				strategy.write(stream, object);
			}

		}
		return null;

	}

	public Object writeNodeInstance(MarshallerWriteContext context, NodeInstance nodeInstance) throws IOException {
		ObjectOutputStream stream = context.stream;
		stream.writeUTF(nodeInstance.getId());
		stream.writeLong(nodeInstance.getNodeId());
		writeNodeInstanceContent(stream, nodeInstance, context);
		return null;
	}

	protected void writeNodeInstanceContent(ObjectOutputStream stream, NodeInstance nodeInstance,
			MarshallerWriteContext context) throws IOException {
		if (nodeInstance instanceof RuleSetNodeInstance) {
			stream.writeShort(PersisterEnums.RULE_SET_NODE_INSTANCE);
			List<String> timerInstances = ((RuleSetNodeInstance) nodeInstance).getTimerInstances();
			if (timerInstances != null) {
				stream.writeInt(timerInstances.size());
				for (String id : timerInstances) {
					stream.writeUTF(id);
				}
			} else {
				stream.writeInt(0);
			}
		} else if (nodeInstance instanceof HumanTaskNodeInstance) {
			stream.writeShort(PersisterEnums.HUMAN_TASK_NODE_INSTANCE);
			stream.writeUTF(((HumanTaskNodeInstance) nodeInstance).getWorkItemId());
			List<String> timerInstances = ((HumanTaskNodeInstance) nodeInstance).getTimerInstances();
			if (timerInstances != null) {
				stream.writeInt(timerInstances.size());
				for (String id : timerInstances) {
					stream.writeUTF(id);
				}
			} else {
				stream.writeInt(0);
			}
		} else if (nodeInstance instanceof WorkItemNodeInstance) {
			stream.writeShort(PersisterEnums.WORK_ITEM_NODE_INSTANCE);
			stream.writeUTF(((WorkItemNodeInstance) nodeInstance).getWorkItemId());
			List<String> timerInstances = ((WorkItemNodeInstance) nodeInstance).getTimerInstances();
			if (timerInstances != null) {
				stream.writeInt(timerInstances.size());
				for (String id : timerInstances) {
					stream.writeUTF(id);
				}
			} else {
				stream.writeInt(0);
			}
		} else if (nodeInstance instanceof SubProcessNodeInstance) {
			stream.writeShort(PersisterEnums.SUB_PROCESS_NODE_INSTANCE);
			stream.writeUTF(((SubProcessNodeInstance) nodeInstance).getProcessInstanceId());
			List<String> timerInstances = ((SubProcessNodeInstance) nodeInstance).getTimerInstances();
			if (timerInstances != null) {
				stream.writeInt(timerInstances.size());
				for (String id : timerInstances) {
					stream.writeUTF(id);
				}
			} else {
				stream.writeInt(0);
			}
		} else if (nodeInstance instanceof MilestoneNodeInstance) {
			stream.writeShort(PersisterEnums.MILESTONE_NODE_INSTANCE);
			List<String> timerInstances = ((MilestoneNodeInstance) nodeInstance).getTimerInstances();
			if (timerInstances != null) {
				stream.writeInt(timerInstances.size());
				for (String id : timerInstances) {
					stream.writeUTF(id);
				}
			} else {
				stream.writeInt(0);
			}
		} else if (nodeInstance instanceof EventNodeInstance) {
			stream.writeShort(PersisterEnums.EVENT_NODE_INSTANCE);
		} else if (nodeInstance instanceof TimerNodeInstance) {
			stream.writeShort(PersisterEnums.TIMER_NODE_INSTANCE);
			stream.writeUTF(((TimerNodeInstance) nodeInstance).getTimerId());
		} else if (nodeInstance instanceof JoinInstance) {
			stream.writeShort(PersisterEnums.JOIN_NODE_INSTANCE);
			Map<Long, Integer> triggers = ((JoinInstance) nodeInstance).getTriggers();
			stream.writeInt(triggers.size());
			List<Long> keys = new ArrayList<Long>(triggers.keySet());
			Collections.sort(keys, new Comparator<Long>() {

				public int compare(Long o1, Long o2) {
					return o1.compareTo(o2);
				}
			});
			for (Long key : keys) {
				stream.writeLong(key);
				stream.writeInt(triggers.get(key));
			}
		} else if (nodeInstance instanceof StateNodeInstance) {
			stream.writeShort(PersisterEnums.STATE_NODE_INSTANCE);
			List<String> timerInstances = ((StateNodeInstance) nodeInstance).getTimerInstances();
			if (timerInstances != null) {
				stream.writeInt(timerInstances.size());
				for (String id : timerInstances) {
					stream.writeUTF(id);
				}
			} else {
				stream.writeInt(0);
			}
		} else if (nodeInstance instanceof CompositeContextNodeInstance) {
			if (nodeInstance instanceof DynamicNodeInstance) {
				stream.writeShort(PersisterEnums.DYNAMIC_NODE_INSTANCE);
			} else {
				stream.writeShort(PersisterEnums.COMPOSITE_NODE_INSTANCE);
			}
			CompositeContextNodeInstance compositeNodeInstance = (CompositeContextNodeInstance) nodeInstance;
			List<String> timerInstances = ((CompositeContextNodeInstance) nodeInstance).getTimerInstances();
			if (timerInstances != null) {
				stream.writeInt(timerInstances.size());
				for (String id : timerInstances) {
					stream.writeUTF(id);
				}
			} else {
				stream.writeInt(0);
			}
			VariableScopeInstance variableScopeInstance = (VariableScopeInstance) compositeNodeInstance
					.getContextInstance(VariableScope.VARIABLE_SCOPE);
			if (variableScopeInstance == null) {
				stream.writeInt(0);
			} else {
				Map<String, Object> variables = variableScopeInstance.getVariables();
				List<String> keys = new ArrayList<String>(variables.keySet());
				Collections.sort(keys, new Comparator<String>() {
					public int compare(String o1, String o2) {
						return o1.compareTo(o2);
					}
				});
				stream.writeInt(keys.size());
				for (String key : keys) {
					stream.writeUTF(key);
					stream.writeObject(variables.get(key));
				}
			}
			List<NodeInstance> nodeInstances = new ArrayList<NodeInstance>(compositeNodeInstance.getNodeInstances());
			Collections.sort(nodeInstances, new Comparator<NodeInstance>() {

				public int compare(NodeInstance o1, NodeInstance o2) {
					return (int) (o1.getId().compareTo(o2.getId()));
				}
			});
			for (NodeInstance subNodeInstance : nodeInstances) {
				stream.writeShort(PersisterEnums.NODE_INSTANCE);
				writeNodeInstance(context, subNodeInstance);
			}
			stream.writeShort(PersisterEnums.END);
			List<ContextInstance> exclusiveGroupInstances = compositeNodeInstance
					.getContextInstances(ExclusiveGroup.EXCLUSIVE_GROUP);
			if (exclusiveGroupInstances == null) {
				stream.writeInt(0);
			} else {
				stream.writeInt(exclusiveGroupInstances.size());
				for (ContextInstance contextInstance : exclusiveGroupInstances) {
					ExclusiveGroupInstance exclusiveGroupInstance = (ExclusiveGroupInstance) contextInstance;
					Collection<NodeInstance> groupNodeInstances = exclusiveGroupInstance.getNodeInstances();
					stream.writeInt(groupNodeInstances.size());
					for (NodeInstance groupNodeInstance : groupNodeInstances) {
						stream.writeUTF(groupNodeInstance.getId());
					}
				}
			}
		} else if (nodeInstance instanceof ForEachNodeInstance) {
			stream.writeShort(PersisterEnums.FOR_EACH_NODE_INSTANCE);
			ForEachNodeInstance forEachNodeInstance = (ForEachNodeInstance) nodeInstance;
			List<NodeInstance> nodeInstances = new ArrayList<NodeInstance>(forEachNodeInstance.getNodeInstances());
			Collections.sort(nodeInstances, new Comparator<NodeInstance>() {
				public int compare(NodeInstance o1, NodeInstance o2) {
					return (int) (o1.getId().compareTo(o2.getId()));
				}
			});
			for (NodeInstance subNodeInstance : nodeInstances) {
				if (subNodeInstance instanceof CompositeContextNodeInstance) {
					stream.writeShort(PersisterEnums.NODE_INSTANCE);
					writeNodeInstance(context, subNodeInstance);
				}
			}
			stream.writeShort(PersisterEnums.END);
		} else {
			throw new IllegalArgumentException("Unknown node instance type: " + nodeInstance);
		}
	}

	// Input methods
	public ProcessInstance readProcessInstance(MarshallerReaderContext context) throws IOException {
		ObjectInputStream stream = context.stream;

		WorkflowProcessInstanceImpl processInstance = createProcessInstance();
		processInstance.setId(stream.readUTF());
		String processId = stream.readUTF();
		processInstance.setProcessId(processId);
		Process process = context.getProcessRuntime().getProcess(processId);
		if (process != null) {
			processInstance.setProcess(process);
		}
		processInstance.setState(stream.readInt());
		long nodeInstanceCounter = stream.readLong();
		processInstance.setProcessRuntime(context.getProcessRuntime());

		int nbSwimlanes = stream.readInt();
		if (nbSwimlanes > 0) {
			Context swimlaneContext = ((io.automatik.engine.workflow.base.core.Process) process)
					.getDefaultContext(SwimlaneContext.SWIMLANE_SCOPE);
			SwimlaneContextInstance swimlaneContextInstance = (SwimlaneContextInstance) processInstance
					.getContextInstance(swimlaneContext);
			for (int i = 0; i < nbSwimlanes; i++) {
				String name = stream.readUTF();
				String value = stream.readUTF();
				swimlaneContextInstance.setActorId(name, value);
			}
		}

		while (stream.readShort() == PersisterEnums.NODE_INSTANCE) {
			readNodeInstance(context, processInstance, processInstance);
		}

		int exclusiveGroupInstances = stream.readInt();
		for (int i = 0; i < exclusiveGroupInstances; i++) {
			ExclusiveGroupInstance exclusiveGroupInstance = new ExclusiveGroupInstance();
			processInstance.addContextInstance(ExclusiveGroup.EXCLUSIVE_GROUP, exclusiveGroupInstance);
			int nodeInstances = stream.readInt();
			for (int j = 0; j < nodeInstances; j++) {
				String nodeInstanceId = stream.readUTF();
				NodeInstance nodeInstance = processInstance.getNodeInstance(nodeInstanceId);
				if (nodeInstance == null) {
					throw new IllegalArgumentException(
							"Could not find node instance when deserializing exclusive group instance: "
									+ nodeInstanceId);
				}
				exclusiveGroupInstance.addNodeInstance(nodeInstance);
			}
		}

		// Process Variables
		// - Number of Variables = keys.size()
		// For Each Variable
		// - Variable Key
		// - Marshalling Strategy Index
		// - Marshalled Object
		int nbVariables = stream.readInt();
		if (nbVariables > 0) {
			Context variableScope = ((io.automatik.engine.workflow.base.core.Process) process)
					.getDefaultContext(VariableScope.VARIABLE_SCOPE);
			VariableScopeInstance variableScopeInstance = (VariableScopeInstance) processInstance
					.getContextInstance(variableScope);
			for (int i = 0; i < nbVariables; i++) {
				String name = stream.readUTF();
				try {
					ObjectMarshallingStrategy strategy = null;
					int index = stream.readInt();
					// This is the old way of de/serializing strategy objects
					if (index >= 0) {
						strategy = context.resolverStrategyFactory.getStrategy(index);
					}
					// This is the new way
					else if (index == -2) {
						String strategyClassName = context.stream.readUTF();
						if (!StringUtils.isEmpty(strategyClassName)) {
							strategy = context.resolverStrategyFactory.getStrategyObject(strategyClassName);
							if (strategy == null) {
								throw new IllegalStateException(
										"No strategy of type " + strategyClassName + " available.");
							}
						}
					}
					// If either way retrieves a strategy, use it
					Object value = null;
					if (strategy != null) {
						value = strategy.read(stream);
					}
					variableScopeInstance.internalSetVariable(name, value);
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException("Could not reload variable " + name);
				}
			}
		}
		if (context.getProcessRuntime() != null) {
			processInstance.reconnect();
		}
		return processInstance;
	}

	protected abstract WorkflowProcessInstanceImpl createProcessInstance();

	public NodeInstance readNodeInstance(MarshallerReaderContext context, NodeInstanceContainer nodeInstanceContainer,
			WorkflowProcessInstance processInstance) throws IOException {
		ObjectInputStream stream = context.stream;
		String id = stream.readUTF();
		long nodeId = stream.readLong();
		int nodeType = stream.readShort();
		NodeInstanceImpl nodeInstance = readNodeInstanceContent(nodeType, stream, context, processInstance);

		nodeInstance.setNodeId(nodeId);
		nodeInstance.setNodeInstanceContainer(nodeInstanceContainer);
		nodeInstance.setProcessInstance(
				(io.automatik.engine.workflow.process.instance.WorkflowProcessInstance) processInstance);
		nodeInstance.setId(id);

		switch (nodeType) {
		case PersisterEnums.COMPOSITE_NODE_INSTANCE:
		case PersisterEnums.DYNAMIC_NODE_INSTANCE:
			int nbVariables = stream.readInt();
			if (nbVariables > 0) {
				Context variableScope = ((io.automatik.engine.workflow.base.core.Process) ((io.automatik.engine.workflow.base.instance.ProcessInstance) processInstance)
						.getProcess()).getDefaultContext(VariableScope.VARIABLE_SCOPE);
				VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((CompositeContextNodeInstance) nodeInstance)
						.getContextInstance(variableScope);
				for (int i = 0; i < nbVariables; i++) {
					String name = stream.readUTF();
					try {
						Object value = stream.readObject();
						variableScopeInstance.internalSetVariable(name, value);
					} catch (ClassNotFoundException e) {
						throw new IllegalArgumentException("Could not reload variable " + name);
					}
				}
			}
			while (stream.readShort() == PersisterEnums.NODE_INSTANCE) {
				readNodeInstance(context, (CompositeContextNodeInstance) nodeInstance, processInstance);
			}

			int exclusiveGroupInstances = stream.readInt();
			for (int i = 0; i < exclusiveGroupInstances; i++) {
				ExclusiveGroupInstance exclusiveGroupInstance = new ExclusiveGroupInstance();
				((io.automatik.engine.workflow.base.instance.ProcessInstance) processInstance)
						.addContextInstance(ExclusiveGroup.EXCLUSIVE_GROUP, exclusiveGroupInstance);
				int nodeInstances = stream.readInt();
				for (int j = 0; j < nodeInstances; j++) {
					String nodeInstanceId = stream.readUTF();
					NodeInstance groupNodeInstance = processInstance.getNodeInstance(nodeInstanceId);
					if (groupNodeInstance == null) {
						throw new IllegalArgumentException(
								"Could not find node instance when deserializing exclusive group instance: "
										+ nodeInstanceId);
					}
					exclusiveGroupInstance.addNodeInstance(groupNodeInstance);
				}
			}

			break;
		case PersisterEnums.FOR_EACH_NODE_INSTANCE:
			while (stream.readShort() == PersisterEnums.NODE_INSTANCE) {
				readNodeInstance(context, (ForEachNodeInstance) nodeInstance, processInstance);
			}
			break;
		default:
			// do nothing
		}

		return nodeInstance;
	}

	protected NodeInstanceImpl readNodeInstanceContent(int nodeType, ObjectInputStream stream,
			MarshallerReaderContext context, WorkflowProcessInstance processInstance) throws IOException {
		NodeInstanceImpl nodeInstance = null;
		switch (nodeType) {
		case PersisterEnums.RULE_SET_NODE_INSTANCE:
			nodeInstance = new RuleSetNodeInstance();
			int nbTimerInstances = stream.readInt();
			if (nbTimerInstances > 0) {
				List<String> timerInstances = new ArrayList<>();
				for (int i = 0; i < nbTimerInstances; i++) {
					timerInstances.add(stream.readUTF());
				}
				((RuleSetNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
			}
			break;
		case PersisterEnums.HUMAN_TASK_NODE_INSTANCE:
			nodeInstance = new HumanTaskNodeInstance();
			((HumanTaskNodeInstance) nodeInstance).internalSetWorkItemId(stream.readUTF());
			nbTimerInstances = stream.readInt();
			if (nbTimerInstances > 0) {
				List<String> timerInstances = new ArrayList<>();
				for (int i = 0; i < nbTimerInstances; i++) {
					timerInstances.add(stream.readUTF());
				}
				((HumanTaskNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
			}
			break;
		case PersisterEnums.WORK_ITEM_NODE_INSTANCE:
			nodeInstance = new WorkItemNodeInstance();
			((WorkItemNodeInstance) nodeInstance).internalSetWorkItemId(stream.readUTF());
			nbTimerInstances = stream.readInt();
			if (nbTimerInstances > 0) {
				List<String> timerInstances = new ArrayList<>();
				for (int i = 0; i < nbTimerInstances; i++) {
					timerInstances.add(stream.readUTF());
				}
				((WorkItemNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
			}
			break;
		case PersisterEnums.SUB_PROCESS_NODE_INSTANCE:
			nodeInstance = new SubProcessNodeInstance();
			((SubProcessNodeInstance) nodeInstance).internalSetProcessInstanceId(stream.readUTF());
			nbTimerInstances = stream.readInt();
			if (nbTimerInstances > 0) {
				List<String> timerInstances = new ArrayList<>();
				for (int i = 0; i < nbTimerInstances; i++) {
					timerInstances.add(stream.readUTF());
				}
				((SubProcessNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
			}
			break;
		case PersisterEnums.MILESTONE_NODE_INSTANCE:
			nodeInstance = new MilestoneNodeInstance();
			nbTimerInstances = stream.readInt();
			if (nbTimerInstances > 0) {
				List<String> timerInstances = new ArrayList<>();
				for (int i = 0; i < nbTimerInstances; i++) {
					timerInstances.add(stream.readUTF());
				}
				((MilestoneNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
			}
			break;
		case PersisterEnums.TIMER_NODE_INSTANCE:
			nodeInstance = new TimerNodeInstance();
			((TimerNodeInstance) nodeInstance).internalSetTimerId(stream.readUTF());
			break;
		case PersisterEnums.EVENT_NODE_INSTANCE:
			nodeInstance = new EventNodeInstance();
			break;
		case PersisterEnums.JOIN_NODE_INSTANCE:
			nodeInstance = new JoinInstance();
			int number = stream.readInt();
			if (number > 0) {
				Map<Long, Integer> triggers = new HashMap<Long, Integer>();
				for (int i = 0; i < number; i++) {
					long l = stream.readLong();
					int count = stream.readInt();
					triggers.put(l, count);
				}
				((JoinInstance) nodeInstance).internalSetTriggers(triggers);
			}
			break;
		case PersisterEnums.COMPOSITE_NODE_INSTANCE:
			nodeInstance = new CompositeContextNodeInstance();
			nbTimerInstances = stream.readInt();
			if (nbTimerInstances > 0) {
				List<String> timerInstances = new ArrayList<>();
				for (int i = 0; i < nbTimerInstances; i++) {
					timerInstances.add(stream.readUTF());
				}
				((CompositeContextNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
			}
			break;
		case PersisterEnums.FOR_EACH_NODE_INSTANCE:
			nodeInstance = new ForEachNodeInstance();
			break;
		case PersisterEnums.DYNAMIC_NODE_INSTANCE:
			nodeInstance = new DynamicNodeInstance();
			nbTimerInstances = stream.readInt();
			if (nbTimerInstances > 0) {
				List<String> timerInstances = new ArrayList<>();
				for (int i = 0; i < nbTimerInstances; i++) {
					timerInstances.add(stream.readUTF());
				}
				((CompositeContextNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
			}
			break;
		case PersisterEnums.STATE_NODE_INSTANCE:
			nodeInstance = new StateNodeInstance();
			nbTimerInstances = stream.readInt();
			if (nbTimerInstances > 0) {
				List<String> timerInstances = new ArrayList<>();
				for (int i = 0; i < nbTimerInstances; i++) {
					timerInstances.add(stream.readUTF());
				}
				((CompositeContextNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown node type: " + nodeType);
		}
		return nodeInstance;

	}
}
