
package io.automatik.engine.workflow.process.core.node;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Assignment implements Serializable {

	private static final long serialVersionUID = 5L;

	private String dialect;
	private String from;
	private String to;
	private Map<String, Object> metaData = new HashMap<String, Object>();

	public Assignment(String dialect, String from, String to) {
		this.dialect = dialect;
		this.from = from;
		this.to = to;
	}

	public String getDialect() {
		return dialect;
	}

	public void setDialect(String dialect) {
		this.dialect = dialect;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public void setMetaData(String name, Object value) {
		this.metaData.put(name, value);
	}

	public Object getMetaData(String name) {
		return this.metaData.get(name);
	}
}
