
package io.automatiko.engine.workflow.process.instance.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.NodeInstanceContainer;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.process.core.node.Join;
import io.automatiko.engine.workflow.process.core.node.Split;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceImpl;

/**
 * Runtime counterpart of a join node.
 * 
 */
public class JoinInstance extends NodeInstanceImpl {

    private static final long serialVersionUID = 510l;

    private Map<Long, Integer> triggers = new HashMap<Long, Integer>();

    protected Join getJoin() {
        return (Join) getNode();
    }

    public void internalTrigger(final NodeInstance from, String type) {
        if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException("An ActionNode only accepts default incoming connections!");
        }
        triggerTime = new Date();
        final Join join = getJoin();
        switch (join.getType()) {
            case Join.TYPE_XOR:
                triggerCompleted();
                break;
            case Join.TYPE_AND:
                Integer count = (Integer) this.triggers.get(from.getNodeId());
                if (count == null) {
                    this.triggers.put(from.getNodeId(), 1);
                } else {
                    this.triggers.put(from.getNodeId(), count.intValue() + 1);
                }
                if (checkAllActivated()) {
                    decreaseAllTriggers();
                    triggerCompleted();

                }
                break;
            case Join.TYPE_DISCRIMINATOR:
                boolean triggerCompleted = triggers.isEmpty();
                triggers.put(from.getNodeId(), new Integer(1));
                if (checkAllActivated()) {
                    resetAllTriggers();
                }
                if (triggerCompleted) {
                    triggerCompleted();
                }
                break;
            case Join.TYPE_N_OF_M:
                count = (Integer) this.triggers.get(from.getNodeId());
                if (count == null) {
                    this.triggers.put(from.getNodeId(), 1);
                } else {
                    this.triggers.put(from.getNodeId(), count.intValue() + 1);
                }
                int counter = 0;
                for (final Connection connection : getJoin().getDefaultIncomingConnections()) {
                    if (this.triggers.get(connection.getFrom().getId()) != null) {
                        counter++;
                    }
                }
                String n = join.getN();
                Integer number = null;
                if (n.startsWith("#{") && n.endsWith("}")) {
                    n = n.substring(2, n.length() - 1);
                    VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                            VariableScope.VARIABLE_SCOPE, n);
                    if (variableScopeInstance == null) {
                        throw new IllegalArgumentException("Could not find variable " + n + " when executing join.");
                    }
                    Object value = variableScopeInstance.getVariable(n);
                    if (value instanceof Number) {
                        number = ((Number) value).intValue();
                    } else {
                        throw new IllegalArgumentException(
                                "Variable " + n + " did not return a number when executing join: " + value);
                    }
                } else {
                    number = Integer.parseInt(n);
                }
                if (counter >= number) {
                    resetAllTriggers();
                    NodeInstanceContainer nodeInstanceContainer = (NodeInstanceContainer) getNodeInstanceContainer();
                    cancelRemainingDirectFlows(nodeInstanceContainer, getJoin());
                    triggerCompleted();
                }
                break;
            case Join.TYPE_OR:
                NodeInstanceContainer nodeInstanceContainer = (NodeInstanceContainer) getNodeInstanceContainer();
                boolean activePathExists = existsActiveDirectFlow(nodeInstanceContainer, getJoin());
                if (!activePathExists) {
                    triggerCompleted();
                }
                break;
            default:
                throw new IllegalArgumentException("Illegal join type " + join.getType());
        }
    }

    private boolean checkAllActivated() {
        // check whether all parent nodes have been triggered
        for (final Connection connection : getJoin().getDefaultIncomingConnections()) {
            if (this.triggers.get(connection.getFrom().getId()) == null) {
                return false;
            }
        }
        return true;
    }

    private void decreaseAllTriggers() {
        // decrease trigger count for all incoming connections
        for (final Connection connection : getJoin().getDefaultIncomingConnections()) {
            final Integer count = (Integer) this.triggers.get(connection.getFrom().getId());
            if (count.intValue() == 1) {
                this.triggers.remove(connection.getFrom().getId());
            } else {
                this.triggers.put(connection.getFrom().getId(), count.intValue() - 1);
            }
        }
    }

    private boolean existsActiveDirectFlow(NodeInstanceContainer nodeInstanceContainer, final Node lookFor) {

        Collection<NodeInstance> activeNodeInstancesOrig = nodeInstanceContainer.getNodeInstances();
        List<NodeInstance> activeNodeInstances = new ArrayList<NodeInstance>(activeNodeInstancesOrig);
        // sort active instances in the way that lookFor nodeInstance will be last to
        // not finish too early
        Collections.sort(activeNodeInstances, new Comparator<NodeInstance>() {

            @Override
            public int compare(NodeInstance o1, NodeInstance o2) {
                if (o1.getNodeId() == lookFor.getId()) {
                    return 1;
                } else if (o2.getNodeId() == lookFor.getId()) {
                    return -1;
                }
                return 0;
            }
        });

        for (NodeInstance nodeInstance : activeNodeInstances) {
            // do not consider NodeInstanceContainers to be checked, enough to treat is as
            // black box
            if (((io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance).getLevel() != getLevel()) {
                continue;
            }
            Node node = nodeInstance.getNode();
            Set<Long> vistedNodes = new HashSet<Long>();
            checkNodes(vistedNodes, node, node, lookFor);
            if (vistedNodes.contains(lookFor.getId()) && !vistedNodes.contains(node.getId())) {
                return true;
            }
        }

        return false;
    }

    private boolean checkNodes(Set<Long> vistedNodes, Node startAt, Node currentNode, Node lookFor) {
        if (currentNode == null) {
            // for dynamic/ad hoc task there is no node
            return false;
        }

        List<Connection> connections = currentNode
                .getOutgoingConnections(io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
        // special handling for XOR split as it usually is used for arbitrary loops
        if (currentNode instanceof Split && ((Split) currentNode).getType() == Split.TYPE_XOR) {
            if (vistedNodes.contains(startAt.getId())) {
                return false;
            }
            for (Connection conn : connections) {
                Set<Long> xorCopy = new HashSet<Long>(vistedNodes);

                Node nextNode = conn.getTo();
                if (nextNode == null) {
                    continue;
                } else {
                    xorCopy.add(nextNode.getId());
                    if (nextNode.getId() != lookFor.getId()) {

                        checkNodes(xorCopy, currentNode, nextNode, lookFor);
                    }
                }

                if (xorCopy.contains(lookFor.getId())) {
                    vistedNodes.addAll(xorCopy);
                    return true;
                }

            }
        } else {
            for (Connection conn : connections) {
                Node nextNode = conn.getTo();
                if (nextNode == null) {
                    continue;
                } else {

                    if (vistedNodes.contains(nextNode.getId())) {
                        // we have already been here so let's continue
                        continue;
                    }
                    if (nextNode.getId() == lookFor.getId()) {
                        // we found the node that we are looking for, add it and continue to find out
                        // other parts
                        // as it could be part of a loop
                        vistedNodes.add(nextNode.getId());
                        continue;
                    }
                    vistedNodes.add(nextNode.getId());
                    if (startAt.getId() == nextNode.getId()) {
                        return true;
                    } else {
                        boolean nestedCheck = checkNodes(vistedNodes, startAt, nextNode, lookFor);
                        if (nestedCheck) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void resetAllTriggers() {
        triggers.clear();
    }

    public void triggerCompleted() {
        // join nodes are only removed from the container when they contain no more
        // state
        triggerCompleted(io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, triggers.isEmpty());
    }

    public Map<Long, Integer> getTriggers() {
        return triggers;
    }

    public void internalSetTriggers(Map<Long, Integer> triggers) {
        this.triggers = triggers;
    }

    private void cancelRemainingDirectFlows(NodeInstanceContainer nodeInstanceContainer, final Node lookFor) {

        Collection<NodeInstance> activeNodeInstancesOrig = nodeInstanceContainer.getNodeInstances();
        List<NodeInstance> activeNodeInstances = new ArrayList<NodeInstance>(activeNodeInstancesOrig);
        // sort active instances in the way that lookFor nodeInstance will be last to
        // not finish too early
        Collections.sort(activeNodeInstances, new Comparator<NodeInstance>() {

            @Override
            public int compare(NodeInstance o1, NodeInstance o2) {
                if (o1.getNodeId() == lookFor.getId()) {
                    return 1;
                } else if (o2.getNodeId() == lookFor.getId()) {
                    return -1;
                }
                return 0;
            }
        });

        for (NodeInstance nodeInstance : activeNodeInstances) {
            // do not consider NodeInstanceContainers to be checked, enough to treat is as
            // black box
            if (((io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance).getLevel() != getLevel()) {
                continue;
            }
            Node node = nodeInstance.getNode();
            Set<Long> vistedNodes = new HashSet<Long>();
            checkNodes(vistedNodes, node, node, lookFor);
            if (vistedNodes.contains(lookFor.getId()) && !vistedNodes.contains(node.getId())) {
                ((NodeInstanceImpl) nodeInstance).cancel();
            }
        }
    }
}
