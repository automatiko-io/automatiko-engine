package io.automatiko.addons.usertasks.index.db;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.automatiko.addon.usertasks.index.UserTask;
import io.automatiko.addon.usertasks.index.UserTaskIndexResource;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.api.workflow.workitem.NotAuthorizedException;
import io.quarkus.arc.All;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
public class DBUserTaskIndexResource implements UserTaskIndexResource {

    private IdentitySupplier identitySupplier;

    private Map<String, DbCustomQueryBuilder> customQueries = new HashMap<>();

    @Inject
    public DBUserTaskIndexResource(IdentitySupplier identitySupplier,
            @All List<DbCustomQueryBuilder> queries) {
        this.identitySupplier = identitySupplier;

        queries.stream().forEach(q -> customQueries.put(q.id(), q));
    }

    @Override
    public Collection<? extends UserTask> findTasks(String name, String description, String state,
            String priority, int page, int size, String sortBy, boolean sortAsc, String user, List<String> groups) {

        IdentityProvider identityProvider = identitySupplier.buildIdentityProvider(user, groups);
        if (identityProvider.getName() == null) {
            return Collections.emptyList();
        }
        try {
            Sort sort = null;
            if (sortBy != null) {
                sort = Sort.by(sortBy(sortBy), sortAsc ? Sort.Direction.Ascending : Sort.Direction.Descending);
            }
            Map<String, Object> parameters = new HashMap<>();
            StringBuilder builder = new StringBuilder(authFilter(identityProvider, parameters));

            if (name != null) {
                builder.append(" t.taskName like :taskName and");
                parameters.put("taskName", "%" + name + " %");
            }
            if (description != null) {
                builder.append(" t.taskDescription like :taskDescription and");
                parameters.put("taskDescription", "%" + description + " %");
            }
            if (state != null) {
                builder.append(" t.state = :state and");
                parameters.put("state", state);
            }
            if (priority != null) {
                builder.append(" t.taskPriority like :taskPriority and");
                parameters.put("taskPriority", priority);
            }

            String query = builder.toString();
            // remove the last and
            query = query.substring(0, query.length() - 4);

            return UserTaskInfoEntity.find(query, sort, parameters).page(calculatePage(page, size), size).list();
        } finally {
            IdentityProvider.set(null);
        }
    }

    @Override
    public UserTask findTask(String id, String user, List<String> groups) {
        IdentityProvider identityProvider = identitySupplier.buildIdentityProvider(user, groups);
        try {

            UserTaskInfoEntity entity = UserTaskInfoEntity.findById(id);

            try {
                enforceAuthorization(entity, identityProvider);
                return entity;
            } catch (NotAuthorizedException e) {
                return null;
            }

        } finally {
            IdentityProvider.set(null);
        }
    }

    @Override
    public Collection<? extends UserTask> queryTasks(UriInfo uriInfo, String name, int page, int size, String sortBy,
            boolean sortAsc, String user, List<String> groups) {

        DbCustomQueryBuilder customQuery = customQueries.get(name);

        if (customQuery == null) {
            throw new NotFoundException("Query with id '" + name + "' was not registered");
        }

        IdentityProvider identityProvider = identitySupplier.buildIdentityProvider(user, groups);
        try {
            Sort sort = null;
            if (sortBy != null) {
                sort = Sort.by(sortBy(sortBy), sortAsc ? Sort.Direction.Ascending : Sort.Direction.Descending);
            }
            Map<String, Object> parameters = new HashMap<>();
            StringBuilder builder = new StringBuilder(authFilter(identityProvider, parameters));

            DbQueryFilter extraFilter = customQuery.build(uriInfo.getQueryParameters());

            String query = builder.append(" " + extraFilter.queryFilter()).toString();
            parameters.putAll(extraFilter.parameters());

            return UserTaskInfoEntity.find(query, sort, parameters).page(calculatePage(page, size), size).list();
        } finally {
            IdentityProvider.set(null);
        }
    }

    protected String authFilter(IdentityProvider identityProvider, Map<String, Object> parameters) {

        parameters.put("user", identityProvider.getName());
        parameters.put("groups", identityProvider.getRoles());

        String authFilter = "from UserTaskInfoEntity t left join t.potentialUsers pu left join t.potentialGroups pg where (:user not member of t.excludedUsers) and (:user member of t.potentialUsers or pg in (:groups) or t.actualOwner = :user or (size(pg) < 1 and size(pu) < 1)) and ";

        return authFilter;

    }

    protected void enforceAuthorization(UserTaskInfoEntity entity, IdentityProvider identity) {

        if (identity != null) {
            // in case identity/auth info is given enforce security restrictions
            String user = identity.getName();
            String currentOwner = entity.getActualOwner();
            // if actual owner is already set always enforce same user
            if (currentOwner != null && !currentOwner.trim().isEmpty() && !user.equals(currentOwner)) {

                throw new NotAuthorizedException(
                        "User " + user + " is not authorized to access task instance with id " + entity.getId());
            }

            checkAssignedOwners(entity, user, identity);
        }
    }

    protected void checkAssignedOwners(UserTaskInfoEntity entity, String user, IdentityProvider identity) {
        // is not in the excluded users
        if (entity.getExcludedUsers().contains(user)) {
            throw new NotAuthorizedException(
                    "User " + user + " is not authorized to access task instance with id " + entity.getId());
        }

        // if there are no assignments means open to everyone
        if (entity.getPotentialUsers().isEmpty() && entity.getPotentialGroups().isEmpty()) {
            return;
        }
        // check if user is in potential users or groups
        if (!entity.getPotentialUsers().contains(user) && entity.getPotentialGroups().stream().noneMatch(identity::hasRole)) {
            throw new NotAuthorizedException(
                    "User " + user + " is not authorized to access task instance with id " + entity.getId());
        }
    }

    protected int calculatePage(int page, int size) {
        if (page <= 1) {
            return 0;
        }

        return (page - 1) * size;
    }

    protected String sortBy(String sortBy) {

        String fieldName = sortBy;

        switch (sortBy) {
            case "name":
                fieldName = "taskName";
                break;
            case "description":
                fieldName = "taskDescription";
                break;
            case "priority":
                fieldName = "taskPriority";
                break;
            default:
                break;
        }

        return "t." + fieldName;
    }

}
