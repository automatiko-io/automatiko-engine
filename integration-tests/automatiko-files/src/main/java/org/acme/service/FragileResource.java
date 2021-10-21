package org.acme.service;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

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
