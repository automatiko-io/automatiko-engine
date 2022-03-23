package io.automatiko.engine.codegen.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.core.node.CompositeNode;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.FaultNode;
import io.automatiko.engine.workflow.process.core.node.HumanTaskNode;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

public class ProcessNodeLocator {

    public static Collection<FaultNode> findFaultNodes(Process process) {
        Collection<FaultNode> collected = new ArrayList<>();

        collectFaultNodes(collected, ((ExecutableProcess) process).getStart(null));

        return collected;
    }

    public static Collection<FaultNode> findFaultNodes(Process process, Node node) {
        Collection<FaultNode> collected = new ArrayList<>();

        collectFaultNodes(collected, node);

        return collected;
    }

    public static void collectFaultNodes(Collection<FaultNode> collected, Node node) {

        List<Connection> connections = node.getOutgoingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE);

        for (Connection connection : connections) {
            if (connection.getTo() != null) {
                Node nextNode = connection.getTo();
                if (isFaultNode(nextNode)) {
                    collected.add((FaultNode) nextNode);
                    continue;
                } else if (isWaitStateNode(nextNode)) {
                    // check if this is a wait state node and if so move to next outgoing connection of the node
                    continue;
                } else if (isEndNode(nextNode)) {
                    // if this is end node, check if there is parent node and continue from there
                    if (nextNode.getParentContainer() != null && !(nextNode.getParentContainer() instanceof WorkflowProcess)) {
                        CompositeNode subNodes = (CompositeNode) nextNode.getParentContainer();

                        collectFaultNodes(collected, subNodes);
                    }
                } else if (isCompositeNode(nextNode)) {
                    CompositeNode subNodes = (CompositeNode) nextNode;

                    for (Node snode : subNodes.getNodes()) {
                        if (snode instanceof StartNode) {
                            collectFaultNodes(collected, snode);
                        }
                    }

                } else {
                    collectFaultNodes(collected, nextNode);
                }

            }
        }
    }

    public static boolean isFaultNode(Node node) {
        if (node instanceof FaultNode) {
            return true;
        }

        return false;
    }

    public static boolean isWaitStateNode(Node node) {
        if (node instanceof HumanTaskNode || node instanceof EventNode) {
            return true;
        }

        return false;
    }

    public static boolean isEndNode(Node node) {
        if (node instanceof EndNode) {
            return true;
        }

        return false;
    }

    public static boolean isCompositeNode(Node node) {
        if (node instanceof CompositeNode) {
            return true;
        }

        return false;
    }
}
