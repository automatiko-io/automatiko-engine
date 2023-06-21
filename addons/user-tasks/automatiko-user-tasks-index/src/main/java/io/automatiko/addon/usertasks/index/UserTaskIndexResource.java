package io.automatiko.addon.usertasks.index;

import java.util.Collection;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

@Tag(name = "User task search", description = "Allows to search for user tasks across all processes based on user/group assignment")
@Path("/index/usertasks")
public interface UserTaskIndexResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Collection<? extends UserTask> findTasks(
            @Parameter(description = "Name of the task to search for (supports partial patch)", required = false) @QueryParam("name") String name,
            @Parameter(description = "Description of the task to search for (supports partial match)", required = false) @QueryParam("description") String description,
            @Parameter(description = "State of the task (exact match)", required = false) @QueryParam("state") String state,
            @Parameter(description = "Priority of the task (exact match)", required = false) @QueryParam("priority") String priority,
            @Parameter(description = "Pagination - page to start on", required = false) @QueryParam(value = "page") @DefaultValue("1") int page,
            @Parameter(description = "Pagination - number of items to return", required = false) @QueryParam(value = "size") @DefaultValue("10") int size,
            @Parameter(description = "Sorting - name of the field to sort by (description, startDate, completeDate, businessKey)", required = false) @QueryParam(value = "sortBy") String sortBy,
            @Parameter(description = "Sorting - direction of sorting ascending or descending", required = false) @QueryParam(value = "sortAsc") @DefaultValue("true") boolean sortAsc,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") List<String> groups);

    @Path("/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    UserTask findTask(
            @Parameter(description = "Unique identifier to get the task", required = true) @PathParam("id") String id,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") List<String> groups);

    @Path("/queries/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Collection<? extends UserTask> queryTasks(@Context UriInfo uriInfo,
            @Parameter(description = "Unique name of the query to be used to produce filter", required = true) @PathParam("name") String name,
            @Parameter(description = "Pagination - page to start on", required = false) @QueryParam(value = "page") @DefaultValue("1") int page,
            @Parameter(description = "Pagination - number of items to return", required = false) @QueryParam(value = "size") @DefaultValue("10") int size,
            @Parameter(description = "Sorting - name of the field to sort by (description, startDate, completeDate, businessKey)", required = false) @QueryParam(value = "sortBy") String sortBy,
            @Parameter(description = "Sorting - direction of sorting ascending or descending", required = false) @QueryParam(value = "sortAsc") @DefaultValue("true") boolean sortAsc,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") List<String> groups);
}
