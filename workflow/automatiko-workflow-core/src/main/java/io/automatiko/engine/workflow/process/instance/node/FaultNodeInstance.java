
package io.automatiko.engine.workflow.process.instance.node;

import static io.automatiko.engine.workflow.process.executable.core.Metadata.UNIQUE_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.api.runtime.process.DataTransformer;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.context.exception.ExceptionScopeInstance;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.AssignmentAction;
import io.automatiko.engine.workflow.base.instance.impl.workitem.WorkItemImpl;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.core.node.Assignment;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.FaultNode;
import io.automatiko.engine.workflow.process.core.node.Transformation;
import io.automatiko.engine.workflow.process.instance.NodeInstanceContainer;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceImpl;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

/**
 * Runtime counterpart of a fault node.
 * 
 */
public class FaultNodeInstance extends NodeInstanceImpl {

    private static final long serialVersionUID = 510l;
    private static final Logger logger = LoggerFactory.getLogger(FaultNodeInstance.class);

    protected FaultNode getFaultNode() {
        return (FaultNode) getNode();
    }

    public void internalTrigger(final NodeInstance from, String type) {
        if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException("A FaultNode only accepts default incoming connections!");
        }
        triggerTime = new Date();
        if (getProcessInstance().isFunctionFlow(this) && getNodeInstanceContainer() instanceof ProcessInstance) {
            // only when running as function flow and node is in the top level node container meaning process instance
            // and not subprocesses
            getProcessInstance().getMetaData().compute("ATK_FUNC_FLOW_NEXT", (k, v) -> {

                if (v == null) {
                    v = new ArrayList<String>();
                }
                Process process = getProcessInstance().getProcess();
                String version = "";
                if (process.getVersion() != null && !process.getVersion().trim().isEmpty()) {
                    version = ".v" + process.getVersion().replaceAll("\\.", "_");
                }
                String defaultNextNode = process.getPackageName() + "." + process.getId() + version + "."
                        + getNodeName().toLowerCase();

                ((List<String>) v).add((String) getNode().getMetaData().getOrDefault("functionType", defaultNextNode));

                return v;
            });
        }
        String faultName = getFaultName();
        ExceptionScopeInstance exceptionScopeInstance = getExceptionScopeInstance(faultName);
        NodeInstanceContainer nodeInstanceContainer = (NodeInstanceContainer) getNodeInstanceContainer();
        nodeInstanceContainer.removeNodeInstance(this);
        boolean exceptionHandled = false;
        if (getFaultNode().isTerminateParent()) {
            // handle exception before canceling nodes to allow boundary event to catch the
            // events
            if (exceptionScopeInstance != null) {
                exceptionHandled = true;
                handleException(faultName, exceptionScopeInstance);
            }
            if (nodeInstanceContainer instanceof CompositeNodeInstance) {

                ((CompositeNodeInstance) nodeInstanceContainer).cancel();
            } else if (nodeInstanceContainer instanceof WorkflowProcessInstance) {
                Collection<NodeInstance> nodeInstances = ((WorkflowProcessInstance) nodeInstanceContainer)
                        .getNodeInstances();
                for (NodeInstance nodeInstance : nodeInstances) {
                    ((io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance).cancel();
                }
            }
        }
        String uniqueId = (String) getNode().getMetaData().get(UNIQUE_ID);
        if (uniqueId == null) {
            uniqueId = ((NodeImpl) getNode()).getUniqueId();
        }
        ((WorkflowProcessInstanceImpl) getProcessInstance()).addCompletedNodeId(uniqueId);
        if (exceptionScopeInstance != null) {
            if (!exceptionHandled) {
                handleException(faultName, exceptionScopeInstance);
            }
            boolean hidden = false;
            if (getNode().getMetaData().get("hidden") != null) {
                hidden = true;
            }
            if (!hidden) {
                InternalProcessRuntime runtime = getProcessInstance().getProcessRuntime();
                runtime.getProcessEventSupport().fireBeforeNodeLeft(this, runtime);
            }

            ((NodeInstanceContainer) getNodeInstanceContainer()).nodeInstanceCompleted(this, null);

            if (!hidden) {
                InternalProcessRuntime runtime = getProcessInstance().getProcessRuntime();
                runtime.getProcessEventSupport().fireAfterNodeLeft(this, runtime);
            }
        } else {

            ((ProcessInstance) getProcessInstance()).setState(ProcessInstance.STATE_ABORTED, faultName, getFaultData());

        }
    }

