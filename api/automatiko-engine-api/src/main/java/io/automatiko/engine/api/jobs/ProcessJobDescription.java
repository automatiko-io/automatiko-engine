
package io.automatiko.engine.api.jobs;

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.automatiko.engine.api.workflow.Process;

public class ProcessJobDescription implements JobDescription {

    public static final Integer DEFAULT_PRIORITY = 5;

    private final String id;

    private final ExpirationTime expirationTime;

    private final Integer priority;

    private String processId;

    private String processVersion;

    private Process<?> process;

    private ProcessJobDescription(ExpirationTime expirationTime, Integer priority, String processId,
            String processVersion) {
        this.id = UUID.nameUUIDFromBytes(processId.getBytes(StandardCharsets.UTF_8)).toString();
        this.expirationTime = requireNonNull(expirationTime);
        this.priority = requireNonNull(priority);
        this.processId = requireNonNull(processId);
        this.processVersion = processVersion;
    }

    public ProcessJobDescription(ExpirationTime expirationTime, Integer priority, Process<?> process) {
        this.id = UUID.nameUUIDFromBytes(process.id().getBytes(StandardCharsets.UTF_8)).toString();
        this.expirationTime = requireNonNull(expirationTime);
        this.priority = requireNonNull(priority);
        this.process = requireNonNull(process);
        this.processId = requireNonNull(process.id());
        this.processVersion = process.version();
    }

    public static ProcessJobDescription of(ExpirationTime expirationTime, Process<?> process) {
        return new ProcessJobDescription(expirationTime, DEFAULT_PRIORITY, process);
    }

    public static ProcessJobDescription of(ExpirationTime expirationTime, String processId, String processVersion) {
        return of(expirationTime, DEFAULT_PRIORITY, processId, processVersion);
    }

    public static ProcessJobDescription of(ExpirationTime expirationTime, Integer priority, String processId,
            String processVersion) {

        return new ProcessJobDescription(expirationTime, priority, processId, processVersion);
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

    public String processId() {
        return processId;
    }

    public String processVersion() {
        return processVersion;
    }

    public Process<?> process() {
        return process;
    }
}
