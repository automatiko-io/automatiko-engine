package io.automatiko.engine.addons.usertasks.teams;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

@Path("/")
public interface TeamsIncomingWebHook {

    @POST
    @Consumes(value = MediaType.APPLICATION_JSON)
    void postMessage(String body);
}
