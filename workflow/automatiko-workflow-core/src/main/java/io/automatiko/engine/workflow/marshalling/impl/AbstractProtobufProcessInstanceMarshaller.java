
package io.automatiko.engine.workflow.marshalling.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.protobuf.ExtensionRegistry;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.runtime.process.HumanTaskWorkItem;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.NodeInstanceContainer;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.Tag;
import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.core.context.exclusive.ExclusiveGroup;
import io.automatiko.engine.workflow.base.core.context.swimlane.SwimlaneContext;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.ContextInstance;
import io.automatiko.engine.workflow.base.instance.context.exclusive.ExclusiveGroupInstance;
import io.automatiko.engine.workflow.base.instance.context.swimlane.SwimlaneContextInstance;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskWorkItemImpl;
import io.automatiko.engine.workflow.base.instance.impl.workitem.WorkItemImpl;
import io.automatiko.engine.workflow.marshalling.impl.AutomatikMessages.Header;
import io.automatiko.engine.workflow.marshalling.impl.AutomatikMessages.ProcessInstance.NodeInstanceContent;
import io.automatiko.engine.workflow.marshalling.impl.AutomatikMessages.ProcessInstance.NodeInstanceType;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceImpl;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatiko.engine.workflow.process.instance.node.CompositeContextNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.DynamicNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.EventNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.EventSubProcessNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.ForEachNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.HumanTaskNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.JoinInstance;
import io.automatiko.engine.workflow.process.instance.node.LambdaSubProcessNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.MilestoneNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.RuleSetNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.StateNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.SubProcessNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.TimerNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.WorkItemNodeInstance;

/**
 * Default implementation of a process instance marshaller.
 * 
 */
public abstract class AbstractProtobufProcessInstanceMarshaller implements ProcessInstanceMarshaller {

