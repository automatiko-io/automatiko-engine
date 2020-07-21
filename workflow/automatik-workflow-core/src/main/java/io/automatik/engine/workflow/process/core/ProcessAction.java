
package io.automatik.engine.workflow.process.core;

import java.util.HashMap;
import java.util.Map;

public class ProcessAction {

	private String name;
	private Map<String, Object> metaData = new HashMap<String, Object>();

	public void wire(Object object) {
		setMetaData("Action", object);
	}

	public void setMetaData(String name, Object value) {
		if ("Action".equals(name)) {
			this.metaData.putIfAbsent(name, value);
		} else {
			this.metaData.put(name, value);
		}
	}

	public Object getMetaData(String name) {
		return this.metaData.get(name);
	}

	public Object removeMetaData(String name) {
		return this.metaData.remove(name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
