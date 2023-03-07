package io.automatiko.engine.addons.usertasks.notification;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemManager;
import io.automatiko.engine.api.workflow.workitem.LifeCyclePhase;
import io.automatiko.engine.api.workflow.workitem.Transition;
import io.automatiko.engine.workflow.base.instance.impl.humantask.BaseHumanTaskLifeCycle;

public class HumanTaskLifeCycleWithNotifications extends BaseHumanTaskLifeCycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(HumanTaskLifeCycleWithNotifications.class);

    private Collection<NotificationEmitter> notifications;

    public HumanTaskLifeCycleWithNotifications(Collection<NotificationEmitter> notifications) {
        this.notifications = notifications;
    }

    @Override
    public Map<String, Object> transitionTo(WorkItem workItem, WorkItemManager manager,
            Transition<Map<String, Object>> transition) {
        Map<String, Object> data = super.transitionTo(workItem, manager, transition);
        LifeCyclePhase targetPhase = phaseById(transition.phase());

        if (notifications != null && !notifications.isEmpty()) {
            for (NotificationEmitter emitter : notifications) {

                try {
                    emitter.notify(targetPhase, data, workItem);
                } catch (Throwable e) {
                    LOGGER.error("Unexpected error while notifying via {}", emitter, e);
                }
            }
        }

        return data;
    }
}
