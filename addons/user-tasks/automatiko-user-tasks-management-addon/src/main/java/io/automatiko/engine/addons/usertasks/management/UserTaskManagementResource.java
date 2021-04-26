package io.automatiko.engine.addons.usertasks.management;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.WorkItem;
import io.quarkus.qute.Engine;
import io.quarkus.qute.RawString;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;

@Tag(name = "User task Management", description = "Process management operations on top of the service", externalDocs = @ExternalDocumentation(description = "Manangement UI", url = "/management/processes/ui"))
@Path("/management/tasks")
@Authenticated
public class UserTaskManagementResource {

    private static final String PROCESS_INSTANCE_NOT_FOUND = "not-found-template";
    private static final String TASK_INSTANCE_NOT_FOUND = "not-found-template";
    private static final String DEFAULT_TEMPLATE = "default-task-template";

    private Map<String, Process<?>> processData = new LinkedHashMap<String, Process<?>>();

    private Application application;

    private IdentitySupplier identitySupplier;

    private Engine engine;

    @Inject
    public UserTaskManagementResource(Application application, Instance<Process<?>> availableProcesses,
            IdentitySupplier identitySupplier, Engine engine) {
        this.processData = availableProcesses == null ? Collections.emptyMap()
                : availableProcesses.stream().collect(Collectors.toMap(p -> p.id(), p -> p));
        this.application = application;
        this.identitySupplier = identitySupplier;
        this.engine = engine;
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("{processId}/{pInstanceId}/{taskId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(@Context UriInfo uriInfo,
            @Parameter(description = "Unique identifier of the process", required = true) @PathParam("processId") String processId,
            @Parameter(description = "Unique identifier of the instance", required = true) @PathParam("pInstanceId") String pInstanceId,
            @Parameter(description = "Unique identifier of the task", required = true) @PathParam("taskId") String taskId,
            @Parameter(description = "User identifier as alternative autroization info", required = false, hidden = true) @QueryParam("user") final String user,
            @Parameter(description = "Groups as alternative autroization info", required = false, hidden = true) @QueryParam("group") final List<String> groups) {

        IdentityProvider identityProvider = identitySupplier.buildIdentityProvider(user, groups);
        try {
            Process<?> process = processData.get(processId);
            if (process == null) {
                return engine.getTemplate(PROCESS_INSTANCE_NOT_FOUND).instance().data("instanceId", pInstanceId);
            }
            Optional<ProcessInstance<?>> instance = (Optional<ProcessInstance<?>>) process.instances().findById(pInstanceId,
                    ProcessInstanceReadMode.READ_ONLY);

            if (instance.isEmpty()) {
                return engine.getTemplate(PROCESS_INSTANCE_NOT_FOUND).instance().data("instanceId", pInstanceId);
            }

            ProcessInstance<?> pi = instance.get();

            WorkItem task = pi.workItem(taskId, SecurityPolicy.of(identityProvider));
            Template template = getTemplate(pi.id(), task);

            if (template == null) {
                template = engine.getTemplate(DEFAULT_TEMPLATE);
            }
            String link = task.getReferenceId() + "?redirect=true";
            if (user != null && !user.isEmpty()) {
                link += "&user=" + user;
            }
            if (groups != null && !groups.isEmpty()) {
                for (String group : groups) {
                    link += "&group=" + group;
                }
            }

            TemplateInstance templateInstance = template.data("task", task).data("url",
                    new RawString(link));
            templateInstance.data("inputs", process.taskInputs(task.getId(), task.getName(), task.getParameters()));

            Map<String, Object> results = task.getResults();

            task.getParameters().entrySet().stream().forEach(e -> results.putIfAbsent(e.getKey(), e.getValue()));

            templateInstance.data("results", process.taskOutputs(task.getId(), task.getName(), results));

            return templateInstance;
        } catch (WorkItemNotFoundException e) {

            return engine.getTemplate(TASK_INSTANCE_NOT_FOUND).instance().data("taskId", taskId);

        } finally {
            IdentityProvider.set(null);
        }
    }

    @GET
    @Path("link/{details}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance fromEmailLink(@Context UriInfo uriInfo,
            @Parameter(description = "User task link details", required = true) @PathParam("details") String details) {
        String[] taskCoordinates = new String(Base64.getDecoder().decode(details), StandardCharsets.UTF_8).split("\\|");
        if (taskCoordinates.length > 3) {
            return get(uriInfo, taskCoordinates[0], taskCoordinates[1], taskCoordinates[2], taskCoordinates[3],
                    Collections.emptyList());
        } else {
            return get(uriInfo, taskCoordinates[0], taskCoordinates[1], taskCoordinates[2], null,
                    Collections.emptyList());
        }
    }

    /**
     * Retrieve custom template for the email in following order
     * <ul>
     * <li>processId.taskname</li>
     * <li>taskname</li>
     * </ul>
     * 
     * @param processId id of the process task belongs to
     * @param humanTask an instance of user task
     * @return template for email if found otherwise null
     */
    protected Template getTemplate(String processId, WorkItem humanTask) {
        Template template = engine.getTemplate(processId + "." + humanTask.getName());
        if (template == null) {
            template = engine.getTemplate(humanTask.getName());
        }

        return template;
    }

}
