package io.automatiko.engine.addons.usertasks.teams;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public interface TeamsIncomingWebHook {

    @POST
    @Consumes(value = MediaType.APPLICATION_JSON)
    void postMessage(String body);
}
