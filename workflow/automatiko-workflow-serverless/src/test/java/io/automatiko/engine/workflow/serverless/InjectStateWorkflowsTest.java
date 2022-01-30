package io.automatiko.engine.workflow.serverless;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.services.io.ClassPathResource;

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
        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(pi.variables().toMap()).hasSize(3).containsKey("result").extracting("result")
                .isEqualTo(new TextNode("Hello World!"));
    }

    @Test
    public void testMultipleInjectStateWorkflow() throws Exception {

        ServerlessProcess process = ServerlessProcess.from(new ClassPathResource("inject-state/helloworld-multiple.json"))
                .get(0);
        assertThat(process).isNotNull();

        ServerlessProcessInstance pi = (ServerlessProcessInstance) process.createInstance();
        pi.start();
        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(pi.variables().toMap()).hasSize(1).containsKey("result").extracting("result")
                .isEqualTo(new TextNode("Goodbye World!"));
    }

    @Test
    public void testMultipleInjectStateWithAnnotationsWorkflow() throws Exception {

        ServerlessProcess process = ServerlessProcess
                .from(new ClassPathResource("inject-state/helloworld-multiple-with-annotations.json"))
                .get(0);
        assertThat(process).isNotNull();

        JsonNode data = new ObjectMapper().readTree("{\n"
                + "  \"name\": \"john\"\n"
                + "}");
        ServerlessProcessInstance pi = (ServerlessProcessInstance) process.createInstance(ServerlessModel.from(data));
        pi.start();
        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(pi.variables().toMap()).hasSize(2).containsKey("result").extracting("result")
                .isEqualTo(new TextNode("Goodbye World!"));
        assertThat(pi.tags().values()).hasSize(4).contains("a", "b", "c", "john");
    }

}
