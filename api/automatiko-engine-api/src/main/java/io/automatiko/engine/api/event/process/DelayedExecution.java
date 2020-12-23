package io.automatiko.engine.api.event.process;

/**
 * Marker interface for event listeners to indicate it should not be invoked
 * directly but only upon successful completion of unit of work.
 *
 */
public interface DelayedExecution {

}