    // Output methods
    public AutomatikMessages.ProcessInstance writeProcessInstance(MarshallerWriteContext context,
            ProcessInstance processInstance) throws IOException {
        WorkflowProcessInstanceImpl workFlow = (WorkflowProcessInstanceImpl) processInstance;

        AutomatikMessages.ProcessInstance.Builder _instance = AutomatikMessages.ProcessInstance.newBuilder()
                .setId(workFlow.getId()).setProcessId(workFlow.getProcessId()).setState(workFlow.getState())
                .setProcessType(workFlow.getProcess().getType()).setSignalCompletion(workFlow.isSignalCompletion())
                .setSlaCompliance(workFlow.getSlaCompliance()).setStartDate(workFlow.getStartDate().getTime());
        if (workFlow.getProcessXml() != null) {
            _instance.setProcessXml(workFlow.getProcessXml());
        }
        if (workFlow.getDescription() != null) {
            _instance.setDescription(workFlow.getDescription());
        }
        if (workFlow.getInitiator() != null) {
            _instance.setInitiator(workFlow.getInitiator());
        }
        _instance.addAllCompletedNodeIds(workFlow.getCompletedNodeIds());
        if (workFlow.getCorrelationKey() != null) {
            _instance.setCorrelationKey(workFlow.getCorrelationKey());
        }
        if (workFlow.getSlaDueDate() != null) {
            _instance.setSlaDueDate(workFlow.getSlaDueDate().getTime());
        }
        if (workFlow.getSlaTimerId() != null) {
            _instance.setSlaTimerId(workFlow.getSlaTimerId());
        }
        if (workFlow.getParentProcessInstanceId() != null) {
            _instance.setParentProcessInstanceId(workFlow.getParentProcessInstanceId());
        }
        if (workFlow.getRootProcessInstanceId() != null) {
            _instance.setRootProcessInstanceId(workFlow.getRootProcessInstanceId());
        }
        if (workFlow.getRootProcessId() != null) {
            _instance.setRootProcessId(workFlow.getRootProcessId());
        }
        if (workFlow.getNodeIdInError() != null) {
            _instance.setErrorNodeId(workFlow.getNodeIdInError());
        }
        if (workFlow.getErrorMessage() != null) {
            _instance.setErrorMessage(workFlow.getErrorMessage());
        }
        if (workFlow.getReferenceId() != null) {
            _instance.setReferenceId(workFlow.getReferenceId());
        }

        Map<String, List<String>> children = workFlow.getChildren();
        if (children != null) {
            for (Entry<String, List<String>> entry : children.entrySet()) {
                _instance.addChildren(AutomatikMessages.ProcessInstance.ProcessInstanchChildren.newBuilder()
                        .setProcessId(entry.getKey()).addAllIds(entry.getValue()).build());

            }
        }

        Collection<Tag> tags = workFlow.getTags();
        if (tags != null) {
            for (Tag tag : tags) {
                _instance.addTags(
                        AutomatikMessages.ProcessInstance.Tag.newBuilder().setId(tag.getId()).setValue(tag.getValue()));
            }
        }

        SwimlaneContextInstance swimlaneContextInstance = (SwimlaneContextInstance) workFlow
                .getContextInstance(SwimlaneContext.SWIMLANE_SCOPE);
        if (swimlaneContextInstance != null) {
            Map<String, String> swimlaneActors = swimlaneContextInstance.getSwimlaneActors();
            for (Map.Entry<String, String> entry : swimlaneActors.entrySet()) {
                _instance.addSwimlaneContext(AutomatikMessages.ProcessInstance.SwimlaneContextInstance.newBuilder()
                        .setSwimlane(entry.getKey()).setActorId(entry.getValue()).build());
            }
        }

        List<NodeInstance> nodeInstances = new ArrayList<NodeInstance>(workFlow.getNodeInstances());
        Collections.sort(nodeInstances, new Comparator<NodeInstance>() {

            public int compare(NodeInstance o1, NodeInstance o2) {
                return (int) (o1.getId().compareTo(o2.getId()));
            }
        });
        for (NodeInstance nodeInstance : nodeInstances) {
            _instance.addNodeInstance(writeNodeInstance(context, nodeInstance));
        }

        List<ContextInstance> exclusiveGroupInstances = workFlow.getContextInstances(ExclusiveGroup.EXCLUSIVE_GROUP);
        if (exclusiveGroupInstances != null) {
            for (ContextInstance contextInstance : exclusiveGroupInstances) {
                AutomatikMessages.ProcessInstance.ExclusiveGroupInstance.Builder _exclusive = AutomatikMessages.ProcessInstance.ExclusiveGroupInstance
                        .newBuilder();
                ExclusiveGroupInstance exclusiveGroupInstance = (ExclusiveGroupInstance) contextInstance;
                Collection<NodeInstance> groupNodeInstances = exclusiveGroupInstance.getNodeInstances();
                for (NodeInstance nodeInstance : groupNodeInstances) {
                    _exclusive.addGroupNodeInstanceId(nodeInstance.getId());
                }
                _instance.addExclusiveGroup(_exclusive.build());
            }
        }

        writeVariableScope(context, workFlow, _instance);

        List<Map.Entry<String, Integer>> iterationlevels = new ArrayList<Map.Entry<String, Integer>>(
                workFlow.getIterationLevels().entrySet());
        Collections.sort(iterationlevels, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        for (Map.Entry<String, Integer> level : iterationlevels) {
            if (level.getValue() != null) {
                _instance.addIterationLevels(
                        AutomatikMessages.IterationLevel.newBuilder().setId(level.getKey()).setLevel(level.getValue()));
            }
        }

        return _instance.build();
    }

    public AutomatikMessages.ProcessInstance.NodeInstance writeNodeInstance(MarshallerWriteContext context,
            NodeInstance nodeInstance) throws IOException {
        AutomatikMessages.ProcessInstance.NodeInstance.Builder _node = AutomatikMessages.ProcessInstance.NodeInstance
                .newBuilder().setId(nodeInstance.getId()).setNodeId(nodeInstance.getNodeId())
                .setLevel(((io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance).getLevel())
                .setSlaCompliance(
                        ((io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance).getSlaCompliance())
                .setTriggerDate(((io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance)
                        .getTriggerTime().getTime());

        if (((io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance).getSlaDueDate() != null) {
            _node.setSlaDueDate(((io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance)
                    .getSlaDueDate().getTime());
        }
        if (((io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance).getSlaTimerId() != null) {
            _node.setSlaTimerId(
                    ((io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance).getSlaTimerId());
        }
        if (((NodeInstanceImpl) nodeInstance).getRetryJobId() != null) {
            _node.setRetryJobId(((NodeInstanceImpl) nodeInstance).getRetryJobId());
        }
        if (((NodeInstanceImpl) nodeInstance).getRetryAttempts() != null) {
            _node.setRetryAttempts(((NodeInstanceImpl) nodeInstance).getRetryAttempts());
        }

        _node.setContent(writeNodeInstanceContent(_node, nodeInstance, context));
        return _node.build();
    }

    protected AutomatikMessages.ProcessInstance.NodeInstanceContent writeNodeInstanceContent(
            AutomatikMessages.ProcessInstance.NodeInstance.Builder _node, NodeInstance nodeInstance,
            MarshallerWriteContext context) throws IOException {
        AutomatikMessages.ProcessInstance.NodeInstanceContent.Builder _content = null;
        if (nodeInstance instanceof RuleSetNodeInstance) {
            _content = AutomatikMessages.ProcessInstance.NodeInstanceContent.newBuilder()
                    .setType(NodeInstanceType.RULE_SET_NODE);
            List<String> timerInstances = ((RuleSetNodeInstance) nodeInstance).getTimerInstances();
            AutomatikMessages.ProcessInstance.NodeInstanceContent.RuleSetNode.Builder _ruleSet = AutomatikMessages.ProcessInstance.NodeInstanceContent.RuleSetNode
                    .newBuilder();
            if (timerInstances != null) {

                for (String id : timerInstances) {
                    _ruleSet.addTimerInstanceId(id);
                }
            }

            _content.setRuleSet(_ruleSet.build());

        } else if (nodeInstance instanceof HumanTaskNodeInstance) {
            AutomatikMessages.ProcessInstance.NodeInstanceContent.HumanTaskNode.Builder _task = AutomatikMessages.ProcessInstance.NodeInstanceContent.HumanTaskNode
                    .newBuilder().setWorkItemId(((HumanTaskNodeInstance) nodeInstance).getWorkItemId())
                    .setWorkitem(writeHumanTaskWorkItem(context,
                            (HumanTaskWorkItem) ((HumanTaskNodeInstance) nodeInstance).getWorkItem()));
            List<String> timerInstances = ((HumanTaskNodeInstance) nodeInstance).getTimerInstances();
            if (timerInstances != null) {
                for (String id : timerInstances) {
                    _task.addTimerInstanceId(id);
                }
            }
            if (((WorkItemNodeInstance) nodeInstance).getExceptionHandlingProcessInstanceId() != null) {
                _task.setErrorHandlingProcessInstanceId(
                        ((HumanTaskNodeInstance) nodeInstance).getExceptionHandlingProcessInstanceId());
            }
            _content = AutomatikMessages.ProcessInstance.NodeInstanceContent.newBuilder()
                    .setType(NodeInstanceType.HUMAN_TASK_NODE).setHumanTask(_task.build());
        } else if (nodeInstance instanceof WorkItemNodeInstance) {
            AutomatikMessages.ProcessInstance.NodeInstanceContent.WorkItemNode.Builder _wi = AutomatikMessages.ProcessInstance.NodeInstanceContent.WorkItemNode
                    .newBuilder().setWorkItemId(((WorkItemNodeInstance) nodeInstance).getWorkItemId())
                    .setWorkitem(writeWorkItem(context, ((WorkItemNodeInstance) nodeInstance).getWorkItem()));

            List<String> timerInstances = ((WorkItemNodeInstance) nodeInstance).getTimerInstances();
            if (timerInstances != null) {
                for (String id : timerInstances) {
                    _wi.addTimerInstanceId(id);
                }
            }
            if (((WorkItemNodeInstance) nodeInstance).getExceptionHandlingProcessInstanceId() != null) {
                _wi.setErrorHandlingProcessInstanceId(
                        ((WorkItemNodeInstance) nodeInstance).getExceptionHandlingProcessInstanceId());
            }
            _content = AutomatikMessages.ProcessInstance.NodeInstanceContent.newBuilder()
                    .setType(NodeInstanceType.WORK_ITEM_NODE).setWorkItem(_wi.build());
        } else if (nodeInstance instanceof LambdaSubProcessNodeInstance) {
            AutomatikMessages.ProcessInstance.NodeInstanceContent.SubProcessNode.Builder _sp = AutomatikMessages.ProcessInstance.NodeInstanceContent.SubProcessNode
                    .newBuilder()
                    .setProcessInstanceId(((LambdaSubProcessNodeInstance) nodeInstance).getProcessInstanceId())
                    .setProcessInstanceName(((LambdaSubProcessNodeInstance) nodeInstance).getProcessInstanceName());
            List<String> timerInstances = ((LambdaSubProcessNodeInstance) nodeInstance).getTimerInstances();
            if (timerInstances != null) {
                for (String id : timerInstances) {
                    _sp.addTimerInstanceId(id);
                }
            }
            _content = AutomatikMessages.ProcessInstance.NodeInstanceContent.newBuilder()
                    .setType(NodeInstanceType.SUB_PROCESS_NODE).setSubProcess(_sp.build());
        } else if (nodeInstance instanceof SubProcessNodeInstance) {
            AutomatikMessages.ProcessInstance.NodeInstanceContent.SubProcessNode.Builder _sp = AutomatikMessages.ProcessInstance.NodeInstanceContent.SubProcessNode
                    .newBuilder().setProcessInstanceId(((SubProcessNodeInstance) nodeInstance).getProcessInstanceId());
            List<String> timerInstances = ((SubProcessNodeInstance) nodeInstance).getTimerInstances();
            if (timerInstances != null) {
                for (String id : timerInstances) {
                    _sp.addTimerInstanceId(id);
                }
            }
            _content = AutomatikMessages.ProcessInstance.NodeInstanceContent.newBuilder()
                    .setType(NodeInstanceType.SUBPROCESS_NODE).setSubProcess(_sp.build());
        } else if (nodeInstance instanceof MilestoneNodeInstance) {
            AutomatikMessages.ProcessInstance.NodeInstanceContent.MilestoneNode.Builder _ms = AutomatikMessages.ProcessInstance.NodeInstanceContent.MilestoneNode
                    .newBuilder();
            List<String> timerInstances = ((MilestoneNodeInstance) nodeInstance).getTimerInstances();
            if (timerInstances != null) {
                for (String id : timerInstances) {
                    _ms.addTimerInstanceId(id);
                }
            }
            _content = AutomatikMessages.ProcessInstance.NodeInstanceContent.newBuilder()
                    .setType(NodeInstanceType.MILESTONE_NODE).setMilestone(_ms.build());
        } else if (nodeInstance instanceof EventNodeInstance) {
            _content = AutomatikMessages.ProcessInstance.NodeInstanceContent.newBuilder()
                    .setType(NodeInstanceType.EVENT_NODE);
        } else if (nodeInstance instanceof TimerNodeInstance) {
            _content = AutomatikMessages.ProcessInstance.NodeInstanceContent.newBuilder()
                    .setType(NodeInstanceType.TIMER_NODE)
                    .setTimer(AutomatikMessages.ProcessInstance.NodeInstanceContent.TimerNode.newBuilder()
                            .setTimerId(((TimerNodeInstance) nodeInstance).getTimerId()).build());
        } else if (nodeInstance instanceof JoinInstance) {
            AutomatikMessages.ProcessInstance.NodeInstanceContent.JoinNode.Builder _join = AutomatikMessages.ProcessInstance.NodeInstanceContent.JoinNode
                    .newBuilder();
            Map<Long, Integer> triggers = ((JoinInstance) nodeInstance).getTriggers();
            List<Long> keys = new ArrayList<Long>(triggers.keySet());
            Collections.sort(keys, new Comparator<Long>() {
                public int compare(Long o1, Long o2) {
                    return o1.compareTo(o2);
                }
            });
            for (Long key : keys) {
                _join.addTrigger(AutomatikMessages.ProcessInstance.NodeInstanceContent.JoinNode.JoinTrigger.newBuilder()
                        .setNodeId(key).setCounter(triggers.get(key)).build());
            }
            _content = AutomatikMessages.ProcessInstance.NodeInstanceContent.newBuilder()
                    .setType(NodeInstanceType.JOIN_NODE).setJoin(_join.build());
        } else if (nodeInstance instanceof StateNodeInstance) {
            AutomatikMessages.ProcessInstance.NodeInstanceContent.StateNode.Builder _state = AutomatikMessages.ProcessInstance.NodeInstanceContent.StateNode
                    .newBuilder();
            List<String> timerInstances = ((StateNodeInstance) nodeInstance).getTimerInstances();
            if (timerInstances != null) {
                for (String id : timerInstances) {
                    _state.addTimerInstanceId(id);
                }
            }
            _content = AutomatikMessages.ProcessInstance.NodeInstanceContent.newBuilder()
                    .setType(NodeInstanceType.STATE_NODE).setState(_state.build());
        } else if (nodeInstance instanceof ForEachNodeInstance) {
            AutomatikMessages.ProcessInstance.NodeInstanceContent.ForEachNode.Builder _foreach = AutomatikMessages.ProcessInstance.NodeInstanceContent.ForEachNode
                    .newBuilder();
            ForEachNodeInstance forEachNodeInstance = (ForEachNodeInstance) nodeInstance;
            List<NodeInstance> nodeInstances = new ArrayList<NodeInstance>(forEachNodeInstance.getNodeInstances());
            Collections.sort(nodeInstances, new Comparator<NodeInstance>() {
                public int compare(NodeInstance o1, NodeInstance o2) {
                    return (int) (o1.getId().compareTo(o2.getId()));
                }
            });
            for (NodeInstance subNodeInstance : nodeInstances) {
                if (subNodeInstance instanceof CompositeContextNodeInstance) {
                    _foreach.addNodeInstance(writeNodeInstance(context, subNodeInstance));
                }
            }

            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) forEachNodeInstance
                    .getContextInstance(VariableScope.VARIABLE_SCOPE);
            if (variableScopeInstance != null) {
                List<Map.Entry<String, Object>> variables = new ArrayList<Map.Entry<String, Object>>(
                        variableScopeInstance.getVariables().entrySet());
                Collections.sort(variables, new Comparator<Map.Entry<String, Object>>() {
                    public int compare(Map.Entry<String, Object> o1, Map.Entry<String, Object> o2) {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                });
                for (Map.Entry<String, Object> variable : variables) {

                    _foreach.addVariable(ProtobufProcessMarshaller.marshallVariable(context, variable.getKey(),
                            variable.getValue()));
                }
            }

            List<Map.Entry<String, Integer>> iterationlevels = new ArrayList<Map.Entry<String, Integer>>(
                    forEachNodeInstance.getIterationLevels().entrySet());
            Collections.sort(iterationlevels, new Comparator<Map.Entry<String, Integer>>() {
                public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });

            for (Map.Entry<String, Integer> level : iterationlevels) {
                if (level.getKey() != null && level.getValue() != null) {
                    _foreach.addIterationLevels(AutomatikMessages.IterationLevel.newBuilder().setId(level.getKey())
                            .setLevel(level.getValue()));
                }
            }
            _foreach.setSequentialCounter(forEachNodeInstance.getSequentialCounter());
            _content = AutomatikMessages.ProcessInstance.NodeInstanceContent.newBuilder()
                    .setType(NodeInstanceType.FOR_EACH_NODE).setForEach(_foreach.build());
        } else if (nodeInstance instanceof CompositeContextNodeInstance) {
            AutomatikMessages.ProcessInstance.NodeInstanceContent.CompositeContextNode.Builder _composite = AutomatikMessages.ProcessInstance.NodeInstanceContent.CompositeContextNode
                    .newBuilder();
            AutomatikMessages.ProcessInstance.NodeInstanceType _type = null;
            if (nodeInstance instanceof DynamicNodeInstance) {
                _type = AutomatikMessages.ProcessInstance.NodeInstanceType.DYNAMIC_NODE;
            } else if (nodeInstance instanceof EventSubProcessNodeInstance) {
                _type = AutomatikMessages.ProcessInstance.NodeInstanceType.EVENT_SUBPROCESS_NODE;
            } else {
                _type = AutomatikMessages.ProcessInstance.NodeInstanceType.COMPOSITE_CONTEXT_NODE;
            }

            CompositeContextNodeInstance compositeNodeInstance = (CompositeContextNodeInstance) nodeInstance;
            List<String> timerInstances = ((CompositeContextNodeInstance) nodeInstance).getTimerInstances();
            if (timerInstances != null) {
                for (String id : timerInstances) {
                    _composite.addTimerInstanceId(id);
                }
            }
            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) compositeNodeInstance
                    .getContextInstance(VariableScope.VARIABLE_SCOPE);
            if (variableScopeInstance != null) {
                List<Map.Entry<String, Object>> variables = new ArrayList<Map.Entry<String, Object>>(
                        variableScopeInstance.getVariables().entrySet());
                Collections.sort(variables, new Comparator<Map.Entry<String, Object>>() {
                    public int compare(Map.Entry<String, Object> o1, Map.Entry<String, Object> o2) {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                });
                for (Map.Entry<String, Object> variable : variables) {

                    _composite.addVariable(ProtobufProcessMarshaller.marshallVariable(context, variable.getKey(),
                            variable.getValue()));
                }
            }

            List<Map.Entry<String, Integer>> iterationlevels = new ArrayList<Map.Entry<String, Integer>>(
                    compositeNodeInstance.getIterationLevels().entrySet());
            Collections.sort(iterationlevels, new Comparator<Map.Entry<String, Integer>>() {
                public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });

            for (Map.Entry<String, Integer> level : iterationlevels) {
                if (level.getKey() != null && level.getValue() != null) {
                    _composite.addIterationLevels(AutomatikMessages.IterationLevel.newBuilder().setId(level.getKey())
                            .setLevel(level.getValue()));
                }
            }

            List<NodeInstance> nodeInstances = new ArrayList<NodeInstance>(compositeNodeInstance.getNodeInstances());
            Collections.sort(nodeInstances, new Comparator<NodeInstance>() {
                public int compare(NodeInstance o1, NodeInstance o2) {
                    return (int) (o1.getId().compareTo(o2.getId()));
                }
            });
            for (NodeInstance subNodeInstance : nodeInstances) {
                _composite.addNodeInstance(writeNodeInstance(context, subNodeInstance));
            }
            List<ContextInstance> exclusiveGroupInstances = compositeNodeInstance
                    .getContextInstances(ExclusiveGroup.EXCLUSIVE_GROUP);
            if (exclusiveGroupInstances != null) {
                for (ContextInstance contextInstance : exclusiveGroupInstances) {
                    AutomatikMessages.ProcessInstance.ExclusiveGroupInstance.Builder _excl = AutomatikMessages.ProcessInstance.ExclusiveGroupInstance
                            .newBuilder();
                    ExclusiveGroupInstance exclusiveGroupInstance = (ExclusiveGroupInstance) contextInstance;
                    Collection<NodeInstance> groupNodeInstances = exclusiveGroupInstance.getNodeInstances();
                    for (NodeInstance groupNodeInstance : groupNodeInstances) {
                        _excl.addGroupNodeInstanceId(groupNodeInstance.getId());
                    }
                    _composite.addExclusiveGroup(_excl.build());
                }
            }
            _content = AutomatikMessages.ProcessInstance.NodeInstanceContent.newBuilder().setType(_type)
                    .setComposite(_composite.build());
        } else {
            throw new IllegalArgumentException("Unknown node instance type: " + nodeInstance);
        }
        return _content.build();
    }

    public static AutomatikMessages.WorkItem writeWorkItem(MarshallerWriteContext context, WorkItem workItem)
            throws IOException {
        AutomatikMessages.WorkItem.Builder _workItem = AutomatikMessages.WorkItem.newBuilder().setId(workItem.getId())
                .setProcessInstancesId(workItem.getProcessInstanceId()).setName(workItem.getName())
                .setState(workItem.getState());

        if (workItem instanceof WorkItemImpl) {

            _workItem.setNodeId(((WorkItemImpl) workItem).getNodeId())
                    .setNodeInstanceId(((WorkItemImpl) workItem).getNodeInstanceId());

            if (workItem.getPhaseId() != null) {
                _workItem.setPhaseId(workItem.getPhaseId());
            }
            if (workItem.getPhaseStatus() != null) {
                _workItem.setPhaseStatus(workItem.getPhaseStatus());
            }
            if (workItem.getStartDate() != null) {
                _workItem.setStartDate(workItem.getStartDate().getTime());
            }
            if (workItem.getCompleteDate() != null) {
                _workItem.setCompleteDate(workItem.getCompleteDate().getTime());
            }
        }

        Map<String, Object> parameters = workItem.getParameters();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            _workItem
                    .addVariable(ProtobufProcessMarshaller.marshallVariable(context, entry.getKey(), entry.getValue()));
        }

        return _workItem.build();
    }

    public static WorkItem readWorkItem(MarshallerReaderContext context, AutomatikMessages.WorkItem _workItem)
            throws IOException {
        WorkItemImpl workItem = new WorkItemImpl();
        workItem.setId(_workItem.getId());
        workItem.setProcessInstanceId(_workItem.getProcessInstancesId());
        workItem.setName(_workItem.getName());
        workItem.setState(_workItem.getState());
        workItem.setDeploymentId(_workItem.getDeploymentId());
        workItem.setNodeId(_workItem.getNodeId());
        workItem.setNodeInstanceId(_workItem.getNodeInstanceId());
        workItem.setPhaseId(_workItem.getPhaseId());
        workItem.setPhaseStatus(_workItem.getPhaseStatus());
        workItem.setStartDate(new Date(_workItem.getStartDate()));
        if (_workItem.getCompleteDate() > 0) {
            workItem.setCompleteDate(new Date(_workItem.getCompleteDate()));
        }

        for (AutomatikMessages.Variable _variable : _workItem.getVariableList()) {
            try {
                Object value = ProtobufProcessMarshaller.unmarshallVariableValue(context, _variable);
                workItem.setParameter(_variable.getName(), value);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return workItem;
    }

    public static AutomatikMessages.HumanTaskWorkItem writeHumanTaskWorkItem(MarshallerWriteContext context,
            HumanTaskWorkItem workItem) throws IOException {
        AutomatikMessages.HumanTaskWorkItem.Builder _workItem = AutomatikMessages.HumanTaskWorkItem.newBuilder()
                .setId(workItem.getId()).setProcessInstancesId(workItem.getProcessInstanceId())
                .setName(workItem.getName()).setState(workItem.getState());

        _workItem.setNodeId(((WorkItemImpl) workItem).getNodeId())
                .setNodeInstanceId(((WorkItemImpl) workItem).getNodeInstanceId());

        if (workItem.getPhaseId() != null) {
            _workItem.setPhaseId(workItem.getPhaseId());
        }
        if (workItem.getPhaseStatus() != null) {
            _workItem.setPhaseStatus(workItem.getPhaseStatus());
        }
        if (workItem.getStartDate() != null) {
            _workItem.setStartDate(workItem.getStartDate().getTime());
        }
        if (workItem.getCompleteDate() != null) {
            _workItem.setCompleteDate(workItem.getCompleteDate().getTime());
        }
        if (workItem.getTaskName() != null) {
            _workItem.setTaskName(workItem.getTaskName());
        }
        if (workItem.getTaskDescription() != null) {
            _workItem.setTaskDescription(workItem.getTaskDescription());
        }
        if (workItem.getTaskPriority() != null) {
            _workItem.setTaskPriority(workItem.getTaskPriority());
        }
        if (workItem.getReferenceName() != null) {
            _workItem.setTaskReferenceName(workItem.getReferenceName());
        }
        if (workItem.getActualOwner() != null) {
            _workItem.setActualOwner(workItem.getActualOwner());
        }

        _workItem.addAllAdminUsers(workItem.getAdminUsers());
        _workItem.addAllAdminGroups(workItem.getAdminGroups());
        _workItem.addAllPotUsers(workItem.getPotentialUsers());
        _workItem.addAllPotGroups(workItem.getPotentialGroups());
        _workItem.addAllExcludedUsers(workItem.getExcludedUsers());

        Map<String, Object> parameters = workItem.getParameters();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            _workItem
                    .addVariable(ProtobufProcessMarshaller.marshallVariable(context, entry.getKey(), entry.getValue()));
        }

        return _workItem.build();
    }

    public static HumanTaskWorkItem readHumanTaskWorkItem(MarshallerReaderContext context,
            AutomatikMessages.HumanTaskWorkItem _workItem) throws IOException {
        HumanTaskWorkItemImpl workItem = new HumanTaskWorkItemImpl();
        workItem.setId(_workItem.getId());
        workItem.setProcessInstanceId(_workItem.getProcessInstancesId());
        workItem.setName(_workItem.getName());
        workItem.setState(_workItem.getState());
        workItem.setDeploymentId(_workItem.getDeploymentId());
        workItem.setNodeId(_workItem.getNodeId());
        workItem.setNodeInstanceId(_workItem.getNodeInstanceId());
        workItem.setPhaseId(_workItem.getPhaseId());
        workItem.setPhaseStatus(_workItem.getPhaseStatus());
        workItem.setStartDate(new Date(_workItem.getStartDate()));
        if (_workItem.getCompleteDate() > 0) {
            workItem.setCompleteDate(new Date(_workItem.getCompleteDate()));
        }
        if (_workItem.getTaskName() != null) {
            workItem.setTaskName(_workItem.getTaskName());
        }
        if (_workItem.getTaskDescription() != null) {
            workItem.setTaskDescription(_workItem.getTaskDescription());
        }
        if (_workItem.getTaskPriority() != null) {
            workItem.setTaskPriority(_workItem.getTaskPriority());
        }
        if (_workItem.getTaskReferenceName() != null) {
            workItem.setReferenceName(_workItem.getTaskReferenceName());
        }
        if (_workItem.getActualOwner() != null) {
            workItem.setActualOwner(_workItem.getActualOwner());
        }

        for (String item : _workItem.getAdminGroupsList()) {
            workItem.getAdminGroups().add(item);
        }
        for (String item : _workItem.getAdminUsersList()) {
            workItem.getAdminUsers().add(item);
        }
        for (String item : _workItem.getPotGroupsList()) {
            workItem.getPotentialGroups().add(item);
        }
        for (String item : _workItem.getPotUsersList()) {
            workItem.getPotentialUsers().add(item);
        }
        for (String item : _workItem.getExcludedUsersList()) {
            workItem.getExcludedUsers().add(item);
        }

        for (AutomatikMessages.Variable _variable : _workItem.getVariableList()) {
            try {
                Object value = ProtobufProcessMarshaller.unmarshallVariableValue(context, _variable);
                workItem.setParameter(_variable.getName(), value);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return workItem;
    }

    // Input methods
    public ProcessInstance readProcessInstance(MarshallerReaderContext context) throws IOException {

        AutomatikMessages.ProcessInstance _instance = (io.automatiko.engine.workflow.marshalling.impl.AutomatikMessages.ProcessInstance) context.parameterObject;
        if (_instance == null) {
            // try to parse from the stream
            ExtensionRegistry registry = PersisterHelper.buildRegistry(context, null);
            Header _header;
            try {

                _header = PersisterHelper.readFromStreamWithHeaderPreloaded(context, registry);
            } catch (ClassNotFoundException e) {
                // Java 5 does not accept [new IOException(String, Throwable)]
                IOException ioe = new IOException("Error deserializing process instance.");
                ioe.initCause(e);
                throw ioe;
            }
            _instance = AutomatikMessages.ProcessInstance.parseFrom(_header.getPayload(), registry);
        }

        WorkflowProcessInstanceImpl processInstance = createProcessInstance();
        processInstance.setId(_instance.getId());
        String processId = _instance.getProcessId();
        processInstance.setProcessId(processId);
        String processXml = _instance.getProcessXml();
        Process process = null;
        if (processXml != null && processXml.trim().length() > 0) {
            processInstance.setProcessXml(processXml);
            process = processInstance.getProcess();
        } else {
            process = context.processes.get(processId);
            if (process == null) {
                throw new RuntimeException("Could not find process " + processId + " when restoring process instance "
                        + processInstance.getId());
            }
            processInstance.setProcess(process);
        }
        processInstance.setDescription(_instance.getDescription());
        processInstance.setState(_instance.getState());
        processInstance.setParentProcessInstanceId(_instance.getParentProcessInstanceId());
        processInstance.setRootProcessInstanceId(_instance.getRootProcessInstanceId());
        processInstance.setRootProcessId(_instance.getRootProcessId());
        processInstance.setSignalCompletion(_instance.getSignalCompletion());
        processInstance.setInitiator(_instance.getInitiator());
        processInstance.setCorrelationKey(_instance.getCorrelationKey());
        processInstance.setStartDate(new Date(_instance.getStartDate()));
        processInstance.internalSetSlaCompliance(_instance.getSlaCompliance());
        if (_instance.getSlaDueDate() > 0) {
            processInstance.internalSetSlaDueDate(new Date(_instance.getSlaDueDate()));
        }
        processInstance.internalSetSlaTimerId(_instance.getSlaTimerId());

        processInstance.setProcessRuntime(context.getProcessRuntime());

        processInstance.internalSetErrorNodeId(_instance.getErrorNodeId());
        processInstance.internalSetErrorMessage(_instance.getErrorMessage());

        processInstance.setReferenceId(_instance.getReferenceId());

        for (String completedNodeId : _instance.getCompletedNodeIdsList()) {
            processInstance.addCompletedNodeId(completedNodeId);
        }

        if (_instance.getChildrenCount() > 0) {
            _instance.getChildrenList()
                    .forEach(child -> processInstance.addChildren(child.getProcessId(), child.getIdsList()));
        }

        if (_instance.getTagsCount() > 0) {
            _instance.getTagsList()
                    .forEach(tag -> processInstance.internalAddTag(tag.getId(), tag.getValue()));
        }

        if (_instance.getSwimlaneContextCount() > 0) {
            Context swimlaneContext = ((io.automatiko.engine.workflow.base.core.Process) process)
                    .getDefaultContext(SwimlaneContext.SWIMLANE_SCOPE);
            SwimlaneContextInstance swimlaneContextInstance = (SwimlaneContextInstance) processInstance
                    .getContextInstance(swimlaneContext);
            for (AutomatikMessages.ProcessInstance.SwimlaneContextInstance _swimlane : _instance
                    .getSwimlaneContextList()) {
                swimlaneContextInstance.setActorId(_swimlane.getSwimlane(), _swimlane.getActorId());
            }
        }

        for (AutomatikMessages.ProcessInstance.NodeInstance _node : _instance.getNodeInstanceList()) {
            context.parameterObject = _node;
            readNodeInstance(context, processInstance, processInstance);
        }

        for (AutomatikMessages.ProcessInstance.ExclusiveGroupInstance _excl : _instance.getExclusiveGroupList()) {
            ExclusiveGroupInstance exclusiveGroupInstance = new ExclusiveGroupInstance();
            processInstance.addContextInstance(ExclusiveGroup.EXCLUSIVE_GROUP, exclusiveGroupInstance);
            for (String nodeInstanceId : _excl.getGroupNodeInstanceIdList()) {
                NodeInstance nodeInstance = ((io.automatiko.engine.workflow.process.instance.NodeInstanceContainer) processInstance)
                        .getNodeInstance(nodeInstanceId, true);
                if (nodeInstance == null) {
                    throw new IllegalArgumentException(
                            "Could not find node instance when deserializing exclusive group instance: "
                                    + nodeInstanceId);
                }
                exclusiveGroupInstance.addNodeInstance(nodeInstance);
            }
        }
        readVariableScope(context, process, processInstance, _instance);

        if (_instance.getIterationLevelsCount() > 0) {

            for (AutomatikMessages.IterationLevel _level : _instance.getIterationLevelsList()) {
                processInstance.getIterationLevels().put(_level.getId(), _level.getLevel());
            }
        }
        return processInstance;
    }

    protected void readVariableScope(MarshallerReaderContext context, Process process,
            WorkflowProcessInstanceImpl processInstance, AutomatikMessages.ProcessInstance _instance) throws IOException {

        if (_instance.getVariableCount() > 0) {
            Context variableScope = ((io.automatiko.engine.workflow.base.core.Process) process)
                    .getDefaultContext(VariableScope.VARIABLE_SCOPE);
            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) processInstance
                    .getContextInstance(variableScope);
            for (AutomatikMessages.Variable _variable : _instance.getVariableList()) {
                try {
                    Object _value = ProtobufProcessMarshaller.unmarshallVariableValue(context, _variable);
                    variableScopeInstance.internalSetVariable(_variable.getName(), _value);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Could not reload variable " + _variable.getName());
                }
            }
        }

    }

    protected abstract WorkflowProcessInstanceImpl createProcessInstance();

    public NodeInstance readNodeInstance(MarshallerReaderContext context, NodeInstanceContainer nodeInstanceContainer,
            WorkflowProcessInstance processInstance) throws IOException {
        AutomatikMessages.ProcessInstance.NodeInstance _node = (AutomatikMessages.ProcessInstance.NodeInstance) context.parameterObject;

        NodeInstanceImpl nodeInstance = readNodeInstanceContent(_node, context, processInstance);

        nodeInstance.setNodeId(_node.getNodeId());
        nodeInstance.setId(_node.getId());
        nodeInstance.setNodeInstanceContainer(nodeInstanceContainer);
        nodeInstance.setProcessInstance(
                (io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance) processInstance);
        nodeInstance.setLevel(_node.getLevel() == 0 ? 1 : _node.getLevel());
        nodeInstance.internalSetTriggerTime(new Date(_node.getTriggerDate()));
        nodeInstance.internalSetSlaCompliance(_node.getSlaCompliance());
        if (_node.getSlaDueDate() > 0) {
            nodeInstance.internalSetSlaDueDate(new Date(_node.getSlaDueDate()));
        }
        nodeInstance.internalSetSlaTimerId(_node.getSlaTimerId());

        nodeInstance.internalSetRetryJobId(_node.getRetryJobId());
        nodeInstance.internalSetRetryAttempts(_node.getRetryAttempts());

        switch (_node.getContent().getType()) {
            case COMPOSITE_CONTEXT_NODE:

            case DYNAMIC_NODE:
                if (_node.getContent().getComposite().getVariableCount() > 0) {
                    Context variableScope = ((io.automatiko.engine.workflow.base.core.Process) ((io.automatiko.engine.workflow.base.instance.ProcessInstance) processInstance)
                            .getProcess()).getDefaultContext(VariableScope.VARIABLE_SCOPE);
                    VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((CompositeContextNodeInstance) nodeInstance)
                            .getContextInstance(variableScope);
                    for (AutomatikMessages.Variable _variable : _node.getContent().getComposite().getVariableList()) {
                        try {
                            Object _value = ProtobufProcessMarshaller.unmarshallVariableValue(context, _variable);
                            variableScopeInstance.internalSetVariable(_variable.getName(), _value);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalArgumentException("Could not reload variable " + _variable.getName());
                        }
                    }
                }
                if (_node.getContent().getComposite().getIterationLevelsCount() > 0) {

                    for (AutomatikMessages.IterationLevel _level : _node.getContent().getComposite()
                            .getIterationLevelsList()) {
                        ((CompositeContextNodeInstance) nodeInstance).getIterationLevels().put(_level.getId(),
                                _level.getLevel());
                    }
                }
                for (AutomatikMessages.ProcessInstance.NodeInstance _instance : _node.getContent().getComposite()
                        .getNodeInstanceList()) {
                    context.parameterObject = _instance;
                    readNodeInstance(context, (CompositeContextNodeInstance) nodeInstance, processInstance);
                }

                for (AutomatikMessages.ProcessInstance.ExclusiveGroupInstance _excl : _node.getContent().getComposite()
                        .getExclusiveGroupList()) {
                    ExclusiveGroupInstance exclusiveGroupInstance = new ExclusiveGroupInstance();
                    ((CompositeContextNodeInstance) nodeInstance).addContextInstance(ExclusiveGroup.EXCLUSIVE_GROUP,
                            exclusiveGroupInstance);
                    for (String nodeInstanceId : _excl.getGroupNodeInstanceIdList()) {
                        NodeInstance groupNodeInstance = ((io.automatiko.engine.workflow.process.instance.NodeInstanceContainer) processInstance)
                                .getNodeInstance(nodeInstanceId, true);
                        if (groupNodeInstance == null) {
                            throw new IllegalArgumentException(
                                    "Could not find node instance when deserializing exclusive group instance: "
                                            + nodeInstanceId);
                        }
                        exclusiveGroupInstance.addNodeInstance(groupNodeInstance);
                    }
                }
                break;
            case FOR_EACH_NODE:
                for (AutomatikMessages.ProcessInstance.NodeInstance _instance : _node.getContent().getForEach()
                        .getNodeInstanceList()) {
                    context.parameterObject = _instance;
                    readNodeInstance(context, (ForEachNodeInstance) nodeInstance, processInstance);
                    VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((ForEachNodeInstance) nodeInstance)
                            .getContextInstance(VariableScope.VARIABLE_SCOPE);
                    for (AutomatikMessages.Variable _variable : _node.getContent().getForEach().getVariableList()) {
                        try {
                            Object _value = ProtobufProcessMarshaller.unmarshallVariableValue(context, _variable);
                            variableScopeInstance.internalSetVariable(_variable.getName(), _value);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalArgumentException("Could not reload variable " + _variable.getName());
                        }
                    }
                    if (_node.getContent().getForEach().getIterationLevelsCount() > 0) {

                        for (AutomatikMessages.IterationLevel _level : _node.getContent().getForEach()
                                .getIterationLevelsList()) {
                            ((ForEachNodeInstance) nodeInstance).getIterationLevels().put(_level.getId(),
                                    _level.getLevel());
                        }
                    }
                }
                break;
            case EVENT_SUBPROCESS_NODE:
                for (AutomatikMessages.ProcessInstance.NodeInstance _instance : _node.getContent().getComposite()
                        .getNodeInstanceList()) {
                    context.parameterObject = _instance;
                    readNodeInstance(context, (EventSubProcessNodeInstance) nodeInstance, processInstance);
                    VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((EventSubProcessNodeInstance) nodeInstance)
                            .getContextInstance(VariableScope.VARIABLE_SCOPE);
                    for (AutomatikMessages.Variable _variable : _node.getContent().getComposite().getVariableList()) {
                        try {
                            Object _value = ProtobufProcessMarshaller.unmarshallVariableValue(context, _variable);
                            variableScopeInstance.internalSetVariable(_variable.getName(), _value);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalArgumentException("Could not reload variable " + _variable.getName());
                        }
                    }
                }
                break;
            default:
                // do nothing
        }

        return nodeInstance;
    }

    protected NodeInstanceImpl readNodeInstanceContent(AutomatikMessages.ProcessInstance.NodeInstance _node,
            MarshallerReaderContext context, WorkflowProcessInstance processInstance) throws IOException {
        NodeInstanceImpl nodeInstance = null;
        NodeInstanceContent _content = _node.getContent();
        switch (_content.getType()) {
            case RULE_SET_NODE:
                nodeInstance = new RuleSetNodeInstance();

                if (_content.getRuleSet().getTimerInstanceIdCount() > 0) {
                    List<String> timerInstances = new ArrayList<>();
                    for (String _timerId : _content.getRuleSet().getTimerInstanceIdList()) {
                        timerInstances.add(_timerId);
                    }
                    ((RuleSetNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
                }

                break;
            case HUMAN_TASK_NODE:
                nodeInstance = new HumanTaskNodeInstance();
                ((HumanTaskNodeInstance) nodeInstance).internalSetWorkItemId(_content.getHumanTask().getWorkItemId());
                ((HumanTaskNodeInstance) nodeInstance).internalSetWorkItem(
                        (WorkItemImpl) readHumanTaskWorkItem(context, _content.getHumanTask().getWorkitem()));
                if (_content.getHumanTask().getTimerInstanceIdCount() > 0) {
                    List<String> timerInstances = new ArrayList<>();
                    for (String _timerId : _content.getHumanTask().getTimerInstanceIdList()) {
                        timerInstances.add(_timerId);
                    }
                    ((HumanTaskNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
                }
                ((WorkItemNodeInstance) nodeInstance)
                        .internalSetProcessInstanceId(_content.getHumanTask().getErrorHandlingProcessInstanceId());
                break;
            case WORK_ITEM_NODE:
                nodeInstance = new WorkItemNodeInstance();
                ((WorkItemNodeInstance) nodeInstance).internalSetWorkItemId(_content.getWorkItem().getWorkItemId());
                ((WorkItemNodeInstance) nodeInstance)
                        .internalSetWorkItem((WorkItemImpl) readWorkItem(context, _content.getWorkItem().getWorkitem()));
                if (_content.getWorkItem().getTimerInstanceIdCount() > 0) {
                    List<String> timerInstances = new ArrayList<>();
                    for (String _timerId : _content.getWorkItem().getTimerInstanceIdList()) {
                        timerInstances.add(_timerId);
                    }
                    ((WorkItemNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
                }
                ((WorkItemNodeInstance) nodeInstance)
                        .internalSetProcessInstanceId(_content.getWorkItem().getErrorHandlingProcessInstanceId());
                break;
            case SUBPROCESS_NODE:
                nodeInstance = new SubProcessNodeInstance();
                ((SubProcessNodeInstance) nodeInstance)
                        .internalSetProcessInstanceId(_content.getSubProcess().getProcessInstanceId());
                if (_content.getSubProcess().getTimerInstanceIdCount() > 0) {
                    List<String> timerInstances = new ArrayList<>();
                    for (String _timerId : _content.getSubProcess().getTimerInstanceIdList()) {
                        timerInstances.add(_timerId);
                    }
                    ((SubProcessNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
                }
                break;
            case SUB_PROCESS_NODE:
                nodeInstance = new LambdaSubProcessNodeInstance();
                ((LambdaSubProcessNodeInstance) nodeInstance)
                        .internalSetProcessInstanceId(_content.getSubProcess().getProcessInstanceId());
                ((LambdaSubProcessNodeInstance) nodeInstance)
                        .internalSetProcessInstanceName(_content.getSubProcess().getProcessInstanceName());
                if (_content.getSubProcess().getTimerInstanceIdCount() > 0) {
                    List<String> timerInstances = new ArrayList<>();
                    for (String _timerId : _content.getSubProcess().getTimerInstanceIdList()) {
                        timerInstances.add(_timerId);
                    }
                    ((LambdaSubProcessNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
                }
                break;
            case MILESTONE_NODE:
                nodeInstance = new MilestoneNodeInstance();
                if (_content.getMilestone().getTimerInstanceIdCount() > 0) {
                    List<String> timerInstances = new ArrayList<>();
                    for (String _timerId : _content.getMilestone().getTimerInstanceIdList()) {
                        timerInstances.add(_timerId);
                    }
                    ((MilestoneNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
                }
                break;
            case TIMER_NODE:
                nodeInstance = new TimerNodeInstance();
                ((TimerNodeInstance) nodeInstance).internalSetTimerId(_content.getTimer().getTimerId());
                break;
            case EVENT_NODE:
                nodeInstance = new EventNodeInstance();
                break;
            case JOIN_NODE:
                nodeInstance = new JoinInstance();
                if (_content.getJoin().getTriggerCount() > 0) {
                    Map<Long, Integer> triggers = new HashMap<Long, Integer>();
                    for (AutomatikMessages.ProcessInstance.NodeInstanceContent.JoinNode.JoinTrigger _join : _content
                            .getJoin().getTriggerList()) {
                        triggers.put(_join.getNodeId(), _join.getCounter());
                    }
                    ((JoinInstance) nodeInstance).internalSetTriggers(triggers);
                }
                break;
            case FOR_EACH_NODE:
                nodeInstance = new ForEachNodeInstance();
                ((ForEachNodeInstance) nodeInstance).setInternalSequentialCounter(_content.getForEach().getSequentialCounter());
                break;
            case COMPOSITE_CONTEXT_NODE:
                nodeInstance = new CompositeContextNodeInstance();

                if (_content.getComposite().getTimerInstanceIdCount() > 0) {
                    List<String> timerInstances = new ArrayList<>();
                    for (String _timerId : _content.getComposite().getTimerInstanceIdList()) {
                        timerInstances.add(_timerId);
                    }
                    ((CompositeContextNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
                }
                break;
            case DYNAMIC_NODE:
                nodeInstance = new DynamicNodeInstance();
                if (_content.getComposite().getTimerInstanceIdCount() > 0) {
                    List<String> timerInstances = new ArrayList<>();
                    for (String _timerId : _content.getComposite().getTimerInstanceIdList()) {
                        timerInstances.add(_timerId);
                    }
                    ((CompositeContextNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
                }
                break;
            case STATE_NODE:
                nodeInstance = new StateNodeInstance();
                if (_content.getState().getTimerInstanceIdCount() > 0) {
                    List<String> timerInstances = new ArrayList<>();
                    for (String _timerId : _content.getState().getTimerInstanceIdList()) {
                        timerInstances.add(_timerId);
                    }
                    ((CompositeContextNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
                }
                break;
            case EVENT_SUBPROCESS_NODE:
                nodeInstance = new EventSubProcessNodeInstance();

                if (_content.getComposite().getTimerInstanceIdCount() > 0) {
                    List<String> timerInstances = new ArrayList<>();
                    for (String _timerId : _content.getComposite().getTimerInstanceIdList()) {
                        timerInstances.add(_timerId);
                    }
                    ((CompositeContextNodeInstance) nodeInstance).internalSetTimerInstances(timerInstances);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown node type: " + _content.getType());
        }
        return nodeInstance;

    }

    protected void writeVariableScope(MarshallerWriteContext context, WorkflowProcessInstanceImpl workFlow,
            AutomatikMessages.ProcessInstance.Builder _instance) throws IOException {
        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) workFlow
                .getContextInstance(VariableScope.VARIABLE_SCOPE);
        List<Map.Entry<String, Object>> variables = new ArrayList<Map.Entry<String, Object>>(
                variableScopeInstance.getVariables().entrySet());
        Collections.sort(variables,
                new Comparator<Map.Entry<String, Object>>() {
                    public int compare(Map.Entry<String, Object> o1,
                            Map.Entry<String, Object> o2) {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                });

        for (Map.Entry<String, Object> variable : variables) {
            if (variable.getValue() != null) {
                _instance.addVariable(
                        ProtobufProcessMarshaller.marshallVariable(context, variable.getKey(), variable.getValue()));
            }
        }
    }

}