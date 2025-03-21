package io.automatiko.addons.usertasks.index.mongo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import io.automatiko.addon.usertasks.index.UserTask;
import io.automatiko.addon.usertasks.index.UserTaskIndexResource;
import io.automatiko.addon.usertasks.index.UserTaskInfo;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.quarkus.arc.All;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
public class MongoDBUserTaskIndexResource implements UserTaskIndexResource {

    public static final String DATABASE_KEY = "quarkus.automatiko.persistence.mongodb.database";

    private MongoClient mongoClient;

    private IdentitySupplier identitySupplier;

    private Map<String, MongoDBCustomQueryBuilder> customQueries = new HashMap<>();

    private Optional<String> database;

    @Inject
    public MongoDBUserTaskIndexResource(MongoClient mongoClient, IdentitySupplier identitySupplier,
            @ConfigProperty(name = DATABASE_KEY) Optional<String> database,
            @All List<MongoDBCustomQueryBuilder> queries) {
        this.identitySupplier = identitySupplier;
        this.mongoClient = mongoClient;
        this.database = database;

        queries.stream().forEach(q -> customQueries.put(q.id(), q));
    }

    @Override
    public Collection<? extends UserTask> findTasks(String name, String description, String state,
            String priority, int page, int size, String sortBy, boolean sortAsc, String user, List<String> groups) {
        Collection<UserTaskInfo> result = new ArrayList<>();
        IdentityProvider identityProvider = identitySupplier.buildIdentityProvider(user, groups);
        if (identityProvider.getName() == null) {
            return Collections.emptyList();
        }
        try {
            MongoCollection<UserTaskInfo> usertasks = collection();
            Bson filter;

            List<Bson> filters = new ArrayList<>();

            if (name != null) {
                filters.add(Filters.regex(".*taskName.*", name));
            }
            if (description != null) {
                filters.add(Filters.regex(".*taskDescription.*", description));
            }
            if (state != null) {
                filters.add(Filters.eq("state", state));
            }
            if (priority != null) {
                filters.add(Filters.eq("priority", priority));
            }

            filter = filters(identityProvider, filters.toArray(new Bson[filters.size()]));

            Bson sort = null;
            if (sortBy != null) {
                sort = sortAsc ? Sorts.ascending(sortBy)
                        : Sorts.descending(sortBy);
            }

            usertasks.find(filter).sort(sort).skip(calculatePage(page, size)).limit(size).forEach(result::add);
            return result;
        } finally {
            IdentityProvider.set(null);
        }
    }

    @Override
    public UserTask findTask(String id, String user, List<String> groups) {
        IdentityProvider identityProvider = identitySupplier.buildIdentityProvider(user, groups);
        try {
            MongoCollection<UserTaskInfo> usertasks = collection();
            Bson filter = filters(identityProvider, Filters.eq("_id", id));

            return usertasks.find(filter).first();
        } finally {
            IdentityProvider.set(null);
        }
    }

    @Override
    public Collection<? extends UserTask> queryTasks(UriInfo uriInfo, String name, int page, int size, String sortBy,
            boolean sortAsc, String user, List<String> groups) {

        MongoDBCustomQueryBuilder customQuery = customQueries.get(name);

        if (customQuery == null) {
            throw new NotFoundException("Query with id '" + name + "' was not registered");
        }
        Collection<UserTaskInfo> result = new ArrayList<>();
        IdentityProvider identityProvider = identitySupplier.buildIdentityProvider(user, groups);
        try {
            MongoCollection<UserTaskInfo> usertasks = collection();
            Bson filter = filters(identityProvider, customQuery.build(uriInfo.getQueryParameters()));

            Bson sort = null;
            if (sortBy != null) {
                sort = sortAsc ? Sorts.ascending(sortBy)
                        : Sorts.descending(sortBy);
            }

            usertasks.find(filter).sort(sort).skip(calculatePage(page, size)).limit(size).forEach(result::add);
            return result;
        } finally {
            IdentityProvider.set(null);
        }
    }

    protected MongoCollection<UserTaskInfo> collection() {

        ClassModelBuilder<UserTaskInfo> modelBuilder = ClassModel.builder(UserTaskInfo.class).idPropertyName("id");

        return mongoClient.getDatabase(this.database.orElse("automatiko"))
                .withCodecRegistry(CodecRegistries.fromProviders(MongoClientSettings.getDefaultCodecRegistry(),
                        PojoCodecProvider.builder().register(modelBuilder.build()).build()))
                .getCollection("usertasks",
                        UserTaskInfo.class);

    }

    protected Bson filters(IdentityProvider identityProvider, Bson... extraFilters) {

        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.or(
                Filters.eq("actualOwner", identityProvider.getName()),
                Filters.or(
                        Filters.in("potentialUsers", identityProvider.getName()),
                        Filters.in("potentialGroups", identityProvider.getRoles())),
                Filters.and(
                        Filters.size("potentialUsers", 0),
                        Filters.size("potentialGroups", 0))));

        filters.add(Filters.not(Filters.in("excludedUsers", identityProvider.getName())));

        for (Bson extraFilter : extraFilters) {
            filters.add(extraFilter);
        }

        return Filters.and(filters);
    }

    protected int calculatePage(int page, int size) {
        if (page <= 1) {
            return 0;
        }

        return (page - 1) * size;
    }

}
