
/**
 *
 */
package io.automatiko.engine.workflow.compiler.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.workflow.process.core.Node;

public class ProcessBuildData {

	private static final Logger logger = LoggerFactory.getLogger(ProcessBuildData.class);

	private List<Process> processes = new ArrayList<Process>();
	private Map<Long, Node> nodes = new HashMap<Long, Node>();
	private Map<String, Object> metaData = new HashMap<String, Object>();

	public ProcessBuildData() {
	}

	public List<Process> getProcesses() {
		return processes;
	}

	public void addProcess(Process process) {
		this.processes.add(process);
	}

	public void setProcesses(List<Process> process) {
		this.processes = process;
	}

	public Map<Long, Node> getNodes() {
		return nodes;
	}

	public boolean addNode(Node node) {
		return (this.nodes.put(node.getId(), node) != null);
	}

	public Node getNode(Long id) {
		return this.nodes.get(id);
	}

	public Object getMetaData(String name) {
		return metaData.get(name);
	}

	public void setMetaData(String name, Object data) {
		this.metaData.put(name, data);
	}

}
