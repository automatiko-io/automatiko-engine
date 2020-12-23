
package io.automatiko.engine.services.time;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 */
public class TimerInstance implements Serializable {

	/** Generated serial version UID */
	private static final long serialVersionUID = 9161292833931227195L;

	private String id;
	private long timerId;
	private long delay;
	private long period;
	private Date activated;
	private Date lastTriggered;
	private String processInstanceId;
	private int repeatLimit = -1;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getTimerId() {
		return timerId;
	}

	public void setTimerId(long timerId) {
		this.timerId = timerId;
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public long getPeriod() {
		return period;
	}

	public void setPeriod(long period) {
		this.period = period;
	}

	public Date getActivated() {
		return activated;
	}

	public void setActivated(Date activated) {
		this.activated = activated;
	}

	public void setLastTriggered(Date lastTriggered) {
		this.lastTriggered = lastTriggered;
	}

	public Date getLastTriggered() {
		return lastTriggered;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public int getRepeatLimit() {
		return repeatLimit;
	}

	public void setRepeatLimit(int stopAfter) {
		this.repeatLimit = stopAfter;
	}

	@Override
	public String toString() {
		return "TimerInstance [id=" + id + ", timerId=" + timerId + ", delay=" + delay + ", period=" + period
				+ ", activated=" + activated + ", lastTriggered=" + lastTriggered + ", processInstanceId="
				+ processInstanceId + "]";
	}

	public static TimerInstance with(long timerId, String id, Integer limit) {
		TimerInstance timerInstance = new TimerInstance();
		timerInstance.setId(id);
		timerInstance.setTimerId(timerId);
		timerInstance.setRepeatLimit(limit);

		return timerInstance;
	}

}
