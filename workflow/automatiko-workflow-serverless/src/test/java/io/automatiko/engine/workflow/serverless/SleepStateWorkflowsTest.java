package io.automatiko.engine.workflow.serverless;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.TextNode;

import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.services.io.ClassPathResource;

public class SleepStateWorkflowsTest {

    @Test
    public void testSleepStateWorkflow() throws Exception {

        ServerlessProcess process = ServerlessProcess.from(new ClassPathResource("sleep-state/helloworld-sleep.json"))
                .get(0);
        assertThat(process).isNotNull();

        ServerlessProcessInstance pi = (ServerlessProcessInstance) process.createInstance();
        pi.start();
        Thread.sleep(3000);
        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(pi.variables().toMap()).hasSize(1).containsKey("result").extracting("result")
                .isEqualTo(new TextNode("Goodbye World!"));
    }

}
