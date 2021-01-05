
package io.automatiko.engine.workflow.process.executable.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.NodeContainer;
import io.automatiko.engine.workflow.base.core.context.exception.CompensationScope;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatiko.engine.workflow.base.core.context.swimlane.SwimlaneContext;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.event.EventFilter;
import io.automatiko.engine.workflow.process.core.impl.NodeContainerImpl;
import io.automatiko.engine.workflow.process.core.impl.WorkflowProcessImpl;
import io.automatiko.engine.workflow.process.core.node.ConstraintTrigger;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventTrigger;
import io.automatiko.engine.workflow.process.core.node.FaultNode;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.Trigger;

public class ExecutableProcess extends WorkflowProcessImpl {

    private static final long serialVersionUID = 510l;

    public ExecutableProcess() {
        setType(WORKFLOW_TYPE);
        VariableScope variableScope = new VariableScope();
        addContext(variableScope);
        setDefaultContext(variableScope);
        SwimlaneContext swimLaneContext = new SwimlaneContext();
        addContext(swimLaneContext);
        setDefaultContext(swimLaneContext);
        ExceptionScope exceptionScope = new ExceptionScope();
        addContext(exceptionScope);
        setDefaultContext(exceptionScope);
    }

    public VariableScope getVariableScope() {
        return (VariableScope) getDefaultContext(VariableScope.VARIABLE_SCOPE);
    }

    public SwimlaneContext getSwimlaneContext() {
        return (SwimlaneContext) getDefaultContext(SwimlaneContext.SWIMLANE_SCOPE);
    }

    public ExceptionScope getExceptionScope() {
        return (ExceptionScope) getDefaultContext(ExceptionScope.EXCEPTION_SCOPE);
    }

    public CompensationScope getCompensationScope() {
        return (CompensationScope) getDefaultContext(CompensationScope.COMPENSATION_SCOPE);
    }

    protected NodeContainer createNodeContainer() {
        return new WorkflowProcessNodeContainer();
    }

    public List<Node> getStartNodes() {
        return getStartNodes(this.getNodes());
    }

    public static List<Node> getStartNodes(Node[] nodes) {
        List<Node> startNodes = new ArrayList<Node>();
        for (Node node : nodes) {
            if (node instanceof StartNode) {
                startNodes.add(node);
            }
        }

        return startNodes;
    }

    public List<Node> getEndNodes() {
        return getEndNodes(this.getNodes());
    }

    public static List<Node> getEndNodes(Node[] nodes) {
        final List<Node> endNodes = new ArrayList<Node>();
        for (Node node : nodes) {
            if (node instanceof EndNode || node instanceof FaultNode) {
                endNodes.add(node);
            }
        }

        return endNodes;
    }

    public StartNode getStart(String trigger) {
        Node[] nodes = getNodes();

        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] instanceof StartNode) {

                StartNode start = ((StartNode) nodes[i]);
                // return start node that is not event based node
                if (trigger == null && ((start.getTriggers() == null || start.getTriggers().isEmpty())
                        && start.getTimer() == null)) {
                    return start;
                } else {
                    if (start.getTriggers() != null) {
                        for (Trigger t : start.getTriggers()) {
                            if (t instanceof EventTrigger) {
                                for (EventFilter filter : ((EventTrigger) t).getEventFilters()) {
                                    if (filter.acceptsEvent(trigger, null)) {
                                        return start;
                                    }
                                }
                            } else if (t instanceof ConstraintTrigger && "conditional".equals(trigger)) {
                                return start;
                            }
                        }
                    } else if (start.getTimer() != null) {

                        if ("timer".equals(trigger)) {
                            return start;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<Node> getAutoStartNodes() {
        if (!isDynamic()) {
            return Collections.emptyList();
        }

        List<Node> nodes = Arrays.stream(getNodes())
                .filter(n -> n.getIncomingConnections().isEmpty()
                        && "true".equalsIgnoreCase((String) n.getMetaData().get("customAutoStart")))
                .collect(Collectors.toList());

        return nodes;
    }

    private class WorkflowProcessNodeContainer extends NodeContainerImpl {

        private static final long serialVersionUID = 510l;

        protected void validateAddNode(Node node) {
            super.validateAddNode(node);
            StartNode startNode = getStart(null);
            if ((node instanceof StartNode)
                    && (startNode != null && startNode.getTriggers() == null && startNode.getTimer() == null)) {
                // ignore start nodes that are event based
                if ((((StartNode) node).getTriggers() == null || ((StartNode) node).getTriggers().isEmpty())
                        && ((StartNode) node).getTimer() == null) {
                    throw new IllegalArgumentException("A RuleFlowProcess cannot have more than one start node!");
                }
            }
        }

    }

}
