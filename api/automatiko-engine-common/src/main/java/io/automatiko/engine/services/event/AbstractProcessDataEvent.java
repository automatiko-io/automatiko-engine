
package io.automatiko.engine.services.event;

import io.automatiko.engine.api.event.AbstractDataEvent;

public abstract class AbstractProcessDataEvent<T> extends AbstractDataEvent<T> {

	protected String automatikParentProcessinstanceId;
	protected String automatikProcessinstanceState;
	protected String automatikReferenceId;

	public AbstractProcessDataEvent(String source, T body, String automatikProcessinstanceId,
			String automatikParentProcessinstanceId, String automatikRootProcessinstanceId, String automatikProcessId,
			String automatikRootProcessId, String automatikProcessinstanceState, String automatikAddons) {
		this(null, source, body, automatikProcessinstanceId, automatikParentProcessinstanceId,
				automatikRootProcessinstanceId, automatikProcessId, automatikRootProcessId,
				automatikProcessinstanceState, automatikAddons);
	}

	public AbstractProcessDataEvent(String type, String source, T body, String automatikProcessinstanceId,
			String automatikParentProcessinstanceId, String automatikRootProcessinstanceId, String automatikProcessId,
			String automatikRootProcessId, String automatikProcessinstanceState, String automatikAddons) {
		super(type, source, body, automatikProcessinstanceId, automatikRootProcessinstanceId, automatikProcessId,
				automatikRootProcessId, automatikAddons);
		this.automatikParentProcessinstanceId = automatikParentProcessinstanceId;
		this.automatikProcessinstanceState = automatikProcessinstanceState;
	}

	public String getAutomatikParentProcessinstanceId() {
		return automatikParentProcessinstanceId;
	}

	public String getAutomatikProcessinstanceState() {
		return automatikProcessinstanceState;
	}

	public String getAutomatikReferenceId() {
		return this.automatikReferenceId;
	}

	public void setAutomatikReferenceId(String automatikReferenceId) {
		this.automatikReferenceId = automatikReferenceId;
	}
}
