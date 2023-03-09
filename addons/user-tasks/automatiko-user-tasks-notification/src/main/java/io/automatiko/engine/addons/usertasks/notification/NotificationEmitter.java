package io.automatiko.engine.addons.usertasks.notification;

import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;

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

    /**
     * Verifies if given work item has notifications disabled or they are globally disabled
     * 
     * @param workItem work item that notifications should be emitted for
     * @return true in case it is disabled globally or for work item only otherwise false
     */
    default boolean isDisabled(WorkItem workItem) {
        // first check if notifications are globally disabled 
        if (ConfigProvider.getConfig().getOptionalValue("quarkus.automatiko.notifications.disabled", Boolean.class)
                .orElse(false)) {
            return true;
        }
        if (workItem.getNodeInstance() != null) {
            // then check if it is disabled on given work item
            return "disabled".equalsIgnoreCase((String) workItem.getNodeInstance().getNode().getMetaData().get("notification"));
        }

        return false;
    }
}
