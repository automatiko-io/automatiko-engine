
package io.automatiko.engine.workflow.bpmn2.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lane implements Serializable {

	private static final long serialVersionUID = 510l;

	private String id;
	private String name;
	private List<String> flowElementIds = new ArrayList<String>();
	private Map<String, Object> metaData = new HashMap<String, Object>();

	public Lane(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getFlowElements() {
		return flowElementIds;
	}

	public void addFlowElement(String id) {
		flowElementIds.add(id);
	}

	public Map<String, Object> getMetaData() {
		return this.metaData;
	}

	public void setMetaData(String name, Object data) {
		this.metaData.put(name, data);
	}

}
