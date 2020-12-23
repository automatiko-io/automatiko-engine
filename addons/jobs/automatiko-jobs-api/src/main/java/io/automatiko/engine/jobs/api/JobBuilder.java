
package io.automatiko.engine.jobs.api;

import java.time.ZonedDateTime;

public class JobBuilder {

	private String id;
	private ZonedDateTime expirationTime;
	private Integer priority;
	private String callbackEndpoint;
	private String processInstanceId;
	private String rootProcessInstanceId;
	private String processId;
	private String rootProcessId;
	private Long repeatInterval;
	private Integer repeatLimit;

	public JobBuilder id(String id) {
		this.id = id;
		return this;
	}

	public JobBuilder expirationTime(ZonedDateTime expirationTime) {
		this.expirationTime = expirationTime;
		return this;
	}

	public JobBuilder priority(Integer priority) {
		this.priority = priority;
		return this;
	}

	public JobBuilder callbackEndpoint(String callbackEndpoint) {
		this.callbackEndpoint = callbackEndpoint;
		return this;
	}

	public JobBuilder processInstanceId(String processInstanceId) {
		this.processInstanceId = processInstanceId;
		return this;
	}

	public JobBuilder rootProcessInstanceId(String rootProcessInstanceId) {
		this.rootProcessInstanceId = rootProcessInstanceId;
		return this;
	}

	public JobBuilder processId(String processId) {
		this.processId = processId;
		return this;
	}

	public JobBuilder rootProcessId(String rootProcessId) {
		this.rootProcessId = rootProcessId;
		return this;
	}

	public JobBuilder repeatInterval(Long repeatInterval) {
		this.repeatInterval = repeatInterval;
		return this;
	}

	public JobBuilder repeatLimit(Integer repeatLimit) {
		this.repeatLimit = repeatLimit;
		return this;
	}

	public Job build() {
		return new Job(id, expirationTime, priority, callbackEndpoint, processInstanceId, rootProcessInstanceId,
				processId, rootProcessId, repeatInterval, repeatLimit);
	}

	public static JobBuilder builder() {
		return new JobBuilder();
	}
}