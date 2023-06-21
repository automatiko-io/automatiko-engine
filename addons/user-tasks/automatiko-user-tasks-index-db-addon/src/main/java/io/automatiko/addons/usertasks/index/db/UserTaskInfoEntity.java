package io.automatiko.addons.usertasks.index.db;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import io.automatiko.addon.usertasks.index.UserTask;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Table(name = "USER_TASK_INDEX")
@Entity
public class UserTaskInfoEntity extends PanacheEntityBase implements UserTask {

    @Id
    @Column(name = "TASK_ID")
    private String id;

    @Column(name = "TASK_NAME")
    private String taskName;
    @Column(name = "TASK_DESC")
    private String taskDescription;
    @Column(name = "TASK_PRIORITY")
    private String taskPriority;
    @Column(name = "TASK_REF_NAME")
    private String referenceName;
    @Column(name = "TASK_REF_ID")
    private String referenceId;
    @Column(name = "TASK_FORM_LINK")
    private String formLink;
    @Column(name = "TASK_START_DATE")
    private Date startDate;
    @Column(name = "TASK_COMPLETE_DATE")
    private Date completeDate;
    @Column(name = "TASK_STATE")
    private String state;
    @Column(name = "TASK_OWNER")
    private String actualOwner;
    @Column(name = "TASK_POT_OWNERS")
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> potentialUsers;
    @Column(name = "TASK_GROUPS")
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> potentialGroups;
    @Column(name = "TASK_EXCLUDED_USERS")
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> excludedUsers;
    @Column(name = "TASK_P_INSTANCE_ID")
    private String processInstanceId;
    @Column(name = "TASK_ROOT_P_INSTANCE_ID")
    private String rootProcessInstanceId;
    @Column(name = "TASK_PROCESS_ID")
    private String processId;
    @Column(name = "TASK_ROOT_PROCESS_ID")
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
        return Collections.emptyMap();
    }

    @Override
    public void setInputs(Map<String, Object> inputs) {
    }

    @Override
    public Map<String, Object> getOutputs() {
        return Collections.emptyMap();
    }

    @Override
    public void setOutputs(Map<String, Object> outputs) {
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
        UserTask other = (UserTask) obj;
        if (id == null) {
            if (other.getId() != null)
                return false;
        } else if (!id.equals(other.getId()))
            return false;
        return true;
    }
}
