package io.automatiko.engine.workflow.serverless;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemHandler;
import io.automatiko.engine.api.runtime.process.WorkItemManager;
import io.automatiko.engine.api.workflow.ProcessConfig;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.services.io.ClassPathResource;
import io.automatiko.engine.workflow.DefaultWorkItemHandlerConfig;
import io.automatiko.engine.workflow.Sig;
import io.automatiko.engine.workflow.base.core.context.variable.JsonVariableScope;

public class EventStateWorkflowsTest {

    @Test
    public void testStartWithEventStateWorkflow() throws Exception {

        ProcessConfig processConfig = ServerlessProcess.processConfig();
        ((DefaultWorkItemHandlerConfig) processConfig.workItemHandlers()).register("Service Task", new WorkItemHandler() {

            @Override
            public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

                System.out.println(workItem.getParameters());

                ObjectMapper mapper = new ObjectMapper();
                ObjectNode data = mapper.createObjectNode();
                data.put("greeting", "Hello " + ((TextNode) workItem.getParameter("name")).asText());

                manager.completeWorkItem(workItem.getId(),
                        Collections.singletonMap(JsonVariableScope.WORKFLOWDATA_KEY, data));
            }

            @Override
            public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {

            }
        });
        ServerlessProcess process = ServerlessProcess
                .from(processConfig, new ClassPathResource("event-state/event-state-greeting.json"))
                .get(0);
        assertThat(process).isNotNull();

        JsonNode data = new ObjectMapper().readTree("{\n"
                + "  \"data\": {\"greet\" : { \"name\" : \"john\"}}\n"
                + "}");

        ServerlessProcessInstance pi = (ServerlessProcessInstance) process.createInstance();
        pi.start("Message-GreetingEvent", null, data);
        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(pi.variables().toMap()).hasSize(2).containsKey("greeting").extracting("greeting")
                .isEqualTo(new TextNode("Hello john"));
    }

    @Test
    public void testEventStateWorkflow() throws Exception {

        ProcessConfig processConfig = ServerlessProcess.processConfig();
        ((DefaultWorkItemHandlerConfig) processConfig.workItemHandlers()).register("Service Task", new WorkItemHandler() {

            @Override
            public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

                System.out.println(workItem.getParameters());

                ObjectMapper mapper = new ObjectMapper();
                ObjectNode data = mapper.createObjectNode();
                data.put("greeting", "Hello " + ((TextNode) workItem.getParameter("name")).asText());

                manager.completeWorkItem(workItem.getId(),
                        Collections.singletonMap(JsonVariableScope.WORKFLOWDATA_KEY, data));
            }

            @Override
            public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {

            }
        });
        ServerlessProcess process = ServerlessProcess
                .from(processConfig, new ClassPathResource("event-state/event-state-greeting2.json"))
                .get(0);
        assertThat(process).isNotNull();

        ServerlessProcessInstance pi = (ServerlessProcessInstance) process.createInstance();
        pi.start();

        JsonNode data = new ObjectMapper().readTree("{\n"
                + "  \"data\": {\"greet\" : { \"name\" : \"john\"}}\n"
                + "}");

        pi.send(Sig.of("Message-GreetingEvent", data));
        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(pi.variables().toMap()).hasSize(3).containsKey("greeting").extracting("greeting")
                .isEqualTo(new TextNode("Hello john"));
    }
}
