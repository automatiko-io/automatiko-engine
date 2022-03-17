
package io.automatiko.engine.workflow.process.core.node;

import java.util.Optional;

import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.api.workflow.ProcessInstance;

public interface SubProcessFactory<T> {

    /**
     * Binds given context to the model
     * 
     * @param ctx actual context of a process execution
     * @return returns model build out of the context
     */
    T bind(ProcessContext ctx);

    /**
     * Creates new instance based on given model
     * 
     * @param initial model data model for process instance
     * @return returns non started process instance
     */
    ProcessInstance<T> createInstance(T model);

    /**
     * Unbinds given context into the model, meaning it copies context data
     * into the model
     * 
     * @param ctx actual context of a process execution
     * @param model current state of data model
     */
    void unbind(ProcessContext ctx, T model);

    /**
     * Aborts process instance with given instanceId
     * 
     * @param instanceId unique identifier of process instance to be aborted
     */
    default void abortInstance(String instanceId) {

    }

    /**
     * Attempts to find process instance based on given instanceId
     * 
     * @param instanceId unique identifier of the process instance
     * @return optional containing process instance
     */
    Optional<ProcessInstance<T>> findInstance(String instanceId);

    /**
     * Attempts to find process instance based on given instanceId
     * 
     * @param instanceId unique identifier of the process instance
     * @param status status the instance should be in
     * @return optional containing process instance
     */
    Optional<ProcessInstance<T>> findInstanceByStatus(String instanceId, int status);

}
