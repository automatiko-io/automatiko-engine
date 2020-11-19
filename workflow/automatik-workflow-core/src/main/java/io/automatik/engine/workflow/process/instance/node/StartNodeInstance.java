
package io.automatik.engine.workflow.process.instance.node;

import static io.automatik.engine.workflow.base.core.context.variable.VariableScope.VARIABLE_SCOPE;
import static io.automatik.engine.workflow.process.executable.core.Metadata.HIDDEN;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Matcher;

import org.mvel2.MVEL;

import io.automatik.engine.api.runtime.process.DataTransformer;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.workflow.datatype.DataType;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.event.EventTransformer;
import io.automatik.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.base.instance.impl.util.VariableUtil;
import io.automatik.engine.workflow.process.core.node.DataAssociation;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.core.node.Transformation;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceImpl;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatik.engine.workflow.util.PatternConstants;

/**
 * Runtime counterpart of a start node.
 * 
 */
public class StartNodeInstance extends NodeInstanceImpl {

    private static final long serialVersionUID = 510l;

    public void internalTrigger(final NodeInstance from, String type) {
        if (type != null) {
            throw new IllegalArgumentException("A StartNode does not accept incoming connections!");
        }
        if (from != null) {
            throw new IllegalArgumentException("A StartNode can only be triggered by the process itself!");
        }

        triggerCompleted();
    }

    public void signalEvent(String type, Object event) {
        boolean hidden = false;
        if (getNode().getMetaData().get(HIDDEN) != null) {
            hidden = true;
        }
        InternalProcessRuntime runtime = getProcessInstance().getProcessRuntime();
        if (!hidden) {
            runtime.getProcessEventSupport().fireBeforeNodeTriggered(this, runtime);
        }

        String variableName = (String) getStartNode().getMetaData("TriggerMapping");
        if (!getStartNode().getOutAssociations().isEmpty()) {
            for (DataAssociation association : getStartNode().getOutAssociations()) {
                if (association.getTransformation() != null) {
                    Transformation transformation = association.getTransformation();
                    DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
                    if (transformer != null) {
                        Object parameterValue = transformer.transform(transformation.getCompiledExpression(),
                                Collections.singletonMap(association.getSources().get(0), event));
                        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                                VARIABLE_SCOPE, association.getTarget());
                        if (variableScopeInstance != null && parameterValue != null) {

                            variableScopeInstance.getVariableScope().validateVariable(
                                    getProcessInstance().getProcessName(), association.getTarget(), parameterValue);

                            variableScopeInstance.setVariable(this, association.getTarget(), parameterValue);
                        } else {
                            logger.warn("Could not find variable scope for variable {}", association.getTarget());
                            logger.warn("when trying to complete start node {}", getStartNode().getName());
                            logger.warn("Continuing without setting variable.");
                        }
                        if (parameterValue != null) {
                            variableScopeInstance.setVariable(this, association.getTarget(), parameterValue);
                        }
                    }
                } else if (association.getAssignments() == null || association.getAssignments().isEmpty()) {
                    VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                            VARIABLE_SCOPE, association.getTarget());
                    if (variableScopeInstance != null) {
                        Variable varDef = variableScopeInstance.getVariableScope()
                                .findVariable(association.getTarget());
                        DataType dataType = varDef.getType();
                        // exclude java.lang.Object as it is considered unknown type
                        if (!dataType.getStringType().endsWith("java.lang.Object")
                                && !dataType.getStringType().endsWith("Object") && event instanceof String) {
                            event = dataType.readValue((String) event);
                        } else {
                            variableScopeInstance.getVariableScope().validateVariable(
                                    getProcessInstance().getProcessName(), association.getTarget(), event);
                        }
                        variableScopeInstance.setVariable(this, association.getTarget(), event);
                    } else {
                        String output = association.getSources().get(0);
                        String target = association.getTarget();

                        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(target);
                        if (matcher.find()) {
                            String paramName = matcher.group(1);

                            String expression = VariableUtil.transformDotNotation(paramName, output);
                            NodeInstanceResolverFactory resolver = new NodeInstanceResolverFactory(this);
                            resolver.addExtraParameters(
                                    Collections.singletonMap(association.getSources().get(0), event));
                            Serializable compiled = MVEL.compileExpression(expression);
                            MVEL.executeExpression(compiled, resolver);
                        } else {
                            logger.warn("Could not find variable scope for variable {}", association.getTarget());
                            logger.warn("when trying to complete start node {}", getStartNode().getName());
                            logger.warn("Continuing without setting variable.");
                        }
                    }

                }
            }
        } else {
            if (variableName != null) {
                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                        VariableScope.VARIABLE_SCOPE, variableName);
                if (variableScopeInstance == null) {
                    throw new IllegalArgumentException("Could not find variable for start node: " + variableName);
                }

                EventTransformer transformer = getStartNode().getEventTransformer();
                if (transformer != null) {
                    event = transformer.transformEvent(event);
                }

                variableScopeInstance.setVariable(this, variableName, event);
            }
        }
        triggerCompleted();
        if (!hidden) {
            runtime.getProcessEventSupport().fireAfterNodeTriggered(this, runtime);
        }
    }

    public StartNode getStartNode() {
        return (StartNode) getNode();
    }

    public void triggerCompleted() {
        triggerTime = new Date();
        ((io.automatik.engine.workflow.process.instance.NodeInstanceContainer) getNodeInstanceContainer())
                .setCurrentLevel(getLevel());
        triggerCompleted(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, true);
    }
}
