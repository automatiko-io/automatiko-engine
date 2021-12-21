
package io.automatiko.engine.workflow.process.instance.node;

import static io.automatiko.engine.workflow.base.core.context.variable.VariableScope.VARIABLE_SCOPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.api.runtime.process.DataTransformer;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError;
import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.context.ProcessContext;
import io.automatiko.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatiko.engine.workflow.base.instance.ContextInstance;
import io.automatiko.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatiko.engine.workflow.base.instance.ContextableInstance;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.AssignmentAction;
import io.automatiko.engine.workflow.base.instance.impl.ContextInstanceFactory;
import io.automatiko.engine.workflow.base.instance.impl.ContextInstanceFactoryRegistry;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;
import io.automatiko.engine.workflow.process.core.node.Assignment;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.Transformation;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;

public class CompositeContextNodeInstance extends CompositeNodeInstance
        implements ContextInstanceContainer, ContextableInstance {

    private static final Logger logger = LoggerFactory.getLogger(CompositeContextNodeInstance.class);

    private static final long serialVersionUID = 510l;

    private Map<String, ContextInstance> contextInstances = new HashMap<String, ContextInstance>();
    private Map<String, List<ContextInstance>> subContextInstances = new HashMap<String, List<ContextInstance>>();

    protected CompositeContextNode getCompositeContextNode() {
        return (CompositeContextNode) getNode();
    }

    public ContextContainer getContextContainer() {
        return getCompositeContextNode();
    }

    public void setContextInstance(String contextId, ContextInstance contextInstance) {
        this.contextInstances.put(contextId, contextInstance);
    }

    public ContextInstance getContextInstance(String contextId) {
        ContextInstance contextInstance = this.contextInstances.get(contextId);
        if (contextInstance != null) {
            return contextInstance;
        }
        Context context = getCompositeContextNode().getDefaultContext(contextId);
        if (context != null) {
            contextInstance = getContextInstance(context);
            return contextInstance;
        }
        return null;
    }

    public List<ContextInstance> getContextInstances(String contextId) {
        return this.subContextInstances.get(contextId);
    }

    public void addContextInstance(String contextId, ContextInstance contextInstance) {
        List<ContextInstance> list = this.subContextInstances.get(contextId);
        if (list == null) {
            list = new ArrayList<ContextInstance>();
            this.subContextInstances.put(contextId, list);
        }
        list.add(contextInstance);
    }

    public void removeContextInstance(String contextId, ContextInstance contextInstance) {
        List<ContextInstance> list = this.subContextInstances.get(contextId);
        if (list != null) {
            list.remove(contextInstance);
        }
    }

    public ContextInstance getContextInstance(String contextId, long id) {
        List<ContextInstance> contextInstances = subContextInstances.get(contextId);
        if (contextInstances != null) {
            for (ContextInstance contextInstance : contextInstances) {
                if (contextInstance.getContextId() == id) {
                    return contextInstance;
                }
            }
        }
        return null;
    }

    public ContextInstance getContextInstance(final Context context) {
        ContextInstanceFactory conf = ContextInstanceFactoryRegistry.INSTANCE.getContextInstanceFactory(context);
        if (conf == null) {
            throw new IllegalArgumentException("Illegal context type (registry not found): " + context.getClass());
        }
        ContextInstance contextInstance = (ContextInstance) conf.getContextInstance(context, this,
                (ProcessInstance) getProcessInstance());
        if (contextInstance == null) {
            throw new IllegalArgumentException("Illegal context type (instance not found): " + context.getClass());
        }
        return contextInstance;
    }

    @Override
    public void internalTrigger(NodeInstance from, String type) {
        processInputMappings();
        super.internalTrigger(from, type);

    }

    @Override
    protected void internalTriggerOnlyParent(NodeInstance from, String type) {
        processInputMappings();
        super.internalTriggerOnlyParent(from, type);

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void triggerCompleted(String outType) {
        VariableScopeInstance compositeVariableScopeInstance = (VariableScopeInstance) getContextInstance(VARIABLE_SCOPE);

        for (DataAssociation association : getCompositeContextNode().getOutAssociations()) {
            if (association.getTransformation() != null) {
                Transformation transformation = association.getTransformation();
                DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
                if (transformer != null) {
                    Object parameterValue = transformer.transform(transformation.getCompiledExpression(),
                            compositeVariableScopeInstance.getVariables());
                    if (parameterValue != null) {
                        getProcessInstance().setVariable(association.getTarget(), parameterValue);
                    }
                }
            } else if (association.getAssignments() == null || association.getAssignments().isEmpty()) {
                Object parameterValue = null;
                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                        VARIABLE_SCOPE, association.getSources().get(0));
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
                    getProcessInstance().setVariable(association.getTarget(), parameterValue);
                }
            } else {
                association.getAssignments().stream()
                        .forEach(assignment -> handleAssignment(assignment, compositeVariableScopeInstance));
            }
        }
        super.triggerCompleted(outType);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void processInputMappings() {
        VariableScopeInstance compositeVariableScopeInstance = (VariableScopeInstance) getContextInstance(VARIABLE_SCOPE);

        for (DataAssociation association : getCompositeContextNode().getInAssociations()) {
            if (association.getTransformation() != null) {
                Transformation transformation = association.getTransformation();
                DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
                if (transformer != null) {
                    Object parameterValue = transformer.transform(transformation.getCompiledExpression(),
                            getProcessInstance().getVariables());
                    if (parameterValue != null) {
                        compositeVariableScopeInstance.setVariable(association.getTarget(), parameterValue);
                    }
                }
            } else if (association.getAssignments() == null || association.getAssignments().isEmpty()) {
                Object parameterValue = null;
                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                        VARIABLE_SCOPE, association.getSources().get(0));
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
                    compositeVariableScopeInstance.setVariable(association.getTarget(), parameterValue);
                }
            } else {
                association.getAssignments().stream()
                        .forEach(assignment -> handleAssignment(assignment, compositeVariableScopeInstance));
            }
        }
    }

    private void handleAssignment(Assignment assignment, VariableScopeInstance compositeVariableScopeInstance) {
        AssignmentAction action = (AssignmentAction) assignment.getMetaData("Action");
        try {
            ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime());
            context.setNodeInstance(this);
            action.execute(null, context);
        } catch (WorkItemExecutionError e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("unable to execute Assignment", e);
        }
    }
}
