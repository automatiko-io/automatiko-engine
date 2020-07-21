
package io.automatik.engine.api.event;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * This is an abstract implementation of the {@link DataEvent} that contains
 * basic common attributes referring to automatik processes metadata. This class
 * can be extended mainly by Services that need to publish events to be indexed
 * by the Data-Index service.
 *
 * @param <T> the payload
 */
public abstract class AbstractDataEvent<T> implements DataEvent<T> {

	private static final String SPEC_VERSION = "0.3";

	private String specversion;
	private String id;
	private String source;
	private String type;
	private String time;
	private T data;
	private String automatikProcessinstanceId;
	private String automatikRootProcessinstanceId;
	private String automatikProcessId;
	private String automatikRootProcessId;
	private String automatikAddons;

	public AbstractDataEvent(String type, String source, T body, String automatikProcessinstanceId,
			String automatikRootProcessinstanceId, String automatikProcessId, String automatikRootProcessId,
			String automatikAddons) {
		this.specversion = SPEC_VERSION;
		this.id = UUID.randomUUID().toString();
		this.source = source;
		this.type = type;
		this.time = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		this.data = body;

		this.automatikProcessinstanceId = automatikProcessinstanceId;
		this.automatikRootProcessinstanceId = automatikRootProcessinstanceId;
		this.automatikProcessId = automatikProcessId;
		this.automatikRootProcessId = automatikRootProcessId;
		this.automatikAddons = automatikAddons;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public String getSpecversion() {
		return specversion;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getTime() {
		return time;
	}

	@Override
	public T getData() {
		return data;
	}

	public String getAutomatikProcessinstanceId() {
		return automatikProcessinstanceId;
	}

	public String getAutomatikRootProcessinstanceId() {
		return automatikRootProcessinstanceId;
	}

	public String getAutomatikProcessId() {
		return automatikProcessId;
	}

	public String getAutomatikRootProcessId() {
		return automatikRootProcessId;
	}

	public String getAutomatikAddons() {
		return automatikAddons;
	}
}
