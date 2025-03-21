package io.automatiko.addons.usertasks.index.mongo;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;

import io.automatiko.addon.usertasks.index.UserTask;
import io.automatiko.addon.usertasks.index.UserTaskInfo;
import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.event.EventPublisher;
import io.automatiko.engine.services.event.UserTaskInstanceDataEvent;
import io.automatiko.engine.services.event.impl.UserTaskInstanceEventBody;
import io.automatiko.engine.workflow.base.instance.impl.humantask.phases.Claim;
import io.automatiko.engine.workflow.base.instance.impl.humantask.phases.Release;
import io.automatiko.engine.workflow.base.instance.impl.workitem.Active;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MongoDBUserTaskEventPublisher implements EventPublisher {

    public static final String DATABASE_KEY = "quarkus.automatiko.persistence.mongodb.database";

    private MongoClient mongoClient;

    private String serviceUrl;

    private boolean keepCompleted;

    private ObjectMapper mapper;

    private Optional<String> database;

    @Inject
    public MongoDBUserTaskEventPublisher(MongoClient mongoClient, ObjectMapper mapper,
            @ConfigProperty(name = "quarkus.automatiko.service-url") Optional<String> serviceUrl,
            @ConfigProperty(name = "quarkus.automatiko.on-instance-end") Optional<String> onInstanceEnd,
            @ConfigProperty(name = DATABASE_KEY) Optional<String> database) {
        this.mongoClient = mongoClient;
        this.database = database;
        this.mapper = mapper;
        this.serviceUrl = serviceUrl.orElse("");
        this.keepCompleted = onInstanceEnd.orElse("remove").equalsIgnoreCase("keep");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void publish(DataEvent<?> event) {

        if (event instanceof UserTaskInstanceDataEvent) {
            UserTaskInstanceDataEvent uevent = (UserTaskInstanceDataEvent) event;
            UserTaskInstanceEventBody data = uevent.getData();

            UserTaskInfo task = new UserTaskInfo();

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
            task.setInputs(mapper.convertValue(data.getInputs(), Map.class));
            task.setOutputs(mapper.convertValue(data.getOutputs(), Map.class));
            task.setProcessId(data.getProcessId());
            task.setProcessInstanceId(data.getProcessInstanceId());
            task.setRootProcessId(data.getRootProcessId());
            task.setRootProcessInstanceId(data.getRootProcessInstanceId());
            task.setReferenceId(data.getReferenceId());
            task.setReferenceName(data.getReferenceName());
            task.setStartDate(data.getStartDate());

            MongoCollection<UserTaskInfo> usertasks = collection();

            if (keepCompleted || isActive(task)) {
                usertasks.findOneAndReplace(Filters.eq("_id", task.getId()), task, new FindOneAndReplaceOptions().upsert(true));
            } else {
                usertasks.findOneAndDelete(Filters.eq("_id", task.getId()));
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

    protected MongoCollection<UserTaskInfo> collection() {
        return mongoClient.getDatabase(this.database.orElse("automatiko"))
                .withCodecRegistry(CodecRegistries.fromProviders(MongoClientSettings.getDefaultCodecRegistry(),
                        PojoCodecProvider.builder().register(UserTaskInfo.class).build()))
                .getCollection("usertasks",
                        UserTaskInfo.class);

    }
}
