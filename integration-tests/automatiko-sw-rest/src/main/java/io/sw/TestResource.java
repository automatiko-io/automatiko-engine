package io.sw;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
