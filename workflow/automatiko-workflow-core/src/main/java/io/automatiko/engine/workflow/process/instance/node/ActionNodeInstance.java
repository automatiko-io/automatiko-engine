
package io.automatiko.engine.workflow.process.instance.node;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import io.automatiko.engine.api.runtime.process.DataTransformer;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.workflow.base.core.context.ProcessContext;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.Action;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.Transformation;
import io.automatiko.engine.workflow.process.instance.WorkflowRuntimeException;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceImpl;

/**
 * Runtime counterpart of an action node.
 * 
 */
public class ActionNodeInstance extends NodeInstanceImpl {

    private static final long serialVersionUID = 510l;

    protected ActionNode getActionNode() {
        return (ActionNode) getNode();
    }

    public void internalTrigger(final NodeInstance from, String type) {
        triggerTime = new Date();
        if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException("An ActionNode only accepts default incoming connections!");
        }
        Action action = (Action) getActionNode().getAction().getMetaData("Action");
        try {
            ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime());
            context.setNodeInstance(this);
            executeAction(action);
        } catch (WorkflowRuntimeException wre) {
            throw wre;
        } catch (Exception e) {
            // for the case that one of the following throws an exception
            // - the ProcessContext() constructor
            // - or context.setNodeInstance(this)
            throw new WorkflowRuntimeException(this, getProcessInstance(),
                    "Unable to execute Action: " + e.getMessage(), e);
        }
        triggerCompleted();
    }

    public void setOutputVariable(Object variable) {
        List<DataAssociation> outputs = getActionNode().getOutAssociations();
        if (outputs != null && !outputs.isEmpty()) {

            for (DataAssociation output : outputs) {

                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                        VariableScope.VARIABLE_SCOPE, getActionNode().getOutAssociations().get(0).getTarget());

                if (variableScopeInstance != null) {
                    if (output.getTransformation() != null) {
                        Transformation transformation = output.getTransformation();
                        DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
                        if (transformer != null) {
                            final Object currentValue = variable;
                            variable = transformer.transform(transformation.getCompiledExpression(),
                                    output.getSources().stream().collect(Collectors.toMap(s -> s, v -> currentValue)));
                        }
                    }

                    Variable var = variableScopeInstance.getVariableScope().getVariables().stream()
                            .filter(v -> v.getId().equals(output.getTarget())).findFirst().orElse(null);
                    if (var != null) {
                        variableScopeInstance.setVariable(var.getName(), variable);
                    } else {
                        variableScopeInstance.setVariable(getActionNode().getOutAssociations().get(0).getTarget(), variable);
                    }

                }
            }
        }
    }

    public void triggerCompleted() {
        triggerCompleted(io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, true);
    }

}
