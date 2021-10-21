package io.automatiko.engine.api.workflow;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.automatiko.engine.api.definition.process.Process;

public interface VariableInitializer {

    /**
     * Initializes new instance of the given variable either by creating empty instance or based
     * on configured default value
     * 
     * @param definition variable definition that should get value assigned
     * @param data current set of variables that can be used to compute the value
     * @return new instance of given class
     */
    Object initialize(Process process, Variable definition, Map<String, Object> data);

    /**
     * Returns set of known augmentors
     * 
     * @return all discovered augmentors available at runtime
     */
    default Set<VariableAugmentor> augmentors() {
        return Collections.emptySet();
    }

}
