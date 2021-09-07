
package io.automatiko.engine.api.event.process;

/**
 * An event when a SLA has been violated.
 */
public interface ProcessSignaledEvent extends ProcessEvent {

    /**
     * The signal that was used to trigger process instance
     *
     * @return the signal
     */
    String getSignal();

    /**
     * Optional data object used as part of the signal operation
     * 
     * @return data object if available otherwise null
     */
    Object getData();

}
