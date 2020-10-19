
package io.automatik.engine.workflow.base.instance.impl.humantask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.auth.IdentityProvider;
import io.automatik.engine.api.auth.SecurityPolicy;
import io.automatik.engine.api.runtime.process.HumanTaskWorkItem;
import io.automatik.engine.api.workflow.workitem.NotAuthorizedException;
import io.automatik.engine.api.workflow.workitem.Policy;
import io.automatik.engine.workflow.base.instance.impl.workitem.WorkItemImpl;

public class HumanTaskWorkItemImpl extends WorkItemImpl implements HumanTaskWorkItem {

    private static final long serialVersionUID = 6168927742199190604L;
    private static final Logger logger = LoggerFactory.getLogger(HumanTaskWorkItemImpl.class);

    private String taskName;
    private String taskDescription;
    private String taskPriority;
    private String referenceName;

    private String actualOwner;
    private Set<String> potentialUsers = new HashSet<>();
    private Set<String> potentialGroups = new HashSet<>();
    private Set<String> excludedUsers = new HashSet<>();
    private Set<String> adminUsers = new HashSet<>();
    private Set<String> adminGroups = new HashSet<>();

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

    @Override
    public boolean enforce(Policy<?>... policies) {
        boolean authorized = true;
        for (Policy<?> policy : policies) {
            if (policy instanceof SecurityPolicy) {
                try {
                    enforceAuthorization(((SecurityPolicy) policy).value());

                    return true;
                } catch (NotAuthorizedException e) {
                    authorized = false;
                }
            }
        }

        // there might have not been any policies given so let's ensure task is
        // protected if any assignments is set
        String currentOwner = getActualOwner();
        if ((currentOwner != null && !currentOwner.trim().isEmpty()) || !getPotentialUsers().isEmpty()) {
            authorized = false;
        }

        return authorized;
    }

    protected void enforceAuthorization(IdentityProvider identity) {

        if (identity != null) {
            logger.debug("Identity information provided, enforcing security restrictions, user '{}' with roles '{}'",
                    identity.getName(), identity.getRoles());
            // in case identity/auth info is given enforce security restrictions
            String user = identity.getName();
            String currentOwner = getActualOwner();
            // if actual owner is already set always enforce same user
            if (currentOwner != null && !currentOwner.trim().isEmpty() && !user.equals(currentOwner)) {
                logger.debug(
                        "Work item {} has already owner assigned so requesting user must match - owner '{}' == requestor '{}'",
                        getId(), currentOwner, user);
                throw new NotAuthorizedException(
                        "User " + user + " is not authorized to access task instance with id " + getId());
            }

            checkAssignedOwners(user, identity.getRoles());
        }
    }

    protected void checkAssignedOwners(String user, List<String> roles) {
        // is not in the excluded users
        if (getExcludedUsers().contains(user)) {
            logger.debug("Requesting user '{}' is excluded from the potential workers on work item {}", user, getId());
            throw new NotAuthorizedException(
                    "User " + user + " is not authorized to access task instance with id " + getId());
        }

        // if there are no assignments means open to everyone
        if (getPotentialUsers().isEmpty() && getPotentialGroups().isEmpty()) {
            return;
        }
        // check if user is in potential users or groups
        if (!getPotentialUsers().contains(user) && getPotentialGroups().stream().noneMatch(roles::contains)) {
            throw new NotAuthorizedException(
                    "User " + user + " is not authorized to access task instance with id " + getId());
        }
    }
}
