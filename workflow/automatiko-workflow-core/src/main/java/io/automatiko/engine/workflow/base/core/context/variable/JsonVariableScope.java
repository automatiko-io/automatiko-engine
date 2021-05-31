
package io.automatiko.engine.workflow.base.core.context.variable;

import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;

public class JsonVariableScope extends VariableScope {

    private static final long serialVersionUID = -4244473609877592595L;

    public static final String WORKFLOWDATA_KEY = "workflowdata";

    public JsonVariableScope() {
        super();
        this.internalVariables.add(new Variable("internal-workflowdata", WORKFLOWDATA_KEY, new ObjectDataType()));
    }

    @Override
    public Variable findVariable(String variableName) {
        Variable found = super.findVariable(variableName);

        if (found != null) {
            return found;
        }

        return new Variable(variableName, variableName, new ObjectDataType());
    }

}
