package io.automatiko.engine.api.workflow;

/**
 * Variable augmentor allows to perform modification to the variable bfore it is added to
 * variable scope instance. This usually acts like a interceptor to be able to
 * change variable value structure for example moving binary data of a file to data store
 * to offload the instance storage from big data sets
 *
 */
public interface VariableAugmentor {

    /**
     * Determines if given augmentor is capable of working on given values
     * 
     * @param variable variable definition
     * @param value value of the variable to be set/updated/removed
     * @return true if it can process it otherwise false
     */
    boolean accept(Variable variable, Object value);

    /**
     * Invoked when variable is going to be created for the first time
     * 
     * @param processId identifier of the process definition
     * @param processVersion version of the process definition
     * @param processInstanceId instance identifier of the process instance
     * @param variable variable definition
     * @param value value to be set
     */
    Object augmentOnCreate(String processId, String processVersion, String processInstanceId, Variable variable, Object value);

    /**
     * Invoked when variable is going to be updated
     * 
     * @param processId identifier of the process definition
     * @param processVersion version of the process definition
     * @param processInstanceId instance identifier of the process instance
     * @param variable variable definition
     * @param value value to be updated
     */
    Object augmentOnUpdate(String processId, String processVersion, String processInstanceId, Variable variable, Object value);

    /**
     * Invoked when variable is going to be removed
     * 
     * @param processId identifier of the process definition
     * @param processVersion version of the process definition
     * @param processInstanceId instance identifier of the process instance
     * @param variable variable definition
     * @param value value to be removed
     */
    void augmentOnDelete(String processId, String processVersion, String processInstanceId, Variable variable, Object value);
}