    protected ExceptionScopeInstance getExceptionScopeInstance(String faultName) {
        return (ExceptionScopeInstance) resolveContextInstance(ExceptionScope.EXCEPTION_SCOPE, faultName);
    }

    protected String getFaultName() {
        return getFaultNode().getFaultName();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Object getFaultData() {
        Object value = null;
        FaultNode faultNode = getFaultNode();
        String faultVariable = faultNode.getFaultVariable();

        if (faultNode.getInAssociations() != null && !faultNode.getInAssociations().isEmpty()) {
            Map<String, Object> faultData = new LinkedHashMap<>();
            for (DataAssociation association : faultNode.getInAssociations()) {
                if (association.getTransformation() != null) {
                    Transformation transformation = association.getTransformation();
                    DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
                    if (transformer != null) {
                        Object parameterValue = transformer.transform(transformation.getCompiledExpression(),
                                getSourceParameters(association));
                        if (parameterValue != null) {
                            faultData.put(association.getTarget(), parameterValue);
                        }
                    }
                } else if (association.getAssignments() == null || association.getAssignments().isEmpty()) {
                    Object parameterValue = null;
                    VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                            VariableScope.VARIABLE_SCOPE,
                            association.getSources().get(0));
                    if (variableScopeInstance != null) {
                        parameterValue = variableScopeInstance.getVariable(association.getSources().get(0));
                    } else {
                        try {
                            ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) getProcessInstance()
                                    .getProcess())
                                            .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);
                            parameterValue = evaluator.evaluate(association.getSources().get(0),
                                    new NodeInstanceResolverFactory(this));
                        } catch (Throwable t) {
                            logger.error("Could not find variable scope for variable {}", association.getSources().get(0));
                            logger.error("Continuing without setting parameter.");
                        }
                    }
                    if (parameterValue != null) {
                        faultData.put(association.getTarget(), parameterValue);
                    }
                } else {

                    ProcessContext context = new io.automatiko.engine.workflow.base.core.context.ProcessContext(
                            getProcessInstance().getProcessRuntime());
                    association.getAssignments().stream().forEach(a -> handleAssignment(context, faultData, a));
                }
            }
            value = faultData;

        } else if (faultVariable != null) {

            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                    VariableScope.VARIABLE_SCOPE, faultVariable);
            if (variableScopeInstance != null) {
                value = variableScopeInstance.getVariable(faultVariable);
            } else {
                logger.error("Could not find variable scope for variable {}", faultVariable);
                logger.error("when trying to execute fault node {}", getFaultNode().getName());
                logger.error("Continuing without setting value.");
            }
        }
        return value;
    }

    protected void handleException(String faultName, ExceptionScopeInstance exceptionScopeInstance) {
        exceptionScopeInstance.handleException(this, faultName, getFaultData());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Map<String, Object> getSourceParameters(DataAssociation association) {
        Map<String, Object> parameters = new HashMap<>();
        for (String sourceParam : association.getSources()) {
            Object parameterValue = null;
            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                    VariableScope.VARIABLE_SCOPE,
                    sourceParam);
            if (variableScopeInstance != null) {
                parameterValue = variableScopeInstance.getVariable(sourceParam);
            } else {
                try {
                    ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) getProcessInstance()
                            .getProcess())
                                    .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);
                    parameterValue = evaluator.evaluate(sourceParam, new NodeInstanceResolverFactory(this));
                } catch (Throwable t) {
                    logger.warn("Could not find variable scope for variable {}", sourceParam);
                }
            }
            if (parameterValue != null) {
                parameters.put(sourceParam, parameterValue);
            }
        }

        return parameters;
    }

    private void handleAssignment(ProcessContext ctx, Map<String, Object> output, Assignment assignment) {
        AssignmentAction action = (AssignmentAction) assignment.getMetaData("Action");
        try {
            WorkItemImpl workItem = new WorkItemImpl();
            action.execute(workItem, ctx);

            output.putAll(workItem.getParameters());
        } catch (WorkItemExecutionError e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("unable to execute Assignment", e);
        }
    }

}
