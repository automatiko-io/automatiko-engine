package io.automatiko.engine.workflow.serverless;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;

import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.services.io.ClassPathResource;
import io.automatiko.engine.workflow.Sig;

public class CallbackStateWorkflowsTest {

    @Test
    public void testCallbackStateWorkflow() throws Exception {

        ServerlessProcess process = ServerlessProcess
                .from(new ClassPathResource("callback-state/simple-callback.json"))
                .get(0);
        assertThat(process).isNotNull();

        ServerlessProcessInstance pi = (ServerlessProcessInstance) process.createInstance();
        pi.start();
        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(pi.variables().toMap()).hasSize(1).containsKey("count").extracting("count")
                .isEqualTo(new IntNode(10));

        JsonNode data = new ObjectMapper().readTree("{\n"
                + "  \"name\": \"john\"\n"
                + "}");
        pi.send(Sig.of("Message-CarBidEvent", data));

        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(pi.variables().toMap()).hasSize(2).containsKey("count").extracting("count")
                .isEqualTo(new IntNode(11));
    }

}
