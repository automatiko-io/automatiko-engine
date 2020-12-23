package io.automatiko.engine.workflow.process.executable.core.factory;

import io.automatiko.engine.api.workflow.datatype.DataType;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcessFactory;

public class VariableFactory {

    private Variable variable;
    protected ExecutableProcessFactory process;

    public VariableFactory(ExecutableProcessFactory process) {
        this.process = process;
        this.variable = new Variable();
    }

    public VariableFactory variable(String name, DataType type) {
        return variable("", name, type, null);
    }

    public VariableFactory variable(String name, DataType type, Object value) {
        return variable("", name, type, value, null, null);
    }

    public VariableFactory variable(String name, DataType type, String metaDataName, Object metaDataValue) {
        return variable("", name, type, null, metaDataName, metaDataValue);
    }

    public VariableFactory variable(String id, String name, DataType type) {
        return variable(id, name, type, null);
    }

    public VariableFactory variable(String id, String name, DataType type, Object value) {
        return variable(id, name, type, value, null, null);
    }

    public VariableFactory variable(String id, String name, DataType type, String metaDataName, Object metaDataValue) {
        return variable(id, name, type, null, metaDataName, metaDataValue);
    }

    public VariableFactory variable(String id, String name, DataType type, Object value, String metaDataName,
            Object metaDataValue) {
        variable.setId(id);
        variable.setName(name);
        variable.setType(type);
        variable.setValue(value);
        if (metaDataName != null && metaDataValue != null) {
            variable.setMetaData(metaDataName, metaDataValue);
        }

        return this;
    }

    public VariableFactory metaData(String name, Object value) {
        variable.setMetaData(name, value);
        return this;
    }

    public ExecutableProcessFactory done() {
        process.getExecutableProcess().getVariableScope().getVariables().add(variable);
        return this.process;
    }
}
