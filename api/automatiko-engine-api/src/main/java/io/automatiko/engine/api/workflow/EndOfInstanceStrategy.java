package io.automatiko.engine.api.workflow;

public interface EndOfInstanceStrategy {

    enum Type {
        REMOVE,
        KEEP,
        ARCHIVE
    }

    /**
     * Indicates if the instance should be removed from the storage
     * 
     * @return true for removing it otherwise false
     */
    boolean shouldInstanceBeRemoved();

    /**
     * Indicates if the instance should be updated in the storage
     * 
     * @return true for updating it otherwise false
     */
    boolean shouldInstanceBeUpdated();

    /**
     * Performs additional work associated with the strategy on given instance
     * 
     * @param instance instance that is at its end
     */
    void perform(ProcessInstance<?> instance);
}
