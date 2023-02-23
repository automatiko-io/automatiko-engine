package io.sw;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.WILDCARD)
public class TestResource {

    @POST
    public Response handle(String payload) {

        System.out.println("Received payload\n");
        System.out.println(payload);
        return Response.ok().build();
    }
}
