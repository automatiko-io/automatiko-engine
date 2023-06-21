package io.automatiko.addons.usertasks.index.fs;

import java.util.Date;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.MultiValueNullableAttribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.attribute.SimpleNullableAttribute;
import com.googlecode.cqengine.persistence.support.serialization.PersistenceConfig;
import com.googlecode.cqengine.query.option.QueryOptions;

import io.automatiko.addon.usertasks.index.UserTaskInfo;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@PersistenceConfig(serializer = KryoSerializer.class)
public class CQEngineUserTaskInfo extends UserTaskInfo {

    public static final SimpleAttribute<CQEngineUserTaskInfo, String> TASK_ID = new SimpleAttribute<>("id") {
        public String getValue(CQEngineUserTaskInfo task, QueryOptions queryOptions) {
            return task.getId();
        }
    };

    public static final SimpleNullableAttribute<CQEngineUserTaskInfo, String> TASK_PRIORITY = new SimpleNullableAttribute<>(
            "taskPriority") {
        public String getValue(CQEngineUserTaskInfo task, QueryOptions queryOptions) {
            return task.getTaskPriority();
        }
    };

    public static final SimpleAttribute<CQEngineUserTaskInfo, String> TASK_STATE = new SimpleAttribute<>("taskState") {
        public String getValue(CQEngineUserTaskInfo task, QueryOptions queryOptions) {
            return task.getState();
        }
    };

    public static final SimpleNullableAttribute<CQEngineUserTaskInfo, String> TASK_NAME = new SimpleNullableAttribute<>(
            "taskName") {
        public String getValue(CQEngineUserTaskInfo task, QueryOptions queryOptions) {
            return task.getTaskName();
        }
    };

    public static final SimpleNullableAttribute<CQEngineUserTaskInfo, String> TASK_DESCRIPTION = new SimpleNullableAttribute<>(
            "taskDescription") {
        public String getValue(CQEngineUserTaskInfo task, QueryOptions queryOptions) {
            return task.getTaskDescription();
        }
    };

    public static final SimpleAttribute<CQEngineUserTaskInfo, Date> TASK_START_DATE = new SimpleAttribute<>("startDate") {
        public Date getValue(CQEngineUserTaskInfo task, QueryOptions queryOptions) {
            return task.getStartDate();
        }
    };

    public static final Attribute<CQEngineUserTaskInfo, String> POT_OWNERS = new MultiValueNullableAttribute<>(
            "potentialUsers", true) {
        public Iterable<String> getNullableValues(CQEngineUserTaskInfo task, QueryOptions queryOptions) {
            return task.getPotentialUsers();
        }
    };

    public static final Attribute<CQEngineUserTaskInfo, String> POT_GROUPS = new MultiValueNullableAttribute<>(
            "potentialGroups", true) {
        public Iterable<String> getNullableValues(CQEngineUserTaskInfo task, QueryOptions queryOptions) {
            return task.getPotentialGroups();
        }
    };

    public static final Attribute<CQEngineUserTaskInfo, String> EXCLUDED_USERS = new MultiValueNullableAttribute<>(
            "excludedUsers", true) {
        public Iterable<String> getNullableValues(CQEngineUserTaskInfo task, QueryOptions queryOptions) {
            return task.getExcludedUsers();
        }
    };

}
