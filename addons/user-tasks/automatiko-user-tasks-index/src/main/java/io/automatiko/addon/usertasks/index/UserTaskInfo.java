package io.automatiko.addon.usertasks.index;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class UserTaskInfo implements UserTask {

    private String id;
    private String taskName;
    private String taskDescription;
    private String taskPriority;
    private String referenceName;
    private String referenceId;
    private String formLink;
    private Date startDate;
    private Date completeDate;

    private String state;

    private String actualOwner;
    private Set<String> potentialUsers;
    private Set<String> potentialGroups;
    private Set<String> excludedUsers;

    private Map<String, Object> inputs;
    private Map<String, Object> outputs;

    private String processInstanceId;
    private String rootProcessInstanceId;
    private String processId;
    private String rootProcessId;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    @Override
    public String getTaskDescription() {
        return taskDescription;
    }

    @Override
    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    @Override
    public String getTaskPriority() {
        return taskPriority;
    }

    @Override
    public void setTaskPriority(String taskPriority) {
        this.taskPriority = taskPriority;
    }

    @Override
    public String getReferenceName() {
        return referenceName;
    }

    @Override
    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    @Override
    public String getReferenceId() {
        return referenceId;
    }

    @Override
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public String getFormLink() {
        return formLink;
    }

    @Override
    public void setFormLink(String formLink) {
        this.formLink = formLink;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public Date getCompleteDate() {
        return completeDate;
    }

    @Override
    public void setCompleteDate(Date completeDate) {
        this.completeDate = completeDate;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String getActualOwner() {
        return actualOwner;
    }

    @Override
    public void setActualOwner(String actualOwner) {
        this.actualOwner = actualOwner;
    }

    @Override
    public Set<String> getPotentialUsers() {
        return potentialUsers;
    }

    @Override
    public void setPotentialUsers(Set<String> potentialUsers) {
        this.potentialUsers = potentialUsers;
    }

    @Override
    public Set<String> getPotentialGroups() {
        return potentialGroups;
    }

    @Override
    public void setPotentialGroups(Set<String> potentialGroups) {
        this.potentialGroups = potentialGroups;
    }

    @Override
    public Set<String> getExcludedUsers() {
        return excludedUsers;
    }

    @Override
    public void setExcludedUsers(Set<String> excludedUsers) {
        this.excludedUsers = excludedUsers;
    }

    @Override
    public Map<String, Object> getInputs() {
        return inputs;
    }

    @Override
    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;

        if (this.inputs != null) {
            // remove internal inputs
            this.inputs.remove("ActorId");
            this.inputs.remove("Groups");
            this.inputs.remove("GroupId");
            this.inputs.remove("ExcludedUsers");
            this.inputs.remove("Locale");
            this.inputs.remove("NodeName");
            this.inputs.remove("Description");
            this.inputs.remove("Priority");
            this.inputs.remove("Skippable");
            this.inputs.remove("TaskName");
            this.inputs.remove("SwimlaneActorId");
        }
    }

    @Override
    public Map<String, Object> getOutputs() {
        return outputs;
    }

    @Override
    public void setOutputs(Map<String, Object> outputs) {
        this.outputs = outputs;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String getRootProcessInstanceId() {
        return rootProcessInstanceId;
    }

    @Override
    public void setRootProcessInstanceId(String rootProcessInstanceId) {
        this.rootProcessInstanceId = rootProcessInstanceId;
    }

    @Override
    public String getProcessId() {
        return processId;
    }

    @Override
    public void setProcessId(String processId) {
        this.processId = processId;
    }

    @Override
    public String getRootProcessId() {
        return rootProcessId;
    }

    @Override
    public void setRootProcessId(String rootProcessId) {
        this.rootProcessId = rootProcessId;
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
        UserTaskInfo other = (UserTaskInfo) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}
