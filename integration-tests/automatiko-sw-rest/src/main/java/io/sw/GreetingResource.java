package io.sw;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
