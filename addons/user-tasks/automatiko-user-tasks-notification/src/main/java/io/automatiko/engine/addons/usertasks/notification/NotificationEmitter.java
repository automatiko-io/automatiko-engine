package io.automatiko.engine.addons.usertasks.notification;

import java.util.Map;

import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.workflow.workitem.LifeCyclePhase;

/**
 * Emitts notification based on user tasks
 *
 */
public interface NotificationEmitter {

    /**
     * Invoke for a user task that transitions to given life cycle phase.
     * 
     * @param phase life cycle phase that user task transitioned to e.g. Active, Claim, Complete etc
     * @param data inputs/outputs of the user task at given life cycle phase
     * @param workItem definition of the user task that also give access to node instance and process instance
     */
    void notify(LifeCyclePhase phase, Map<String, Object> data, WorkItem workItem);
}
