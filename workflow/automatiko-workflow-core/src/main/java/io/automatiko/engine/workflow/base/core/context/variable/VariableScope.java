
package io.automatiko.engine.workflow.base.core.context.variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.core.context.AbstractContext;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.StringDataType;

public class VariableScope extends AbstractContext {

    public static final String VARIABLE_STRICT_ENABLED_PROPERTY = "org.jbpm.variable.strict";
    private static boolean variableStrictEnabled = Boolean
            .parseBoolean(System.getProperty(VARIABLE_STRICT_ENABLED_PROPERTY, Boolean.FALSE.toString()));

    public static final String VARIABLE_SCOPE = "VariableScope";

    private static final long serialVersionUID = 510l;

    private List<Variable> variables;

    private List<Variable> internalVariables;

    public VariableScope() {
        this.variables = new ArrayList<>();
        this.internalVariables = new ArrayList<Variable>();
        this.internalVariables.add(new Variable("internal-piid", "processInstanceId", new StringDataType()));
        this.internalVariables.add(new Variable("internal-ppiid", "parentProcessInstanceId", new StringDataType()));
        this.internalVariables.add(new Variable("internal-rpiid", "rootProcessInstanceId", new StringDataType()));
        this.internalVariables.add(new Variable("internal-piname", "processInstanceName", new StringDataType()));
        this.internalVariables.add(new Variable("internal-pid", "processId", new StringDataType()));
        this.internalVariables.add(new Variable("internal-rpid", "rootProcessId", new StringDataType()));
        this.internalVariables.add(new Variable("internal-bkey", "businessKey", new StringDataType()));
        this.internalVariables.add(new Variable("internal-ckey", "correlationKey", new StringDataType()));
    }

    public String getType() {
        return VariableScope.VARIABLE_SCOPE;
    }

    public List<Variable> getVariables() {
        return this.variables;
    }

    public void setVariables(final List<Variable> variables) {
        if (variables == null) {
            throw new IllegalArgumentException("Variables is null");
        }
        this.variables = variables;
    }

    public String[] getVariableNames() {
        return variables.stream().map(Variable::getName).toArray(String[]::new);
    }

    public Variable findVariable(String variableName) {
        if (variableName.contains(":")) {
            String[] items = variableName.split(":");
            variableName = items[0];
        }

        for (Variable variable : getVariables()) {
            if (variable.getName().equals(variableName) || variable.getId().equals(variableName)) {
                return variable;
            }
        }
        for (Variable variable : internalVariables) {
            if (variable.getName().equals(variableName) || variable.getId().equals(variableName)) {
                return variable;
            }
        }
        return systemOrEnvironmentVariable(variableName);
    }

    public Context resolveContext(Object param) {
        if (param instanceof String) {
            return findVariable((String) param) == null ? null : this;
        }
        throw new IllegalArgumentException("VariableScopes can only resolve variable names: " + param);
    }

    public void validateVariable(String processName, String name, Object value) {
        if (!variableStrictEnabled) {
            return;
        }
        Variable var = findVariable(name);
        if (var == null) {
            throw new IllegalArgumentException("Variable '" + name + "' is not defined in process " + processName);
        }
        if (var.getType() != null && value != null) {
            boolean isValidType = var.getType().verifyDataType(value);
            if (!isValidType) {
                throw new IllegalArgumentException("Variable '" + name + "' has incorrect data type expected:"
                        + var.getType().getStringType() + " actual:" + value.getClass().getName());
            }
        }
    }

    /*
     * mainly for test coverage to easily switch between settings
     */
    public static void setVariableStrictOption(boolean turnedOn) {
        variableStrictEnabled = turnedOn;
    }

    public static boolean isVariableStrictEnabled() {
        return variableStrictEnabled;
    }

    public boolean isReadOnly(String name) {
        Variable v = findVariable(name);

        if (v != null) {
            return v.hasTag(Variable.READONLY_TAG);
        }
        return false;
    }

    public boolean isRequired(String name) {
        Variable v = findVariable(name);

        if (v != null) {
            return v.hasTag(Variable.REQUIRED_TAG);
        }
        return false;
    }

    public boolean isNullable(String name) {
        Variable v = findVariable(name);

        if (v != null) {
            return !v.hasTag(Variable.NOT_NULL_TAG);
        }
        return true;
    }

    public List<String> tags(String name) {
        Variable v = findVariable(name);

        if (v != null) {
            return v.getTags();
        }
        return Collections.emptyList();
    }

    public void addVariable(Variable variable) {
        this.variables.add(variable);
    }

    public Variable systemOrEnvironmentVariable(String name) {
        String value = System.getProperty(name);

        if (value == null) {
            value = System.getenv(name.replaceAll("\\.", "_").toUpperCase());
        }
        if (value != null) {
            return new Variable(name, name, new StringDataType());
        }

        return null;
    }
}
