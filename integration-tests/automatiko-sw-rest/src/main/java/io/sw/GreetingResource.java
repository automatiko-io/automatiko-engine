package io.sw;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Path("/greeting")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GreetingResource {

    @POST
    public Response handle(ObjectNode payload) {

        System.out.println("About to send greetings to " + payload.get("name").asText());
        Map<String, Object> data = new HashMap<>();
        data.put("greeting", "Hello " + payload.get("name").asText());
        return Response.ok(data).build();
    }
}
