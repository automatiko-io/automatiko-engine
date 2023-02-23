package io.automatiko.addon.files.mongodb.web;

import java.io.FileNotFoundException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import io.automatiko.addon.files.mongodb.GridFSStore;
import io.automatiko.engine.api.workflow.files.File;

@Path("/management/files")
public class GridFSDownloadResource {

    private GridFSStore store;

    @Inject
    public GridFSDownloadResource(GridFSStore store) {
        this.store = store;
    }

    @GET
    @Path("download/{processId}/{processInstanceId}/{variable}/{filename}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@PathParam("processId") String processId,
            @PathParam("processInstanceId") String processInstanceId,
            @PathParam("variable") String variable,
            @PathParam("filename") String filename) {
        try {
            byte[] content = store.content(processId, processInstanceId, variable, filename);

            ResponseBuilder response = Response.ok().entity(content);
            response.header("Content-Disposition", "attachment;filename=" + filename);
            response.header("Content-Type", File.discoverType(filename));
            return response.build();
        } catch (FileNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), "File with name " + filename + " not found")
                    .build();
        } catch (Exception e) {

            return Response.serverError().build();
        }
    }

    @GET
    @Path("download/{processId}/{processVersion}/{processInstanceId}/{variable}/{filename}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadVersionedFile(@PathParam("processId") String processId,
            @PathParam("processVersion") String processVersion,
            @PathParam("processInstanceId") String processInstanceId,
            @PathParam("variable") String variable,
            @PathParam("filename") String filename) {
        try {
            byte[] content = store.content(processId, processVersion, processInstanceId, variable, filename);

            ResponseBuilder response = Response.ok().entity(content);
            response.header("Content-Disposition", "attachment;filename=" + filename);
            response.header("Content-Type", File.discoverType(filename));
            return response.build();
        } catch (FileNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), "File with name " + filename + " not found")
                    .build();
        } catch (Exception e) {

            return Response.serverError().build();
        }
    }

}
