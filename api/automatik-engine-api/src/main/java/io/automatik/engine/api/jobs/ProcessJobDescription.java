
package io.automatik.engine.api.jobs;

import java.util.UUID;

import io.automatik.engine.api.workflow.Process;

import static java.util.Objects.requireNonNull;

public class ProcessJobDescription implements JobDescription {

	public static final Integer DEFAULT_PRIORITY = 5;

	private final String id;

	private final ExpirationTime expirationTime;

	private final Integer priority;

	private String processId;

	private Process<?> process;

	private ProcessJobDescription(ExpirationTime expirationTime, Integer priority, String processId) {
		this.id = UUID.randomUUID().toString();
		this.expirationTime = requireNonNull(expirationTime);
		this.priority = requireNonNull(priority);
		this.processId = requireNonNull(processId);
	}

	public ProcessJobDescription(ExpirationTime expirationTime, Integer priority, Process<?> process) {
		this.id = UUID.randomUUID().toString();
		this.expirationTime = requireNonNull(expirationTime);
		this.priority = requireNonNull(priority);
		this.process = requireNonNull(process);
	}

	public static ProcessJobDescription of(ExpirationTime expirationTime, Process<?> process) {
		return new ProcessJobDescription(expirationTime, DEFAULT_PRIORITY, process);
	}

	public static ProcessJobDescription of(ExpirationTime expirationTime, String processId) {
		return of(expirationTime, DEFAULT_PRIORITY, processId);
	}

	public static ProcessJobDescription of(ExpirationTime expirationTime, Integer priority, String processId) {

		return new ProcessJobDescription(expirationTime, priority, processId);
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

	public Process<?> process() {
		return process;
	}
}
