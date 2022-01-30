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

public class SwitchStateWorkflowsTest {

    @Test
    public void testSwitchWithDataConditions() throws Exception {

        ProcessConfig processConfig = ServerlessProcess.processConfig();
        ((DefaultWorkItemHandlerConfig) processConfig.workItemHandlers()).register("Service Task", new WorkItemHandler() {

            @Override
            public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

                ObjectMapper mapper = new ObjectMapper();
                ObjectNode data = mapper.createObjectNode();

                if ("emailStart".equals(workItem.getParameter("Operation"))) {
                    data.put("status", "accepted");
                    manager.completeWorkItem(workItem.getId(),
                            Collections.singletonMap(JsonVariableScope.WORKFLOWDATA_KEY, data));
                } else if ("emailRejection".equals(workItem.getParameter("Operation"))) {
                    data.put("status", "rejected");
                    manager.completeWorkItem(workItem.getId(),
                            Collections.singletonMap(JsonVariableScope.WORKFLOWDATA_KEY, data));
                }

            }

            @Override
            public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {

            }
        });
        ServerlessProcess process = ServerlessProcess
                .from(processConfig, new ClassPathResource("switch-state/data-condition.json"))
                .get(0);
        assertThat(process).isNotNull();

        JsonNode data = new ObjectMapper().readTree("{\"applicant\" : {\"name\":\"John\", \"age\":20}}");
        ServerlessProcessInstance pi = (ServerlessProcessInstance) process.createInstance(ServerlessModel.from(data));
        pi.start();

        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(pi.variables().toMap()).hasSize(2).containsKey("status").extracting("status")
                .isEqualTo(new TextNode("accepted"));

        data = new ObjectMapper().readTree("{\"applicant\" : {\"name\":\"Mary\", \"age\":10}}");
        pi = (ServerlessProcessInstance) process.createInstance(ServerlessModel.from(data));
        pi.start();

        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(pi.variables().toMap()).hasSize(2).containsKey("status").extracting("status")
                .isEqualTo(new TextNode("rejected"));
    }

    @Test
    public void testSwitchWithEventConditions() throws Exception {

        ProcessConfig processConfig = ServerlessProcess.processConfig();
        ((DefaultWorkItemHandlerConfig) processConfig.workItemHandlers()).register("Service Task", new WorkItemHandler() {

            @Override
            public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

                ObjectMapper mapper = new ObjectMapper();
                ObjectNode data = mapper.createObjectNode();

                if ("emailStart".equals(workItem.getParameter("Operation"))) {
                    data.put("status", "accepted");
                    manager.completeWorkItem(workItem.getId(),
                            Collections.singletonMap(JsonVariableScope.WORKFLOWDATA_KEY, data));
                } else if ("emailRejection".equals(workItem.getParameter("Operation"))) {
                    data.put("status", "rejected");
                    manager.completeWorkItem(workItem.getId(),
                            Collections.singletonMap(JsonVariableScope.WORKFLOWDATA_KEY, data));
                }

            }

            @Override
            public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {

            }
        });
        ServerlessProcess process = ServerlessProcess
                .from(processConfig, new ClassPathResource("switch-state/event-condition.json"))
                .get(0);
        assertThat(process).isNotNull();

        JsonNode data = new ObjectMapper().readTree("{\"applicant\" : {\"name\":\"John\", \"age\":20}}");
        ServerlessProcessInstance pi = (ServerlessProcessInstance) process.createInstance(ServerlessModel.from(data));
        pi.start();

        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        pi.send(Sig.of("Message-visaApprovedEvent", data));

        assertThat(pi.variables().toMap()).hasSize(2).containsKey("status").extracting("status")
                .isEqualTo(new TextNode("accepted"));

        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        data = new ObjectMapper().readTree("{\"applicant\" : {\"name\":\"Mary\", \"age\":10}}");
        pi = (ServerlessProcessInstance) process.createInstance(ServerlessModel.from(data));
        pi.start();

        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        pi.send(Sig.of("Message-visaRejectedEvent", data));

        assertThat(pi.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(pi.variables().toMap()).hasSize(2).containsKey("status").extracting("status")
                .isEqualTo(new TextNode("rejected"));
    }

}
