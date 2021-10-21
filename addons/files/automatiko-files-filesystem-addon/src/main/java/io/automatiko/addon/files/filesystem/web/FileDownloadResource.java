package io.automatiko.addon.files.filesystem.web;

import java.io.FileNotFoundException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.automatiko.addon.files.filesystem.FileStore;
import io.automatiko.engine.api.workflow.files.File;

@Path("/management/files")
public class FileDownloadResource {

    private FileStore store;

    @Inject
    public FileDownloadResource(FileStore store) {
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
