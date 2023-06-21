package io.automatiko.addons.usertasks.index.fs;

import static com.googlecode.cqengine.query.QueryFactory.and;
import static com.googlecode.cqengine.query.QueryFactory.ascending;
import static com.googlecode.cqengine.query.QueryFactory.contains;
import static com.googlecode.cqengine.query.QueryFactory.descending;
import static com.googlecode.cqengine.query.QueryFactory.equal;
import static com.googlecode.cqengine.query.QueryFactory.has;
import static com.googlecode.cqengine.query.QueryFactory.in;
import static com.googlecode.cqengine.query.QueryFactory.not;
import static com.googlecode.cqengine.query.QueryFactory.or;
import static com.googlecode.cqengine.query.QueryFactory.orderBy;
import static com.googlecode.cqengine.query.QueryFactory.queryOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.logical.And;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.googlecode.cqengine.resultset.ResultSet;
import com.googlecode.cqengine.resultset.common.NoSuchObjectException;

import io.automatiko.addon.usertasks.index.UserTask;
import io.automatiko.addon.usertasks.index.UserTaskIndexResource;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.quarkus.arc.All;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.UriInfo;

public class CQEngineUserTaskIndexResource implements UserTaskIndexResource {

    private CQEngineBasedIndex index;

    private IdentitySupplier identitySupplier;

    private Map<String, CQEngineCustomQueryBuilder> customQueries = new HashMap<>();

    @Inject
    public CQEngineUserTaskIndexResource(CQEngineBasedIndex index, IdentitySupplier identitySupplier,
            @All List<CQEngineCustomQueryBuilder> queries) {
        this.identitySupplier = identitySupplier;
        this.index = index;

        queries.stream().forEach(q -> customQueries.put(q.id(), q));
    }

    @Override
    public Collection<? extends UserTask> findTasks(String name, String description, String state,
            String priority, int page, int size, String sortBy,
            boolean sortAsc, String user, List<String> groups) {
        IdentityProvider identityProvider = identitySupplier.buildIdentityProvider(user, groups);
        List<Query<CQEngineUserTaskInfo>> conditions = new ArrayList<>();
        // @formatter:off
        conditions.add(
                        and(
                          or(
                            in(CQEngineUserTaskInfo.POT_OWNERS, identityProvider.getName()),
                            in(CQEngineUserTaskInfo.POT_GROUPS, false, identityProvider.getRoles()),
                            and(not(has(CQEngineUserTaskInfo.POT_OWNERS)), not(has(CQEngineUserTaskInfo.POT_OWNERS)))
                            ),
                          not(in(CQEngineUserTaskInfo.EXCLUDED_USERS, identityProvider.getName()))
                         )        
                      );
        
        
        // @formatter:on
        if (name != null) {
            conditions.add(contains(CQEngineUserTaskInfo.TASK_NAME, name));
        }
        if (description != null) {
            conditions.add(contains(CQEngineUserTaskInfo.TASK_DESCRIPTION, description));
        }

        if (state != null) {
            conditions.add(equal(CQEngineUserTaskInfo.TASK_STATE, state));
        }

        if (priority != null) {
            conditions.add(equal(CQEngineUserTaskInfo.TASK_PRIORITY, priority));
        }

        QueryOptions queryOptions = null;
        if (sortBy != null) {
            queryOptions = queryOptions(
                    orderBy(sortAsc ? ascending(sortAttribute(sortBy)) : descending(sortAttribute(sortBy))));
        }

        Query<CQEngineUserTaskInfo> query = conditions.size() > 1 ? new And<>(conditions) : conditions.get(0);
        try (ResultSet<CQEngineUserTaskInfo> resultSet = index.get().retrieve(query, queryOptions)) {
            return resultSet.stream().skip(calculatePage(page, size)).limit(size).toList();
        } finally {
            IdentityProvider.set(null);
        }
    }

