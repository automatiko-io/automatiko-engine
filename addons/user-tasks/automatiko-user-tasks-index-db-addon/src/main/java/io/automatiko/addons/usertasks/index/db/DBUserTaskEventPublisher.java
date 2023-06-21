package io.automatiko.addons.usertasks.index.db;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.addon.usertasks.index.UserTask;
import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.event.EventPublisher;
import io.automatiko.engine.services.event.UserTaskInstanceDataEvent;
import io.automatiko.engine.services.event.impl.UserTaskInstanceEventBody;
import io.automatiko.engine.workflow.base.instance.impl.humantask.phases.Claim;
import io.automatiko.engine.workflow.base.instance.impl.humantask.phases.Release;
import io.automatiko.engine.workflow.base.instance.impl.workitem.Active;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DBUserTaskEventPublisher implements EventPublisher {

    private String serviceUrl;

    private boolean keepCompleted;

    @Inject
    public DBUserTaskEventPublisher(
            @ConfigProperty(name = "quarkus.automatiko.service-url") Optional<String> serviceUrl,
            @ConfigProperty(name = "quarkus.automatiko.on-instance-end") Optional<String> onInstanceEnd) {
        this.serviceUrl = serviceUrl.orElse("");
        this.keepCompleted = onInstanceEnd.orElse("remove").equalsIgnoreCase("keep");
    }

    @Override
    @Transactional
    public void publish(DataEvent<?> event) {

        if (event instanceof UserTaskInstanceDataEvent) {
            UserTaskInstanceDataEvent uevent = (UserTaskInstanceDataEvent) event;
            UserTaskInstanceEventBody data = uevent.getData();

            UserTaskInfoEntity task = new UserTaskInfoEntity();

            task.setId(data.getId());
            task.setTaskName(data.getTaskName());
            task.setTaskDescription(data.getTaskDescription());
            task.setPotentialUsers(nullIfEmpty(data.getPotentialUsers()));
            task.setPotentialGroups(nullIfEmpty(data.getPotentialGroups()));
            task.setExcludedUsers(nullIfEmpty(data.getExcludedUsers()));
            task.setTaskPriority(data.getTaskPriority());
            task.setState(data.getState());
            task.setActualOwner(data.getActualOwner());
            task.setCompleteDate(data.getCompleteDate());
            task.setFormLink(this.serviceUrl + data.getFormLink());
            task.setProcessId(data.getProcessId());
            task.setProcessInstanceId(data.getProcessInstanceId());
            task.setRootProcessId(data.getRootProcessId());
            task.setRootProcessInstanceId(data.getRootProcessInstanceId());
            task.setReferenceId(data.getReferenceId());
            task.setReferenceName(data.getReferenceName());
            task.setStartDate(data.getStartDate());

            if (keepCompleted || isActive(task)) {
                UserTaskInfoEntity.persist(task);
            } else {
                UserTaskInfoEntity.deleteById(task.getId());
            }
        }

    }

    @Override
    public void publish(Collection<DataEvent<?>> events) {
        for (DataEvent<?> event : events) {
            publish(event);
        }
    }

    private Set<String> nullIfEmpty(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return null;
        }

        return set;
    }

    private boolean isActive(UserTask task) {
        return Active.STATUS.equalsIgnoreCase(task.getState()) || Claim.STATUS.equalsIgnoreCase(task.getState())
                || Release.STATUS.equalsIgnoreCase(task.getState());
    }

}
