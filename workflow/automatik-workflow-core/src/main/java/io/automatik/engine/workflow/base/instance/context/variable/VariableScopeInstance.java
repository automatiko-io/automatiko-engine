
package io.automatik.engine.workflow.base.instance.context.variable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.workflow.VariableViolationException;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.event.ProcessEventSupport;
import io.automatik.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.context.AbstractContextInstance;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.instance.node.CompositeContextNodeInstance;

/**
 * 
 */
public class VariableScopeInstance extends AbstractContextInstance {

    private static final long serialVersionUID = 510l;

    private Map<String, Object> variables = new HashMap<String, Object>();
    private transient String variableIdPrefix = null;
    private transient String variableInstanceIdPrefix = null;

    public String getContextType() {
        return VariableScope.VARIABLE_SCOPE;
    }

    public Object getVariable(String name) {

        Object value = internalGetVariable(name);
        if (value != null) {
            return value;
        }

        // support for processInstanceId and parentProcessInstanceId
        if ("processInstanceId".equals(name) && getProcessInstance() != null) {
            return getProcessInstance().getId();
        } else if ("parentProcessInstanceId".equals(name) && getProcessInstance() != null) {
            return getProcessInstance().getParentProcessInstanceId();
        }

        return null;
    }

    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public void setVariable(String name, Object value) {
        setVariable(null, name, value);
    }

    public void setVariable(NodeInstance nodeInstance, String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("The name of a variable may not be null!");
        }
        Object oldValue = getVariable(name);
        if (oldValue == null) {
            if (value == null) {
                return;
            }
        }

        // check if variable that is being set is readonly and has already been set
        if (oldValue != null && getVariableScope().isReadOnly(name)) {
            throw new VariableViolationException(getProcessInstance().getId(), name,
                    "Variable '" + name + "' is already set and is marked as read only");
        }
        ProcessEventSupport processEventSupport = ((InternalProcessRuntime) getProcessInstance().getProcessRuntime())
                .getProcessEventSupport();
        processEventSupport.fireBeforeVariableChanged((variableIdPrefix == null ? "" : variableIdPrefix + ":") + name,
                (variableInstanceIdPrefix == null ? "" : variableInstanceIdPrefix + ":") + name, oldValue, value,
                getVariableScope().tags(name), getProcessInstance(), nodeInstance,
                getProcessInstance().getProcessRuntime());
        internalSetVariable(name, value);
        processEventSupport.fireAfterVariableChanged((variableIdPrefix == null ? "" : variableIdPrefix + ":") + name,
                (variableInstanceIdPrefix == null ? "" : variableInstanceIdPrefix + ":") + name, oldValue, value,
                getVariableScope().tags(name), getProcessInstance(), nodeInstance,
                getProcessInstance().getProcessRuntime());
    }

    public void internalSetVariable(String name, Object value) {
        variables.put(name, value);
    }

    public Object internalGetVariable(String name) {
        return this.variables.get(name);
    }

    public VariableScope getVariableScope() {
        return (VariableScope) getContext();
    }

    public void setContextInstanceContainer(ContextInstanceContainer contextInstanceContainer) {
        super.setContextInstanceContainer(contextInstanceContainer);
        for (Variable variable : getVariableScope().getVariables()) {
            if (variable.getValue() != null) {
                setVariable(variable.getName(), variable.getValue());
            }
        }
        if (contextInstanceContainer instanceof CompositeContextNodeInstance) {
            this.variableIdPrefix = ((Node) ((CompositeContextNodeInstance) contextInstanceContainer).getNode())
                    .getUniqueId();
            this.variableInstanceIdPrefix = ((CompositeContextNodeInstance) contextInstanceContainer).getUniqueId();
        }
    }

    public void enforceRequiredVariables() {
        VariableScope variableScope = getVariableScope();
        for (Variable variable : variableScope.getVariables()) {
            if (variableScope.isRequired(variable.getName()) && !variables.containsKey(variable.getName())) {
                throw new VariableViolationException(getProcessInstance().getId(), variable.getName(),
                        "Variable '" + variable.getName() + "' is required but not set");
            }
        }
    }
}
