package io.automatiko.addons.graphql.ut;

import java.util.List;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.smallrye.graphql.api.Subscription;
import io.smallrye.mutiny.Multi;

@jakarta.enterprise.context.ApplicationScoped
@Description("Dedicated GraphQL API to user tasks for the complete service")
@GraphQLApi
public class UserTaskSubscriptionResource {

    GraphQLUserTaskSubscriptionEventPublisher subscriptionPublisher;

    IdentitySupplier identitySupplier;

    @jakarta.inject.Inject
    public void $Type$GraphQLResource(IdentitySupplier identitySupplier,
            GraphQLUserTaskSubscriptionEventPublisher subscriptionPublisher) {
        this.identitySupplier = identitySupplier;
        this.subscriptionPublisher = subscriptionPublisher;
    }

    @Subscription
    @Description("Emits on every change (create, complete, abort) of user task within the service")
    public Multi<UserTaskEventInput> userTasks(
            @Name("user") @DefaultValue("") final String user,
            @Name("groups") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return subscriptionPublisher.userTask().onSubscription().invoke(() -> IdentityProvider.set(null));
    }
}
