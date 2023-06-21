package org.acme;

import java.util.List;
import java.util.Map;

import io.automatiko.addon.usertasks.index.UserTask;
import io.automatiko.addons.usertasks.index.db.UserTaskInfoEntity;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/test")
public class TestResource {

    @GET
    @Produces("application/json")
    public List<? extends UserTask> tasks() {
        String query = "from UserTaskInfoEntity t left join t.potentialUsers pu left join t.potentialGroups pg where (:user not member of t.excludedUsers) and (:user member of t.potentialUsers or pg in (:groups) or t.actualOwner = :user or (size(pg) < 1 and size(pu) < 1))";
        Map<String, Object> parameters = Map.of("user", "mike", "groups", List.of("test"));

        return UserTaskInfoEntity.find(query, parameters).list();
    }
}
