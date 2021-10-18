package io.automatiko.addons.graphql.ut;

import java.util.Date;
import java.util.Set;

import org.eclipse.microprofile.graphql.Input;

import io.automatiko.engine.services.event.impl.UserTaskInstanceEventBody;

@Input
public class UserTaskEventInput {

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
    private Set<String> adminUsers;
    private Set<String> adminGroups;

    private String processInstanceId;
    private String rootProcessInstanceId;
    private String processId;
    private String rootProcessId;

    public UserTaskEventInput(UserTaskInstanceEventBody event) {

        this.id = event.getId();
        this.taskName = event.getTaskName();
        this.taskDescription = event.getTaskDescription();
        this.taskPriority = event.getTaskPriority();
        this.referenceName = event.getReferenceName();
        this.referenceId = event.getReferenceId();
        this.formLink = event.getFormLink();
        this.startDate = event.getStartDate();
        this.completeDate = event.getCompleteDate();

        this.state = event.getState();

        this.actualOwner = event.getActualOwner();
        this.potentialUsers = event.getPotentialUsers();
        this.potentialGroups = event.getPotentialGroups();
        this.excludedUsers = event.getExcludedUsers();
        this.adminUsers = event.getAdminUsers();
        this.adminGroups = event.getAdminGroups();

        this.processInstanceId = event.getProcessInstanceId();
        this.rootProcessInstanceId = event.getRootProcessInstanceId();
        this.processId = event.getProcessId();
        this.rootProcessId = event.getRootProcessId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public String getTaskPriority() {
        return taskPriority;
    }

    public void setTaskPriority(String taskPriority) {
        this.taskPriority = taskPriority;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getFormLink() {
        return formLink;
    }

    public void setFormLink(String formLink) {
        this.formLink = formLink;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getActualOwner() {
        return actualOwner;
    }

    public void setActualOwner(String actualOwner) {
        this.actualOwner = actualOwner;
    }

    public Set<String> getPotentialUsers() {
        return potentialUsers;
    }

    public void setPotentialUsers(Set<String> potentialUsers) {
        this.potentialUsers = potentialUsers;
    }

    public Set<String> getPotentialGroups() {
        return potentialGroups;
    }

    public void setPotentialGroups(Set<String> potentialGroups) {
        this.potentialGroups = potentialGroups;
    }

    public Set<String> getExcludedUsers() {
        return excludedUsers;
    }

    public void setExcludedUsers(Set<String> excludedUsers) {
        this.excludedUsers = excludedUsers;
    }

    public Set<String> getAdminUsers() {
        return adminUsers;
    }

    public void setAdminUsers(Set<String> adminUsers) {
        this.adminUsers = adminUsers;
    }

    public Set<String> getAdminGroups() {
        return adminGroups;
    }

    public void setAdminGroups(Set<String> adminGroups) {
        this.adminGroups = adminGroups;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getRootProcessInstanceId() {
        return rootProcessInstanceId;
    }

    public void setRootProcessInstanceId(String rootProcessInstanceId) {
        this.rootProcessInstanceId = rootProcessInstanceId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getRootProcessId() {
        return rootProcessId;
    }

    public void setRootProcessId(String rootProcessId) {
        this.rootProcessId = rootProcessId;
    }

}
