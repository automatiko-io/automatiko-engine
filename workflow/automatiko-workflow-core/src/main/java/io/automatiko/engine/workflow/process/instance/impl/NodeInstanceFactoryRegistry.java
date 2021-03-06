
package io.automatiko.engine.workflow.process.instance.impl;

import static io.automatiko.engine.workflow.process.executable.core.Metadata.UNIQUE_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.NodeInstanceContainer;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatiko.engine.workflow.process.core.node.CatchLinkNode;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.CompositeNode;
import io.automatiko.engine.workflow.process.core.node.DynamicNode;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatiko.engine.workflow.process.core.node.FaultNode;
import io.automatiko.engine.workflow.process.core.node.ForEachNode;
import io.automatiko.engine.workflow.process.core.node.HumanTaskNode;
import io.automatiko.engine.workflow.process.core.node.Join;
import io.automatiko.engine.workflow.process.core.node.MilestoneNode;
import io.automatiko.engine.workflow.process.core.node.RuleSetNode;
import io.automatiko.engine.workflow.process.core.node.Split;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.StateNode;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.core.node.ThrowLinkNode;
import io.automatiko.engine.workflow.process.core.node.TimerNode;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;
import io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatiko.engine.workflow.process.instance.node.ActionNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.BoundaryEventNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.CatchLinkNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.CompositeContextNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.CompositeNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.DynamicNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.EndNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.EventNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.EventSubProcessNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.FaultNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.ForEachNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.HumanTaskNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.JoinInstance;
import io.automatiko.engine.workflow.process.instance.node.LambdaSubProcessNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.MilestoneNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.RuleSetNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.SplitInstance;
import io.automatiko.engine.workflow.process.instance.node.StartNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.StateNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.ThrowLinkNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.TimerNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.WorkItemNodeInstance;

public class NodeInstanceFactoryRegistry {

    private static final NodeInstanceFactoryRegistry INSTANCE = new NodeInstanceFactoryRegistry();

    private Map<Class<? extends Node>, NodeInstanceFactory> registry;

    public static NodeInstanceFactoryRegistry getInstance() {
        return INSTANCE;
    }

    protected NodeInstanceFactoryRegistry() {
        this.registry = new HashMap<>();
    }

    public void register(Class<? extends Node> cls, NodeInstanceFactory factory) {
        this.registry.put(cls, factory);
    }

    public NodeInstanceFactory getProcessNodeInstanceFactory(Node node) {
        Class<?> clazz = node.getClass();
        while (clazz != null) {
            NodeInstanceFactory result = this.get(clazz);
            if (result != null) {
                return result;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    protected NodeInstanceFactory get(Class<?> clazz) {
        // hard wired nodes:
        if (RuleSetNode.class == clazz) {
            return factory(RuleSetNodeInstance::new);
        }
        if (Split.class == clazz) {
            return factory(SplitInstance::new);
        }
        if (Join.class == clazz) {
            return factoryOnce(JoinInstance::new);
        }
        if (StartNode.class == clazz) {
            return factory(StartNodeInstance::new);
        }
        if (EndNode.class == clazz) {
            return factory(EndNodeInstance::new);
        }
        if (MilestoneNode.class == clazz) {
            return factory(MilestoneNodeInstance::new);
        }
        if (SubProcessNode.class == clazz) {
            return factory(LambdaSubProcessNodeInstance::new);
        }
        if (ActionNode.class == clazz) {
            return factory(ActionNodeInstance::new);
        }
        if (WorkItemNode.class == clazz) {
            return factory(WorkItemNodeInstance::new);
        }
        if (TimerNode.class == clazz) {
            return factory(TimerNodeInstance::new);
        }
        if (FaultNode.class == clazz) {
            return factory(FaultNodeInstance::new);
        }
        if (EventSubProcessNode.class == clazz) {
            return factory(EventSubProcessNodeInstance::new);
        }
        if (CompositeNode.class == clazz) {
            return factory(CompositeNodeInstance::new);
        }
        if (CompositeContextNode.class == clazz) {
            return factory(CompositeContextNodeInstance::new);
        }
        if (HumanTaskNode.class == clazz) {
            return factory(HumanTaskNodeInstance::new);
        }
        if (ForEachNode.class == clazz) {
            return factory(ForEachNodeInstance::new);
        }
        if (EventNode.class == clazz) {
            return factory(EventNodeInstance::new);
        }
        if (StateNode.class == clazz) {
            return factory(StateNodeInstance::new);
        }
        if (DynamicNode.class == clazz) {
            return factory(DynamicNodeInstance::new);
        }
        if (BoundaryEventNode.class == clazz) {
            return factory(BoundaryEventNodeInstance::new);
        }
        if (CatchLinkNode.class == clazz) {
            return factory(CatchLinkNodeInstance::new);
        }
        if (ThrowLinkNode.class == clazz) {
            return factory(ThrowLinkNodeInstance::new);
        }
        return this.registry.get(clazz);
    }

    protected NodeInstanceFactory factoryOnce(Supplier<NodeInstanceImpl> supplier) {
        return (node, processInstance, nodeInstanceContainer) -> {
            NodeInstance result = ((io.automatiko.engine.workflow.process.instance.NodeInstanceContainer) nodeInstanceContainer)
                    .getFirstNodeInstance(node.getId());
            if (result != null) {
                return result;
            } else {
                return createInstance(supplier.get(), node, processInstance, nodeInstanceContainer);
            }
        };
    }

    protected NodeInstanceFactory factory(Supplier<NodeInstanceImpl> supplier) {
        return (node, processInstance, nodeInstanceContainer) -> createInstance(supplier.get(), node, processInstance,
                nodeInstanceContainer);
    }

    private static NodeInstance createInstance(NodeInstanceImpl nodeInstance, Node node,
            WorkflowProcessInstance processInstance, NodeInstanceContainer nodeInstanceContainer) {
        if (!processInstance.multipleInstancesOfNodeAllowed(node)) {
            return null;
        }

        nodeInstance.setNodeId(node.getId());
        nodeInstance.setNodeInstanceContainer(nodeInstanceContainer);
        nodeInstance.setProcessInstance(processInstance);
        String uniqueId = (String) node.getMetaData().get(UNIQUE_ID);
        if (uniqueId == null) {
            uniqueId = node.getId() + "";
        }
        nodeInstance.setMetaData(UNIQUE_ID, uniqueId);
        int level = ((io.automatiko.engine.workflow.process.instance.NodeInstanceContainer) nodeInstanceContainer)
                .getLevelForNode(uniqueId);
        nodeInstance.setLevel(level);
        return nodeInstance;
    }
}
