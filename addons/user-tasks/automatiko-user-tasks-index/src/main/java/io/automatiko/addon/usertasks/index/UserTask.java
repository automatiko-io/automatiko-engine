package io.automatiko.addon.usertasks.index;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public interface UserTask {

    String getId();

    void setId(String id);

    String getTaskName();

    void setTaskName(String taskName);

    String getTaskDescription();

    void setTaskDescription(String taskDescription);

    String getTaskPriority();

    void setTaskPriority(String taskPriority);

    String getReferenceName();

    void setReferenceName(String referenceName);

    String getReferenceId();

    void setReferenceId(String referenceId);

    String getFormLink();

    void setFormLink(String formLink);

    Date getStartDate();

    void setStartDate(Date startDate);

    Date getCompleteDate();

    void setCompleteDate(Date completeDate);

    String getState();

    void setState(String state);

    String getActualOwner();

    void setActualOwner(String actualOwner);

    Set<String> getPotentialUsers();

    void setPotentialUsers(Set<String> potentialUsers);

    Set<String> getPotentialGroups();

    void setPotentialGroups(Set<String> potentialGroups);

    Set<String> getExcludedUsers();

    void setExcludedUsers(Set<String> excludedUsers);

    Map<String, Object> getInputs();

    void setInputs(Map<String, Object> inputs);

    Map<String, Object> getOutputs();

    void setOutputs(Map<String, Object> outputs);

    String getProcessInstanceId();

    void setProcessInstanceId(String processInstanceId);

    String getRootProcessInstanceId();

    void setRootProcessInstanceId(String rootProcessInstanceId);

    String getProcessId();

    void setProcessId(String processId);

    String getRootProcessId();

    void setRootProcessId(String rootProcessId);

}