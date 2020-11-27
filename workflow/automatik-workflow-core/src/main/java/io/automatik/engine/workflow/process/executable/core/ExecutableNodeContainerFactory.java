
package io.automatik.engine.workflow.process.executable.core;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatik.engine.workflow.process.executable.core.factory.ActionNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.BoundaryEventNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.CompositeContextNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.DynamicNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.EndNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.EventNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.EventSubProcessNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.FaultNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.ForEachNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.HumanTaskNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.JoinFactory;
import io.automatik.engine.workflow.process.executable.core.factory.MilestoneNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.RuleSetNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.SplitFactory;
import io.automatik.engine.workflow.process.executable.core.factory.StartNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.StateNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.SubProcessNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.TimerNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.WorkItemNodeFactory;

public abstract class ExecutableNodeContainerFactory {

    public static final String METHOD_CONNECTION = "connection";

    private NodeContainer nodeContainer;

    protected void setNodeContainer(NodeContainer nodeContainer) {
        this.nodeContainer = nodeContainer;
    }

    protected NodeContainer getNodeContainer() {
        return nodeContainer;
    }

    public StartNodeFactory startNode(long id) {
        return new StartNodeFactory(this, nodeContainer, id);
    }

    public EndNodeFactory endNode(long id) {
        return new EndNodeFactory(this, nodeContainer, id);
    }

    public ActionNodeFactory actionNode(long id) {
        return new ActionNodeFactory(this, nodeContainer, id);
    }

    public MilestoneNodeFactory milestoneNode(long id) {
        return new MilestoneNodeFactory(this, nodeContainer, id);
    }

    public TimerNodeFactory timerNode(long id) {
        return new TimerNodeFactory(this, nodeContainer, id);
    }

    public HumanTaskNodeFactory humanTaskNode(long id) {
        return new HumanTaskNodeFactory(this, nodeContainer, id);
    }

    public SubProcessNodeFactory subProcessNode(long id) {
        return new SubProcessNodeFactory(this, nodeContainer, id);
    }

    public SplitFactory splitNode(long id) {
        return new SplitFactory(this, nodeContainer, id);
    }

    public JoinFactory joinNode(long id) {
        return new JoinFactory(this, nodeContainer, id);
    }

    public RuleSetNodeFactory ruleSetNode(long id) {
        return new RuleSetNodeFactory(this, nodeContainer, id);
    }

    public FaultNodeFactory faultNode(long id) {
        return new FaultNodeFactory(this, nodeContainer, id);
    }

    public EventNodeFactory eventNode(long id) {
        return new EventNodeFactory(this, nodeContainer, id);
    }

    public BoundaryEventNodeFactory boundaryEventNode(long id) {
        return new BoundaryEventNodeFactory(this, nodeContainer, id);
    }

    public CompositeContextNodeFactory compositeContextNode(long id) {
        return new CompositeContextNodeFactory(this, nodeContainer, id);
    }

    public ForEachNodeFactory forEachNode(long id) {
        return new ForEachNodeFactory(this, nodeContainer, id);
    }

    public DynamicNodeFactory dynamicNode(long id) {
        return new DynamicNodeFactory(this, nodeContainer, id);
    }

    public WorkItemNodeFactory workItemNode(long id) {
        return new WorkItemNodeFactory(this, nodeContainer, id);
    }

    public EventSubProcessNodeFactory eventSubProcessNode(long id) {
        return new EventSubProcessNodeFactory(this, nodeContainer, id);
    }

    public StateNodeFactory stateNode(long id) {
        return new StateNodeFactory(this, nodeContainer, id);
    }

    public ExecutableNodeContainerFactory connection(long fromId, long toId) {
        return connection(fromId, toId, "", false);
    }

    public ExecutableNodeContainerFactory connection(long fromId, long toId, String uniqueId) {
        return connection(fromId, toId, uniqueId, false);
    }

    public ExecutableNodeContainerFactory connection(long fromId, long toId, String uniqueId, boolean association) {
        Node from = nodeContainer.getNode(fromId);
        Node to = nodeContainer.getNode(toId);
        ConnectionImpl connection = new ConnectionImpl(from,
                io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, to,
                io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
        connection.setMetaData("UniqueId", uniqueId);
        if (association) {
            connection.setMetaData("association", true);
        }
        return this;
    }

    public abstract ExecutableNodeContainerFactory done();

}
