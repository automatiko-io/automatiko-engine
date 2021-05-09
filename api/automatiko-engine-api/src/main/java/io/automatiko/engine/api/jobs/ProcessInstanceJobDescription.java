
package io.automatiko.engine.api.jobs;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

public class ProcessInstanceJobDescription implements JobDescription {

    public static final Integer DEFAULT_PRIORITY = 5;

    private final String id;

    private final ExpirationTime expirationTime;

    private final Integer priority;

    private final String processInstanceId;
    private final String rootProcessInstanceId;
    private final String processId;
    private final String processVersion;
    private final String rootProcessId;

    private final String triggerType;

    private ProcessInstanceJobDescription(long timerId, ExpirationTime expirationTime, Integer priority,
            String processInstanceId, String rootProcessInstanceId, String processId, String processVersion,
            String rootProcessId, String triggerType) {
        this.id = UUID.randomUUID().toString() + "_" + timerId;
        this.expirationTime = requireNonNull(expirationTime);
        this.priority = requireNonNull(priority);
        this.processInstanceId = requireNonNull(processInstanceId);
        this.rootProcessInstanceId = rootProcessInstanceId;
        this.processId = processId;
        this.processVersion = processVersion;
        this.rootProcessId = rootProcessId;
        this.triggerType = requireNonNull(triggerType);
    }

    private ProcessInstanceJobDescription(String jobId, long timerId, ExpirationTime expirationTime, Integer priority,
            String processInstanceId, String rootProcessInstanceId, String processId, String processVersion,
            String rootProcessId, String triggerType) {
        this.id = jobId;
        this.expirationTime = requireNonNull(expirationTime);
        this.priority = requireNonNull(priority);
        this.processInstanceId = requireNonNull(processInstanceId);
        this.rootProcessInstanceId = rootProcessInstanceId;
        this.processId = processId;
        this.processVersion = processVersion;
        this.rootProcessId = rootProcessId;
        this.triggerType = requireNonNull(triggerType);
    }

    private ProcessInstanceJobDescription(String jobIdId, ExpirationTime expirationTime, Integer priority,
            String processInstanceId, String rootProcessInstanceId, String processId, String processVersion,
            String rootProcessId, String triggerType) {
        this.id = jobIdId;
        this.expirationTime = requireNonNull(expirationTime);
        this.priority = requireNonNull(priority);
        this.processInstanceId = requireNonNull(processInstanceId);
        this.rootProcessInstanceId = rootProcessInstanceId;
        this.processId = processId;
        this.processVersion = processVersion;
        this.rootProcessId = rootProcessId;
        this.triggerType = requireNonNull(triggerType);
    }

    public static ProcessInstanceJobDescription of(long timerId, ExpirationTime expirationTime,
            String processInstanceId, String processId, String processVersion) {
        return of(timerId, expirationTime, processInstanceId, null, processId, processVersion, null);
    }

    public static ProcessInstanceJobDescription of(String jobId, long timerId, ExpirationTime expirationTime,
            String processInstanceId, String rootProcessInstanceId, String processId, String processVersion,
            String rootProcessId) {
        return new ProcessInstanceJobDescription(jobId, timerId, expirationTime, DEFAULT_PRIORITY, processInstanceId,
                rootProcessInstanceId, processId, processVersion, rootProcessId, "timerTriggered");
    }

    public static ProcessInstanceJobDescription of(long timerId, ExpirationTime expirationTime,
            String processInstanceId, String rootProcessInstanceId, String processId, String processVersion,
            String rootProcessId) {
        return of(timerId, expirationTime, DEFAULT_PRIORITY, processInstanceId, rootProcessInstanceId, processId,
                processVersion, rootProcessId);
    }

    public static ProcessInstanceJobDescription of(long timerId, ExpirationTime expirationTime, Integer priority,
            String processInstanceId, String rootProcessInstanceId, String processId, String processVersion,
            String rootProcessId) {

        return new ProcessInstanceJobDescription(timerId, expirationTime, priority, processInstanceId,
                rootProcessInstanceId, processId, processVersion, rootProcessId, "timerTriggered");
    }

    public static ProcessInstanceJobDescription of(long timerId, String triggerType, ExpirationTime expirationTime,
            String processInstanceId, String processId, String processVersion) {
        return of(timerId, triggerType, expirationTime, processInstanceId, null, processId, processVersion, null);
    }

    public static ProcessInstanceJobDescription of(String jobId, String triggerType, ExpirationTime expirationTime,
            String processInstanceId, String processId, String processVersion) {
        return new ProcessInstanceJobDescription(jobId, expirationTime, DEFAULT_PRIORITY, processInstanceId,
                null, processId, processVersion, null, triggerType);
    }

    public static ProcessInstanceJobDescription of(long timerId, String triggerType, ExpirationTime expirationTime,
            String processInstanceId, String rootProcessInstanceId, String processId, String processVersion,
            String rootProcessId) {
        return of(timerId, triggerType, expirationTime, DEFAULT_PRIORITY, processInstanceId, rootProcessInstanceId, processId,
                processVersion, rootProcessId);
    }

    public static ProcessInstanceJobDescription of(String jobId, long timerId, String triggerType,
            ExpirationTime expirationTime,
            String processInstanceId, String rootProcessInstanceId, String processId, String processVersion,
            String rootProcessId) {
        return of(jobId, timerId, triggerType, expirationTime, DEFAULT_PRIORITY, processInstanceId, rootProcessInstanceId,
                processId,
                processVersion, rootProcessId);
    }

    public static ProcessInstanceJobDescription of(long timerId, String triggerType, ExpirationTime expirationTime,
            Integer priority,
            String processInstanceId, String rootProcessInstanceId, String processId, String processVersion,
            String rootProcessId) {

        return new ProcessInstanceJobDescription(timerId, expirationTime, priority, processInstanceId,
                rootProcessInstanceId, processId, processVersion, rootProcessId, triggerType);
    }

    public static ProcessInstanceJobDescription of(String jobId, long timerId, String triggerType,
            ExpirationTime expirationTime,
            Integer priority,
            String processInstanceId, String rootProcessInstanceId, String processId, String processVersion,
            String rootProcessId) {

        return new ProcessInstanceJobDescription(jobId, timerId, expirationTime, priority, processInstanceId,
                rootProcessInstanceId, processId, processVersion, rootProcessId, triggerType);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public ExpirationTime expirationTime() {
        return expirationTime;
    }

    @Override
    public Integer priority() {
        return priority;
    }

    public String processInstanceId() {
        return processInstanceId;
    }

    public String rootProcessInstanceId() {
        return rootProcessInstanceId;
    }

    public String processId() {
        return processId;
    }

    public String processVersion() {
        return processVersion;
    }

    public String rootProcessId() {
        return rootProcessId;
    }

    public String triggerType() {
        return triggerType;
    }

    @Override
    public String toString() {
        return "ProcessInstanceJobDescription{" + "id='" + id + '\'' + ", expirationTime=" + expirationTime
                + ", triggerType='" + triggerType + '\''
                + ", priority=" + priority + ", processInstanceId='" + processInstanceId + '\''
                + ", rootProcessInstanceId='" + rootProcessInstanceId + '\'' + ", processId='" + processId + '\''
                + ", rootProcessId='" + rootProcessId + '\'' + '}';
    }
}
