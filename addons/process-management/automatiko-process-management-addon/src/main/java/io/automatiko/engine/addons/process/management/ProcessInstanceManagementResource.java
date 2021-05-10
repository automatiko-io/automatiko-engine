
package io.automatiko.engine.addons.process.management;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.automatiko.engine.addons.process.management.export.ProcessInstanceExporter;
import io.automatiko.engine.addons.process.management.model.ErrorInfoDTO;
import io.automatiko.engine.addons.process.management.model.JsonExportedProcessInstance;
import io.automatiko.engine.addons.process.management.model.ProcessDTO;
import io.automatiko.engine.addons.process.management.model.ProcessInstanceDTO;
import io.automatiko.engine.addons.process.management.model.ProcessInstanceDetailsDTO;
import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessImageNotFoundException;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceNotFoundException;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.VariableNotFoundException;
import io.automatiko.engine.services.uow.UnitOfWorkExecutor;
import io.automatiko.engine.workflow.AbstractProcess;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;

@Tag(name = "Process Management", description = "Process management operations on top of the service", externalDocs = @ExternalDocumentation(description = "Manangement UI", url = "/management/processes/ui"))
@Path("/management/processes")
public class ProcessInstanceManagementResource extends BaseProcessInstanceManagementResource<Response> {

    private IdentitySupplier identitySupplier;

    // CDI
    public ProcessInstanceManagementResource() {
        this((Map<String, Process<?>>) null, null, null);
    }

    public ProcessInstanceManagementResource(Map<String, Process<?>> process, Application application,
            IdentitySupplier identitySupplier) {
        super(process, application);
        this.identitySupplier = identitySupplier;
    }

    @Inject
    public ProcessInstanceManagementResource(Application application, Instance<Process<?>> availableProcesses,
            IdentitySupplier identitySupplier) {
        super(availableProcesses == null ? Collections.emptyMap()
                : availableProcesses.stream().collect(Collectors.toMap(p -> p.id(), p -> p)), application);
        this.identitySupplier = identitySupplier;
    }

    @Override
    protected <R> Response buildOkResponse(R body) {
        return Response.status(Response.Status.OK).entity(body).build();
    }

    @Override
    protected Response badRequestResponse(String message) {
        return Response.status(Status.BAD_REQUEST).entity(message).build();
    }

