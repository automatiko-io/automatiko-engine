
package io.automatik.engine.api.jobs;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class ProcessInstanceJobDescription implements JobDescription {

	public static final Integer DEFAULT_PRIORITY = 5;

	private final String id;

	private final ExpirationTime expirationTime;

	private final Integer priority;

	private final String processInstanceId;
	private final String rootProcessInstanceId;
	private final String processId;
	private final String rootProcessId;

	private ProcessInstanceJobDescription(long timerId, ExpirationTime expirationTime, Integer priority,
			String processInstanceId, String rootProcessInstanceId, String processId, String rootProcessId) {
		this.id = UUID.randomUUID().toString() + "_" + timerId;
		this.expirationTime = requireNonNull(expirationTime);
		this.priority = requireNonNull(priority);
		this.processInstanceId = requireNonNull(processInstanceId);
		this.rootProcessInstanceId = rootProcessInstanceId;
		this.processId = processId;
		this.rootProcessId = rootProcessId;
	}

	public static ProcessInstanceJobDescription of(long timerId, ExpirationTime expirationTime,
			String processInstanceId, String processId) {
		return of(timerId, expirationTime, processInstanceId, null, processId, null);
	}

	public static ProcessInstanceJobDescription of(long timerId, ExpirationTime expirationTime,
			String processInstanceId, String rootProcessInstanceId, String processId, String rootProcessId) {
		return of(timerId, expirationTime, DEFAULT_PRIORITY, processInstanceId, rootProcessInstanceId, processId,
				rootProcessId);
	}

	public static ProcessInstanceJobDescription of(long timerId, ExpirationTime expirationTime, Integer priority,
			String processInstanceId, String rootProcessInstanceId, String processId, String rootProcessId) {

		return new ProcessInstanceJobDescription(timerId, expirationTime, priority, processInstanceId,
				rootProcessInstanceId, processId, rootProcessId);
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

	public String rootProcessId() {
		return rootProcessId;
	}

	@Override
	public String toString() {
		return "ProcessInstanceJobDescription{" + "id='" + id + '\'' + ", expirationTime=" + expirationTime
				+ ", priority=" + priority + ", processInstanceId='" + processInstanceId + '\''
				+ ", rootProcessInstanceId='" + rootProcessInstanceId + '\'' + ", processId='" + processId + '\''
				+ ", rootProcessId='" + rootProcessId + '\'' + '}';
	}
}
