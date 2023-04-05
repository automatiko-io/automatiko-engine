package io.automatiko.engine.addons.usertasks.notification;

import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.automatiko.engine.workflow.DefaultWorkItemHandlerConfig;
import io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskWorkItemHandler;

@ApplicationScoped
public class HumanTaskHandlerConfig extends DefaultWorkItemHandlerConfig {

    @Inject
    public HumanTaskHandlerConfig(Instance<NotificationEmitter> notifications) {
        register("Human Task",
                new HumanTaskWorkItemHandler(
                        new HumanTaskLifeCycleWithNotifications(notifications.stream().collect(Collectors.toList()))));
    }

}
