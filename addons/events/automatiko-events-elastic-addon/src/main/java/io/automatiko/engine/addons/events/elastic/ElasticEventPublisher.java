package io.automatiko.engine.addons.events.elastic;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.event.EventPublisher;
import io.automatiko.engine.services.event.ProcessInstanceDataEvent;
import io.automatiko.engine.services.event.UserTaskInstanceDataEvent;
import io.automatiko.engine.services.event.impl.NodeInstanceEventBody;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ElasticEventPublisher implements EventPublisher {

    public static final String AUDIT_KEY = "quarkus.automatiko.events.elastic.audit";
    public static final String INSTANCE_KEY = "quarkus.automatiko.events.elastic.instance";
    public static final String TASKS_KEY = "quarkus.automatiko.events.elastic.tasks";
    public static final String AUDIT_INDEX_KEY = "quarkus.automatiko.events.elastic.audit-index";

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticEventPublisher.class);

    private RestClient restClient;

    private ObjectMapper mapper;

    private Optional<Boolean> audit;

    private Optional<Boolean> instance;

    private Optional<Boolean> tasks;

    private Optional<String> auditIndex;

    @Inject
    public ElasticEventPublisher(RestClient restClient, ObjectMapper mapper,
            @ConfigProperty(name = AUDIT_KEY) Optional<Boolean> audit,
            @ConfigProperty(name = INSTANCE_KEY) Optional<Boolean> instance,
            @ConfigProperty(name = TASKS_KEY) Optional<Boolean> tasks,
            @ConfigProperty(name = AUDIT_INDEX_KEY) Optional<String> auditIndex) {
        this.restClient = restClient;
        this.mapper = mapper;
        this.audit = audit;
        this.instance = instance;
        this.tasks = tasks;
        this.auditIndex = auditIndex;
    }

    @Override
    public void publish(DataEvent<?> event) {
        try {
            Request request;
            Map<String, Object> payload;
            if (event instanceof ProcessInstanceDataEvent) {

                ProcessInstanceDataEvent pevent = (ProcessInstanceDataEvent) event;
                if (instance.orElse(true)) {
                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("instanceId", pevent.getData().getId());
                    metadata.put("processId", pevent.getData().getProcessId());
                    metadata.put("rootInstanceId", pevent.getData().getRootInstanceId());
                    metadata.put("rootProcessId", pevent.getData().getRootProcessId());
                    metadata.put("parentInstanceId", pevent.getData().getParentInstanceId());
                    metadata.put("businessKey", pevent.getData().getBusinessKey());
                    metadata.put("state", pevent.getData().getState());
                    metadata.put("tags", pevent.getData().getTags());
                    if (pevent.getData().getRoles() != null) {
                        metadata.put("roles", pevent.getData().getRoles());
                    }
                    if (pevent.getData().getVisibleTo() != null) {
                        metadata.put("visibleTo", pevent.getData().getVisibleTo());
                    }
                    metadata.put("startDate", pevent.getData().getStartDate());
                    metadata.put("endDate", pevent.getData().getEndDate());

                    payload = new LinkedHashMap<>(pevent.getData().getVariables());

                    payload.put("_metadata", metadata);

                    request = new Request(
                            "PUT",
                            "/" + pevent.getData().sourceInstance().process().id() + "/_doc/" + pevent.getData().getId());
                    request.setJsonEntity(mapper.writeValueAsString(payload));

                    sendRequest(request, event);
                }
                if (audit.orElse(false)) {
                    String index = auditIndex.orElse("atk_audit");

                    StringBuilder bulkRequestBody = new StringBuilder();
                    for (NodeInstanceEventBody nevent : pevent.getData().getNodeInstances()) {
                        String actionMetaData = String.format("{ \"index\" : { \"_index\" : \"%s\", \"_id\" : \"%s\" } }%n",
                                index, nevent.getId());
                        Map<String, Object> audit = new LinkedHashMap<>();
                        audit.put("instanceId", pevent.getData().getId());
                        audit.put("processId", pevent.getData().getProcessId());
                        audit.put("rootInstanceId", pevent.getData().getRootInstanceId());
                        audit.put("rootProcessId", pevent.getData().getRootProcessId());
                        audit.put("parentInstanceId", pevent.getData().getParentInstanceId());
                        audit.put("businessKey", pevent.getData().getBusinessKey());
                        audit.put("nodeDefinitionId", nevent.getNodeDefinitionId());
                        audit.put("nodeId", nevent.getNodeId());
                        audit.put("nodeName", nevent.getNodeName());
                        audit.put("nodeType", nevent.getNodeType());
                        audit.put("triggerTime", nevent.getTriggerTime());
                        audit.put("leaveTime", nevent.getLeaveTime());
                        bulkRequestBody.append(actionMetaData);
                        bulkRequestBody.append(mapper.writeValueAsString(audit));
                        bulkRequestBody.append("\n");

                    }
                    request = new Request(
                            "POST",
                            "/" + index + "/_bulk");
                    request.setJsonEntity(bulkRequestBody.toString());
                    sendRequest(request, event);

                }

            } else if (event instanceof UserTaskInstanceDataEvent && tasks.orElse(true)) {

                UserTaskInstanceDataEvent uevent = (UserTaskInstanceDataEvent) event;
                Set<String> potentialOwners = new LinkedHashSet<String>();
                if (uevent.getData().getPotentialUsers() != null) {
                    potentialOwners.addAll(uevent.getData().getPotentialUsers());
                }
                if (uevent.getData().getPotentialGroups() != null) {
                    potentialOwners.addAll(uevent.getData().getPotentialGroups());
                }
                if (uevent.getData().getAdminUsers() != null) {
                    potentialOwners.addAll(uevent.getData().getAdminUsers());
                }
                if (uevent.getData().getAdminGroups() != null) {
                    potentialOwners.addAll(uevent.getData().getAdminUsers());
                }
                // remove any excluded users known
                if (uevent.getData().getExcludedUsers() != null) {
                    potentialOwners.removeAll(uevent.getData().getExcludedUsers());
                }

                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("processInstanceId", uevent.getData().getProcessInstanceId());
                metadata.put("processId", uevent.getData().getProcessId());
                metadata.put("rootInstanceId", uevent.getData().getRootProcessInstanceId());
                metadata.put("rootProcessId", uevent.getData().getRootProcessId());
                metadata.put("referenceName", uevent.getData().getReferenceName());

                payload = new LinkedHashMap<>();
                payload.put("instanceId", uevent.getData().getId());
                payload.put("name", uevent.getData().getTaskName());
                payload.put("description", uevent.getData().getTaskDescription());
                payload.put("state", uevent.getData().getState());
                payload.put("owner", uevent.getData().getActualOwner());
                payload.put("potentialOwners", potentialOwners);
                payload.put("excludedUsers", uevent.getData().getExcludedUsers());
                payload.put("startDate", uevent.getData().getStartDate());
                payload.put("endDate", uevent.getData().getCompleteDate());
                payload.put("inputs", uevent.getData().getInputs());
                payload.put("outputs", uevent.getData().getOutputs());

                payload.put("_metadata", metadata);

                request = new Request(
                        "PUT",
                        "/tasks/_doc/" + uevent.getData().getId());
                request.setJsonEntity(mapper.writeValueAsString(payload));

                sendRequest(request, event);
            } else {
                return;
            }

        } catch (IOException e) {
            LOGGER.error("Error when publishing event to elastic", e);
        }

    }

    @Override
    public void publish(Collection<DataEvent<?>> events) {
        for (DataEvent<?> event : events) {
            publish(event);
        }
    }

    protected void sendRequest(Request request, DataEvent<?> event) {

        restClient.performRequestAsync(request, new ResponseListener() {

            @Override
            public void onSuccess(Response response) {
                LOGGER.debug("Event {} successfully published to elastic", event);
            }

            @Override
            public void onFailure(Exception exception) {
                LOGGER.error("Event {} failed to be published to elastic", event, exception);

            }
        });
    }
}
