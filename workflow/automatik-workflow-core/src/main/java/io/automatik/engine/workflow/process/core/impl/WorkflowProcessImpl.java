
package io.automatik.engine.workflow.process.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;

import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.definition.process.NodeContainer;
import io.automatik.engine.workflow.base.core.impl.ProcessImpl;
import io.automatik.engine.workflow.base.instance.ProcessInstance;
import io.automatik.engine.workflow.process.core.WorkflowProcess;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatik.engine.workflow.process.instance.impl.ProcessInstanceResolverFactory;
import io.automatik.engine.workflow.util.PatternConstants;

/**
 * Default implementation of a RuleFlow process.
 *
 */
public class WorkflowProcessImpl extends ProcessImpl
        implements WorkflowProcess, io.automatik.engine.workflow.process.core.NodeContainer {

    private static final long serialVersionUID = 510l;
    private static final Logger logger = LoggerFactory.getLogger(WorkflowProcessImpl.class);

    private boolean autoComplete = false;
    private boolean dynamic = false;
    private boolean executable = false;
    private io.automatik.engine.workflow.process.core.NodeContainer nodeContainer;

    private transient BiFunction<String, ProcessInstance, String> expressionEvaluator = (expression, p) -> {

        String evaluatedValue = expression;
        Map<String, String> replacements = new HashMap<String, String>();
        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(evaluatedValue);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (replacements.get(paramName) == null) {
                try {
                    String value = (String) MVEL.eval(paramName,
                            new ProcessInstanceResolverFactory(((WorkflowProcessInstance) p)));
                    replacements.put(paramName, value);
                } catch (Throwable t) {
                    logger.error("Could not resolve, parameter {} while evaluating expression {}", paramName,
                            expression, t);
                }
            }
        }
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            evaluatedValue = evaluatedValue.replace("#{" + replacement.getKey() + "}", replacement.getValue());
        }

        return evaluatedValue;

    };

    public WorkflowProcessImpl() {
        nodeContainer = (io.automatik.engine.workflow.process.core.NodeContainer) createNodeContainer();
    }

    protected NodeContainer createNodeContainer() {
        return new NodeContainerImpl();
    }

    public Node[] getNodes() {
        return nodeContainer.getNodes();
    }

    public Node getNode(final long id) {
        return nodeContainer.getNode(id);
    }

    public Node internalGetNode(long id) {
        try {
            return getNode(id);
        } catch (IllegalArgumentException e) {
            if (dynamic) {
                return null;
            } else {
                throw e;
            }
        }
    }

    public void removeNode(final Node node) {
        nodeContainer.removeNode(node);
        ((io.automatik.engine.workflow.process.core.Node) node).setParentContainer(null);
    }

    public void addNode(final Node node) {
        nodeContainer.addNode(node);
        ((io.automatik.engine.workflow.process.core.Node) node).setParentContainer(this);
    }

    public boolean isAutoComplete() {
        return autoComplete;
    }

    public void setAutoComplete(boolean autoComplete) {
        this.autoComplete = autoComplete;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public boolean isExecutable() {
        return executable;
    }

    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    @Override
    public Integer getProcessType() {
        if (dynamic) {
            return CASE_TYPE;
        }
        return PROCESS_TYPE;
    }

    @Override
    public List<Node> getNodesRecursively() {
        List<Node> nodes = new ArrayList<>();

        processNodeContainer(nodeContainer, nodes);

        return nodes;
    }

    protected void processNodeContainer(io.automatik.engine.workflow.process.core.NodeContainer nodeContainer,
            List<Node> nodes) {

        for (Node node : nodeContainer.getNodes()) {
            nodes.add(node);
            if (node instanceof io.automatik.engine.workflow.process.core.NodeContainer) {
                processNodeContainer((io.automatik.engine.workflow.process.core.NodeContainer) node, nodes);
            }
        }
    }

    protected Node getContainerNode(Node currentNode,
            io.automatik.engine.workflow.process.core.NodeContainer nodeContainer, long nodeId) {
        for (Node node : nodeContainer.getNodes()) {
            if (nodeId == node.getId()) {
                return currentNode;
            } else {
                if (node instanceof io.automatik.engine.workflow.process.core.NodeContainer) {
                    return getContainerNode(node, (io.automatik.engine.workflow.process.core.NodeContainer) node,
                            nodeId);
                }
            }
        }
        return null;
    }

    public Node getParentNode(long nodeId) {
        return getContainerNode(null, nodeContainer, nodeId);
    }

    public List<StartNode> getTimerStart() {
        Node[] nodes = getNodes();

        List<StartNode> timerStartNodes = new ArrayList<StartNode>();
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] instanceof StartNode && ((StartNode) nodes[i]).getTimer() != null) {
                timerStartNodes.add((StartNode) nodes[i]);
            }
        }

        return timerStartNodes;
    }

    public void setExpressionEvaluator(BiFunction<String, ProcessInstance, String> expressionEvaluator) {
        this.expressionEvaluator = expressionEvaluator;
    }

    public String evaluateExpression(String metaData, ProcessInstance processInstance) {
        return this.expressionEvaluator.apply(metaData, processInstance);
    }
}