    @Override
    protected Response notFoundResponse(String message) {
        return Response.status(Status.NOT_FOUND).entity(message).build();
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "200", content = @Content(mediaType = "text/html")) })
    @Operation(summary = "Entry point for the Process management UI", hidden = true)
    @GET
    @Path("/ui")
    @Produces(MediaType.TEXT_HTML)
    public Response ui() {
        ResponseBuilder builder = Response.ok().entity(getClass().getResourceAsStream("/automatiko-index.html"));
        return builder.header("Content-Type", "text/html").build();
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "List of available processes", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Lists available public processes in the service")
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProcessDTO> get(@Context UriInfo uriInfo,
            @Parameter(description = "Pagination - page to start on", required = false) @QueryParam(value = "page") @DefaultValue("1") int page,
            @Parameter(description = "Pagination - number of items to return", required = false) @QueryParam(value = "size") @DefaultValue("10") int size,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        List<ProcessDTO> collected = new ArrayList<ProcessDTO>();
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            for (String id : processData.keySet()) {
                Process<?> process = processData.get(id);

                if (!WorkflowProcess.PUBLIC_VISIBILITY
                        .equals(((WorkflowProcess) ((AbstractProcess<?>) process).process()).getVisibility())) {
                    continue;
                }

                String pathprefix = "";
                if (process.version() != null) {
                    pathprefix = "v" + process.version().replaceAll("\\.", "_") + "/";
                }

                collected.add(new ProcessDTO(id, process.version(), process.name(),
                        (String) ((AbstractProcess<?>) process).process().getMetaData().get("Documentation"),
                        uriInfo.getBaseUri().toString() + pathprefix + ((AbstractProcess<?>) process).process().getId()
                                + "/image",
                        process.instances().size()));
            }
            return collected;
        } finally {
            IdentityProvider.set(null);
        }
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "List of available process instances of the given process", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Lists available process instances of given process")
    @GET
    @Path("/{processId}/instances")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProcessInstanceDTO> getInstances(@Context UriInfo uriInfo,
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Pagination - page to start on", required = false) @QueryParam(value = "page") @DefaultValue("1") int page,
            @Parameter(description = "Pagination - number of items to return", required = false) @QueryParam(value = "size") @DefaultValue("10") int size,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        List<ProcessInstanceDTO> collected = new ArrayList<ProcessInstanceDTO>();
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            Process<?> process = processData.get(processId);

            process.instances().values(page, size).forEach(pi -> collected
                    .add(new ProcessInstanceDTO(pi.id(), pi.businessKey(), pi.description(), pi.tags().values(),
                            pi.errors().isPresent(), processId)));

            return collected;
        } finally {
            IdentityProvider.set(null);
        }
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "Process instance details", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Returns process instance details for given instance id")
    @SuppressWarnings("unchecked")
    @GET
    @Path("/{processId}/instances/{instanceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProcessInstanceDetailsDTO getInstance(@Context UriInfo uriInfo,
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("instanceId") String instanceId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            Process<?> process = processData.get(processId);

            Optional<ProcessInstance<?>> instance = (Optional<ProcessInstance<?>>) process.instances().findById(instanceId,
                    ProcessInstanceReadMode.READ_ONLY);

            if (instance.isEmpty()) {
                throw new ProcessInstanceNotFoundException(instanceId);
            }

            ProcessInstance<?> pi = instance.get();
            ProcessInstanceDetailsDTO details = new ProcessInstanceDetailsDTO();

            String id = pi.id();
            if (pi.parentProcessInstanceId() != null) {
                id = pi.parentProcessInstanceId() + ":" + id;
            }

            details.setId(id);
            details.setProcessId(processId);
            details.setBusinessKey(pi.businessKey());
            details.setDescription(pi.description());
            details.setFailed(pi.errors().isPresent());
            if (pi.errors().isPresent()) {

                details.setErrors(pi.errors().get().errors().stream()
                        .map(e -> new ErrorInfoDTO(e.failedNodeId(), e.errorId(), e.errorMessage(), e.errorDetails()))
                        .collect(Collectors.toList()));
            }
            details.setImage(
                    uriInfo.getBaseUri().toString() + "management/processes/" + processId + "/instances/" + instanceId
                            + "/image");
            details.setTags(pi.tags().values());
            details.setVariables(pi.variables());
            details.setSubprocesses(pi.subprocesses().stream()
                    .map(spi -> new ProcessInstanceDTO(spi.id(), spi.businessKey(), spi.description(), spi.tags().values(),
                            spi.errors().isPresent(), spi.process().id()))
                    .collect(Collectors.toList()));

            VariableScope variableScope = (VariableScope) ((ContextContainer) ((AbstractProcess<?>) process).process())
                    .getDefaultContext(VariableScope.VARIABLE_SCOPE);

            details.setVersionedVariables(variableScope.getVariables().stream().filter(v -> v.hasTag(Variable.VERSIONED_TAG))
                    .map(v -> v.getName()).collect(Collectors.toList()));
            return details;
        } finally {
            IdentityProvider.set(null);
        }
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found or variable was not versioned", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "Variable versions currently known to the process instance", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Returns list of versions of a given variable that is marked as versioned")
    @SuppressWarnings("unchecked")
    @GET
    @Path("/{processId}/instances/{instanceId}/variables/{name}/versions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Object> getInstanceVariableVersions(@Context UriInfo uriInfo,
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("instanceId") String instanceId,
            @Parameter(description = "Unique name of the process variable", required = true) @PathParam("name") String name,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            Process<?> process = processData.get(processId);

            Optional<ProcessInstance<?>> instance = (Optional<ProcessInstance<?>>) process.instances().findById(instanceId,
                    ProcessInstanceReadMode.READ_ONLY);

            if (instance.isEmpty()) {
                throw new ProcessInstanceNotFoundException(instanceId);
            }

            ProcessInstance<?> pi = instance.get();

            Map<String, List<Object>> versions = (Map<String, List<Object>>) ((AbstractProcessInstance<?>) pi)
                    .processInstance().getVariable(VariableScope.VERSIONED_VARIABLES);

            List<Object> varVersions = (List<Object>) versions.get(name);

            if (varVersions == null) {
                throw new VariableNotFoundException(instanceId, name);
            }

            return varVersions;
        } finally {
            IdentityProvider.set(null);
        }
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found or variable was not versioned", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "Variable versions currently known to the process instance", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Returns list of versions of a given variable that is marked as versioned")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @POST
    @Path("/{processId}/instances/{instanceId}/variables/{name}/versions/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Object> restoreInstanceVariableVersions(@Context UriInfo uriInfo,
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("instanceId") String instanceId,
            @Parameter(description = "Unique name of the process variable", required = true) @PathParam("name") String name,
            @Parameter(description = "Version number of the process variable to be made as current", required = true) @PathParam("version") Integer version,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {

        identitySupplier.buildIdentityProvider(user, groups);
        return UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {

            Process<?> process = processData.get(processId);

            Optional<ProcessInstance<?>> instance = (Optional<ProcessInstance<?>>) process.instances().findById(instanceId,
                    ProcessInstanceReadMode.MUTABLE);

            if (instance.isEmpty()) {
                throw new ProcessInstanceNotFoundException(instanceId);
            }

            ProcessInstance<?> pi = instance.get();

            Map<String, List<Object>> versions = (Map<String, List<Object>>) ((AbstractProcessInstance<?>) pi)
                    .processInstance().getVariable(VariableScope.VERSIONED_VARIABLES);

            List<Object> varVersions = (List<Object>) versions.get(name);
            // make sure that variable versions and requested version exists, otherwise throw not found
            if (varVersions == null || version < 0 || varVersions.size() <= version) {
                throw new VariableNotFoundException(instanceId, name);
            }

            // sets the variable with value from the versions
            ((AbstractProcessInstance<?>) pi).processInstance().setVariable(name, varVersions.get(version));
            ((AbstractProcessInstance) pi).updateVariables((Model) process.createModel());

            versions = (Map<String, List<Object>>) ((AbstractProcessInstance<?>) pi).processInstance()
                    .getVariable(VariableScope.VERSIONED_VARIABLES);

            varVersions = (List<Object>) versions.get(name);

            return varVersions;

        });

    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "List of available processes", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Returns process instance image with annotated active nodes")
    @SuppressWarnings("unchecked")
    @GET()
    @Path("/{processId}/instances/{instanceId}/image")
    @Produces(MediaType.APPLICATION_SVG_XML)
    public Response getInstanceImage(
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("instanceId") String instanceId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        try {
            identitySupplier.buildIdentityProvider(user, groups);
            Process<?> process = processData.get(processId);
            String image = process.image();
            if (image == null) {
                throw new ProcessImageNotFoundException(process.id());
            }

            Optional<ProcessInstance<?>> instance = (Optional<ProcessInstance<?>>) process.instances().findById(instanceId,
                    ProcessInstanceReadMode.READ_ONLY);

            if (instance.isEmpty()) {
                throw new ProcessInstanceNotFoundException(instanceId);
            }
            String path = ((AbstractProcess<?>) process).process().getId() + "/" + instanceId;
            if (process.version() != null) {
                path = "/v" + process.version().replaceAll("\\.", "_") + "/" + path;
            }
            StreamingOutput entity = new ImageStreamingOutput(instance.get().image(path));
            ResponseBuilder builder = Response.ok().entity(entity);
            return builder.header("Content-Type", "image/svg+xml").build();
        } finally {
            IdentityProvider.set(null);
        }
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "List of available processes", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Returns error information for given process instance if the instance is in error")
    @Override
    @GET
    @Path("/{processId}/instances/{processInstanceId}/error")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstanceInError(
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("processInstanceId") String processInstanceId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return doGetInstanceInError(processId, processInstanceId);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "List of available processes", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Retruns node instances currently active in given process instance")
    @Override
    @GET
    @Path("/{processId}/instances/{processInstanceId}/nodeInstances")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkItemsInProcessInstance(
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("processInstanceId") String processInstanceId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return doGetWorkItemsInProcessInstance(processId, processInstanceId);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "List of available processes", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Retriggers the node instance that is in error")
    @Override
    @POST
    @Path("/{processId}/instances/{processInstanceId}/retrigger")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retriggerInstanceInError(
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("processInstanceId") String processInstanceId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return doRetriggerInstanceInError(processId, processInstanceId);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "List of available processes", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Retriggers the node instance that is in error")
    @Override
    @POST
    @Path("/{processId}/instances/{processInstanceId}/retrigger/{errorId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retriggerInstanceInErrorByErrorId(
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("processInstanceId") String processInstanceId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("errorId") String errorId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return doRetriggerInstanceInErrorByErrorId(processId, processInstanceId, errorId);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "List of available processes", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Skips the node instance that is in error")
    @Override
    @POST
    @Path("/{processId}/instances/{processInstanceId}/skip")
    @Produces(MediaType.APPLICATION_JSON)
    public Response skipInstanceInError(
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("processInstanceId") String processInstanceId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return doSkipInstanceInError(processId, processInstanceId);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "List of available processes", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Skips the node instance that is in error")
    @Override
    @POST
    @Path("/{processId}/instances/{processInstanceId}/skip/{errorId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response skipInstanceInErrorByErrorId(
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("processInstanceId") String processInstanceId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("errorId") String errorId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return doSkipInstanceInErrorByErrorId(processId, processInstanceId, errorId);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "List of available processes", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Trigger new node instance of a given node in process instance")
    @Override
    @POST
    @Path("/{processId}/instances/{processInstanceId}/nodes/{nodeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response triggerNodeInstanceId(
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("processInstanceId") String processInstanceId,
            @Parameter(description = "Unique identifier of the node to be triggered", required = true) @PathParam("nodeId") String nodeId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return doTriggerNodeInstanceId(processId, processInstanceId, nodeId);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "List of available processes", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Retriggers (preior to triggering it cancels current instance) node instance in given process instance")
    @Override
    @POST
    @Path("/{processId}/instances/{processInstanceId}/nodeInstances/{nodeInstanceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retriggerNodeInstanceId(
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("processInstanceId") String processInstanceId,
            @Parameter(description = "Unique identifier of the node instance", required = true) @PathParam("nodeInstanceId") String nodeInstanceId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return doRetriggerNodeInstanceId(processId, processInstanceId, nodeInstanceId);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "List of available processes", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Cancels given node instance in the process instance")
    @Override
    @DELETE
    @Path("/{processId}/instances/{processInstanceId}/nodeInstances/{nodeInstanceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelNodeInstanceId(
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("processInstanceId") String processInstanceId,
            @Parameter(description = "Unique identifier of the node instance", required = true) @PathParam("nodeInstanceId") String nodeInstanceId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return doCancelNodeInstanceId(processId, processInstanceId, nodeInstanceId);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "List of available processes", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Aborts given process instance")
    @Override
    @DELETE
    @Path("/{processId}/instances/{processInstanceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelProcessInstanceId(
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("processInstanceId") String processInstanceId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return doCancelProcessInstanceId(processId, processInstanceId);
    }

    protected class ImageStreamingOutput implements StreamingOutput {

        private String image;

        public ImageStreamingOutput(String image) {
            this.image = image;
        }

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {
            output.write(image.getBytes(StandardCharsets.UTF_8));
        }
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given id was not found", content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.OBJECT))),
            @APIResponse(responseCode = "200", description = "Exported process instance", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Returns exported process instance for given instance id")
    @SuppressWarnings("unchecked")
    @GET
    @Path("/{processId}/instances/{instanceId}/export")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonExportedProcessInstance exportInstance(@Context UriInfo uriInfo,
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("instanceId") String instanceId,
            @Parameter(description = "Indicates if the instance should be aborted after export, defaults to false", required = false) @QueryParam("abort") @DefaultValue("false") final boolean abort,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {

        identitySupplier.buildIdentityProvider(user, groups);
        JsonExportedProcessInstance exported = UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            Process<?> process = processData.get(processId);
            if (process == null) {
                throw new ProcessInstanceNotFoundException(instanceId);
            }

            Optional<ProcessInstance<?>> instance = (Optional<ProcessInstance<?>>) process.instances().findById(instanceId);

            if (instance.isEmpty()) {
                throw new ProcessInstanceNotFoundException(instanceId);
            }

            ProcessInstance<?> pi = instance.get();

            ProcessInstanceExporter exporter = new ProcessInstanceExporter();
            return exporter.exportInstance(processData, instanceId, pi);
        });

        if (abort) {
            cancelProcessInstanceId(processId, instanceId, user, groups);
        }

        return exported;
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case of instance with given process id was not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "Exported process instance", content = @Content(mediaType = "application/json")) })
    @Operation(summary = "Imports exported process instance and returns its details after the import")
    @POST
    @Path("/{processId}/instances")
    @Produces(MediaType.APPLICATION_JSON)
    public ProcessInstanceDetailsDTO importInstance(@Context UriInfo uriInfo,
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups,
            @Parameter(description = "The input model for orders instance", schema = @Schema(type = SchemaType.OBJECT, implementation = Map.class)) JsonExportedProcessInstance instance) {

        identitySupplier.buildIdentityProvider(user, groups);
        return UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {

            ProcessInstanceExporter exporter = new ProcessInstanceExporter();
            ProcessInstance<?> pi = exporter.importInstance(processData, instance);

            ProcessInstanceDetailsDTO details = new ProcessInstanceDetailsDTO();
            details.setId(pi.id());
            details.setProcessId(processId);
            details.setBusinessKey(pi.businessKey());
            details.setDescription(pi.description());
            details.setFailed(pi.errors().isPresent());
            if (pi.errors().isPresent()) {

                details.setErrors(pi.errors().get().errors().stream()
                        .map(e -> new ErrorInfoDTO(e.failedNodeId(), e.errorId(), e.errorMessage(), e.errorDetails()))
                        .collect(Collectors.toList()));
            }
            details.setImage(
                    uriInfo.getBaseUri().toString() + "management/processes/" + processId + "/instances/" + pi.id()
                            + "/image");
            details.setTags(pi.tags().values());
            details.setVariables(pi.variables());

            VariableScope variableScope = (VariableScope) ((ContextContainer) ((AbstractProcess<?>) pi.process()).process())
                    .getDefaultContext(VariableScope.VARIABLE_SCOPE);

            details.setVersionedVariables(
                    variableScope.getVariables().stream().filter(v -> v.hasTag(Variable.VERSIONED_TAG))
                            .map(v -> v.getName()).collect(Collectors.toList()));
            return details;
        });

    }

}
