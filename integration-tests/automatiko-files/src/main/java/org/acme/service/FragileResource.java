package org.acme.service;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("/test")
public class FragileResource {

    @Inject
    FragileService service;

    @POST
    @Path("/{errorCode}")
    @Produces(value = "application/json")
    public Integer toogleFailure(@PathParam("errorCode") String errorCode) {
        service.toogle(errorCode);

        return service.getCounter();
    }
}
