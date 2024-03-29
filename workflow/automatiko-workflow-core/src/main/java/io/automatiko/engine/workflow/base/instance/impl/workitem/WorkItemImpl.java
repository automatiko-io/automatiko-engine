package io.automatiko.engine.workflow.base.instance.impl.workitem;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.WorkItem;

public class WorkItemImpl implements WorkItem, Serializable {

    private static final long serialVersionUID = 510l;

    private String id;
    private String name;
    private int state = 0;
    private Map<String, Object> parameters = new HashMap<>();
    private Map<String, Object> results = new HashMap<>();
    private String processId;
    private String processInstanceId;
    private String parentProcessInstanceId;
    private String deploymentId;
    private String nodeInstanceId;
    private long nodeId;

    private String phaseId;
    private String phaseStatus;

    private Date startDate;
    private Date completeDate;

    private transient ProcessInstance processInstance;
    private transient NodeInstance nodeInstance;

    public void setId(String id) {
        if (this.id == null) {
            this.id = id;
        }
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void setParameter(String name, Object value) {
        this.parameters.put(name, value);
    }

    public Object getParameter(String name) {
        return parameters.get(name);
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setResults(Map<String, Object> results) {
        if (results != null) {
            this.results = results;
        }
    }

    public void setResult(String name, Object value) {
        results.put(name, value);
    }

    public Object getResult(String name) {
        return results.get(name);
    }

    public Map<String, Object> getResults() {
        return results;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getParentProcessInstanceId() {
        return parentProcessInstanceId;
    }

    public void setParentProcessInstanceId(String parentProcessInstanceId) {
        this.parentProcessInstanceId = parentProcessInstanceId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getNodeInstanceId() {
        return nodeInstanceId;
    }

    public void setNodeInstanceId(String nodeInstanceId) {
        this.nodeInstanceId = nodeInstanceId;
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public String getPhaseId() {
        return this.phaseId;
    }

    @Override
    public String getPhaseStatus() {
        return this.phaseStatus;
    }

    @Override
    public void setPhaseId(String phaseId) {
        this.phaseId = phaseId;
    }

    @Override
    public void setPhaseStatus(String phaseStatus) {
        this.phaseStatus = phaseStatus;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getCompleteDate() {
        return completeDate;
    }

    public void setCompleteDate(Date completeDate) {
        this.completeDate = completeDate;
    }

    @Override
    public NodeInstance getNodeInstance() {
        return this.nodeInstance;
    }

    @Override
    public ProcessInstance getProcessInstance() {
        return this.processInstance;
    }

    @Override
    public void setNodeInstance(NodeInstance nodeInstance) {
        this.nodeInstance = nodeInstance;
    }

    @Override
    public void setProcessInstance(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    @Override
    public String getProcessId() {
        return this.processId;
    }

    public String toString() {
        StringBuilder b = new StringBuilder("WorkItem ");
        b.append(id);
        b.append(" [name=");
        b.append(name);
        b.append(", state=");
        b.append(state);
        b.append(", processInstanceId=");
        b.append(processInstanceId);
        b.append(", parameters{");
        for (Iterator<Map.Entry<String, Object>> iterator = parameters.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Object> entry = iterator.next();
            b.append(entry.getKey());
            b.append("=");
            b.append(entry.getValue());
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        b.append("}]");
        return b.toString();
    }

}
