package io.automatiko.engine.api.workflow;

import java.util.Map;

public interface VariableInitializer {

    /**
     * Initializes new instance of the given variable either by creating empty instance or based
     * on configured default value
     * 
     * @param definition variable definition that should get value assigned
     * @param data current set of variables that can be used to compute the value
     * @return new instance of given class
     */
    Object initialize(Variable definition, Map<String, Object> data);

}
