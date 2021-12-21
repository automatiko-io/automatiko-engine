package io.automatiko.engine.workflow.serverless;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemHandler;
import io.automatiko.engine.api.runtime.process.WorkItemManager;
import io.automatiko.engine.api.workflow.ProcessConfig;
import io.automatiko.engine.services.io.ClassPathResource;
import io.automatiko.engine.workflow.DefaultWorkItemHandlerConfig;
import io.automatiko.engine.workflow.base.core.context.variable.JsonVariableScope;

public class InjectStateWorkflowsTest {

    @Test
    public void testSingleInjectStateWorkflow() throws Exception {

        ServerlessProcess process = ServerlessProcess.from(new ClassPathResource("examples/helloworld.json")).get(0);
        assertThat(process).isNotNull();

        JsonNode data = new ObjectMapper().readTree("{\n"
                + "  \"fruits\": [ \"apple\", \"orange\", \"pear\" ],\n"
                + "  \"vegetables\": [\n"
                + "    {\n"
                + "      \"veggieName\": \"potato\",\n"
                + "      \"veggieLike\": true\n"
                + "    },\n"
                + "    {\n"
                + "      \"veggieName\": \"broccoli\",\n"
                + "      \"veggieLike\": false\n"
                + "    }\n"
                + "  ]\n"
                + "}");
        ServerlessProcessInstance pi = (ServerlessProcessInstance) process.createInstance(ServerlessModel.from(data));
        pi.start();
        System.out.println(pi.variables().toMap());
    }

    @Test
    public void testSingleInjectStateWorkflowWithConstants() throws Exception {

        ServerlessProcess process = ServerlessProcess.from(new ClassPathResource("helloworld-const.json")).get(0);
        assertThat(process).isNotNull();

        JsonNode data = new ObjectMapper().readTree("{\n"
                + "  \"fruits\": [ \"apple\", \"orange\", \"pear\" ],\n"
                + "  \"vegetables\": [\n"
                + "    {\n"
                + "      \"veggieName\": \"potato\",\n"
                + "      \"veggieLike\": true\n"
                + "    },\n"
                + "    {\n"
                + "      \"veggieName\": \"broccoli\",\n"
                + "      \"veggieLike\": false\n"
                + "    }\n"
                + "  ]\n"
                + "}");
        ServerlessProcessInstance pi = (ServerlessProcessInstance) process.createInstance(ServerlessModel.from(data));
        pi.start();
        System.out.println(pi.variables().toMap());
    }

    @Test
    public void testSingleInjectStateWorkflow2() throws Exception {

        ServerlessProcess process = ServerlessProcess.from(new ClassPathResource("helloworld-multiple.json")).get(0);
        assertThat(process).isNotNull();

        ServerlessProcessInstance pi = (ServerlessProcessInstance) process.createInstance();
        pi.start();
        System.out.println(pi.variables().toMap());
    }

    @Test
    public void testSimpleIncrement() throws Exception {

        ServerlessProcess process = ServerlessProcess.from(new ClassPathResource("simple-increment.json")).get(0);
        assertThat(process).isNotNull();

        ServerlessProcessInstance pi = (ServerlessProcessInstance) process.createInstance();
        pi.start();
        System.out.println(pi.variables().toMap());
    }

    @Test
    public void testRestOperationStateWorkflow() throws Exception {
        ProcessConfig processConfig = ServerlessProcess.processConfig();
        ((DefaultWorkItemHandlerConfig) processConfig.workItemHandlers()).register("Service Task", new WorkItemHandler() {

            @Override
            public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

                System.out.println(workItem.getParameters());

                ObjectMapper mapper = new ObjectMapper();
                ObjectNode data = mapper.createObjectNode();
                data.put("greeting", "test");

                manager.completeWorkItem(workItem.getId(),
                        Collections.singletonMap(JsonVariableScope.WORKFLOWDATA_KEY, data));
            }

            @Override
            public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {

            }
        });
        ServerlessProcess process = ServerlessProcess.from(processConfig, new ClassPathResource("examples/greeting.json"))
                .get(0);
        assertThat(process).isNotNull();

        JsonNode data = new ObjectMapper().readTree("{\n"
                + "  \"person\": {\"name\" : \"john\"}\n"
                + "}");
        ServerlessProcessInstance pi = (ServerlessProcessInstance) process.createInstance(ServerlessModel.from(data));
        pi.start();
        System.out.println(pi.variables().toMap());
    }
}
