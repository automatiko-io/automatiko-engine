package io.automatik.engine.addons.jobs.management.fs;

import java.time.ZonedDateTime;

public class ScheduledJob {

	private String id;
	private String processId;
	private boolean removeAtExecution;
	private String processInstanceId;
	private Integer limit;
	private Long reapeatInterval;

	private ZonedDateTime fireTime;

	public ScheduledJob() {

	}

	/**
	 * Used by process timer - one that starts new instances on each expiration
	 * 
	 * @param id                the job id
	 * @param processId         process definition id
	 * @param removeAtExecution indicates of timer should be finished at execution
	 * @param limit             max number of repeats
	 * @param reapeatInterval   interval between repeats
	 * @param fireTime          exact date and time when to fire
	 */
	public ScheduledJob(String id, String processId, boolean removeAtExecution, Integer limit, Long reapeatInterval,
			ZonedDateTime fireTime) {
		this.id = id;
		this.processId = processId;
		this.removeAtExecution = removeAtExecution;
		this.limit = limit;
		this.reapeatInterval = reapeatInterval;
		this.fireTime = fireTime;
	}

	/**
	 * Used by process instance timer - one that triggers/signals existing process
	 * instance
	 * 
	 * @param id                the job id
	 * @param processId         process definition id
	 * @param removeAtExecution indicates of timer should be finished at execution
	 * @param processInstanceId - unique process instance id that job should
	 *                          trigger/signal upon execution
	 * @param limit             max number of repeats
	 * @param reapeatInterval   interval between repeats
	 * @param fireTime          exact date and time when to fire
	 */
	public ScheduledJob(String id, String processId, boolean removeAtExecution, String processInstanceId, Integer limit,
			Long reapeatInterval, ZonedDateTime fireTime) {
		this.id = id;
		this.processId = processId;
		this.removeAtExecution = removeAtExecution;
		this.processInstanceId = processInstanceId;
		this.limit = limit;
		this.reapeatInterval = reapeatInterval;
		this.fireTime = fireTime;
	}

	public boolean isRemoveAtExecution() {
		return removeAtExecution;
	}

	public void setRemoveAtExecution(boolean removeAtExecution) {
		this.removeAtExecution = removeAtExecution;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public Long getReapeatInterval() {
		return reapeatInterval;
	}

	public void setReapeatInterval(Long reapeatInterval) {
		this.reapeatInterval = reapeatInterval;
	}

	public String getId() {
		return id;
	}

	public String getProcessId() {
		return processId;
	}

	public ZonedDateTime getFireTime() {
		return fireTime;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	public void setFireTime(ZonedDateTime fireTime) {
		this.fireTime = fireTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScheduledJob other = (ScheduledJob) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ScheduledJob [id=" + id + ", processId=" + processId + ", removeAtExecution=" + removeAtExecution
				+ ", processInstanceId=" + processInstanceId + ", limit=" + limit + ", reapeatInterval="
				+ reapeatInterval + ", fireTime=" + fireTime + "]";
	}
}