    @Override
    public UserTask findTask(String id, String user, List<String> groups) {
        IdentityProvider identityProvider = identitySupplier.buildIdentityProvider(user, groups);

        Query<CQEngineUserTaskInfo> query = and(equal(CQEngineUserTaskInfo.TASK_ID, id), or(
                in(CQEngineUserTaskInfo.POT_OWNERS, identityProvider.getName()),
                in(CQEngineUserTaskInfo.POT_GROUPS, false, identityProvider.getRoles()),
                and(not(has(CQEngineUserTaskInfo.POT_OWNERS)), not(has(CQEngineUserTaskInfo.POT_OWNERS)))));
        try (ResultSet<CQEngineUserTaskInfo> resultSet = index.get().retrieve(query)) {
            return resultSet.uniqueResult();
        } catch (NoSuchObjectException e) {
            throw new WorkItemNotFoundException("User task with given id was not found", id);
        } finally {
            IdentityProvider.set(null);
        }
    }

    @Override
    public Collection<? extends UserTask> queryTasks(UriInfo uriInfo, String name, int page, int size, String sortBy,
            boolean sortAsc, String user, List<String> groups) {

        CQEngineCustomQueryBuilder customQuery = customQueries.get(name);

        if (customQuery == null) {
            throw new NotFoundException("Query with id '" + name + "' was not registered");
        }

        IdentityProvider identityProvider = identitySupplier.buildIdentityProvider(user, groups);
        List<Query<CQEngineUserTaskInfo>> conditions = new ArrayList<>();
        // @formatter:off
        conditions.add(
                        and(
                          or(
                            in(CQEngineUserTaskInfo.POT_OWNERS, identityProvider.getName()),
                            in(CQEngineUserTaskInfo.POT_GROUPS, false, identityProvider.getRoles()),
                            and(not(has(CQEngineUserTaskInfo.POT_OWNERS)), not(has(CQEngineUserTaskInfo.POT_OWNERS)))
                            ),
                          not(in(CQEngineUserTaskInfo.EXCLUDED_USERS, identityProvider.getName()))
                         )        
                      );
        // @formatter:on

        Query<CQEngineUserTaskInfo> filter = customQuery.build(uriInfo.getQueryParameters());
        conditions.add(filter);

        QueryOptions queryOptions = null;
        if (sortBy != null) {
            queryOptions = queryOptions(
                    orderBy(sortAsc ? ascending(sortAttribute(sortBy)) : descending(sortAttribute(sortBy))));
        }

        Query<CQEngineUserTaskInfo> query = conditions.size() > 1 ? new And<>(conditions) : conditions.get(0);
        try (ResultSet<CQEngineUserTaskInfo> resultSet = index.get().retrieve(query, queryOptions)) {
            return resultSet.stream().skip(calculatePage(page, size)).limit(size).toList();
        } finally {
            IdentityProvider.set(null);
        }
    }

    protected int calculatePage(int page, int size) {
        if (page <= 1) {
            return 0;
        }

        return (page - 1) * size;
    }

    protected Attribute<CQEngineUserTaskInfo, ? extends Comparable> sortAttribute(String name) {
        Attribute<CQEngineUserTaskInfo, ? extends Comparable> sortAttribute = null;

        switch (name.toLowerCase()) {
            case "taskName":
            case "name":
                sortAttribute = CQEngineUserTaskInfo.TASK_NAME;
                break;
            case "id":
                sortAttribute = CQEngineUserTaskInfo.TASK_ID;
                break;
            case "state":
                sortAttribute = CQEngineUserTaskInfo.TASK_STATE;
                break;
            case "priority":
                sortAttribute = CQEngineUserTaskInfo.TASK_PRIORITY;
                break;
            case "startDate":
                sortAttribute = CQEngineUserTaskInfo.TASK_START_DATE;
                break;
            default:
                sortAttribute = CQEngineUserTaskInfo.TASK_START_DATE;
                break;
        }

        return sortAttribute;
    }
}
