package io.automatiko.engine.codegen.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.process.image.SvgBpmnProcessImageGenerator;
import io.automatiko.engine.services.io.ClassPathResource;
import io.automatiko.engine.workflow.bpmn2.BpmnProcess;
import io.automatiko.engine.workflow.bpmn2.BpmnProcessCompiler;

public class SvgProcessImageGeneratorTest {

    @Test
    public void testSingleUserTaskProcessImageGeneration() throws IOException {

        testGeneration("usertask/UserTasksProcess.bpmn2");
    }

    @Test
    public void testGatewaysProcessImageGeneration() throws IOException {

        testGeneration("gateway/ExclusiveSplit.bpmn2");
    }

    @Test
    public void testOrdersProcessImageGeneration() throws IOException {

        testGeneration("Process orders.bpmn2");
    }

    @Test
    public void testZipMIProcessImageGeneration() throws IOException {

        testGeneration("zip file processor.bpmn2");
    }

    @Test
    public void testEmbededSPMIProcessImageGeneration() throws IOException {

        testGeneration("Multi Approval.bpmn2");
    }

    @Test
    public void testEventBasedGatewayProcessImageGeneration() throws IOException {

        testGeneration("gateway/EventBasedSplit.bpmn2");
    }

    @Test
    public void testSubprocessProcessImageGeneration() throws IOException {

        testGeneration("subprocess/EmbeddedSubProcess.bpmn2");
    }

    @Test
    public void testCallactivityProcessImageGeneration() throws IOException {

        testGeneration("subprocess/CallActivity.bpmn2");
    }

    @Test
    public void testMessageStartProcessImageGeneration() throws IOException {

        testGeneration("messagestartevent/MessageAndMessageStartEvent.bpmn2");
    }

    @Test
    public void testMessageEndProcessImageGeneration() throws IOException {

        testGeneration("messagestartevent/MessageEndEvent.bpmn2");
    }

    @Test
    public void testTimerStartProcessImageGeneration() throws IOException {

        testGeneration("timer/StartTimerCycle.bpmn2");
    }

    @Test
    public void testTimerIntermediateProcessImageGeneration() throws IOException {

        testGeneration("timer/IntermediateCatchEventTimerDurationISO.bpmn2");
    }

    @Test
    public void testBusinessRuleTaskProcessImageGeneration() throws IOException {

        testGeneration("decision/models/dmnprocess.bpmn2");
    }

    @Test
    public void testEventSubprocessProcessImageGeneration() throws IOException {

        testGeneration("event-subprocess/EventSubprocessTimer.bpmn2");
    }

    @Test
    public void testEventSubprocessSignalProcessImageGeneration() throws IOException {

        testGeneration("event-subprocess/EventSubprocessSignal.bpmn2");
    }

    @Test
    public void testBoundaryProcessImageGeneration() throws IOException {

        testGeneration("messageevent/BoundaryMessageEventOnTask.bpmn2");
    }

    public void testGeneration(String processResource) throws IOException {

        BpmnProcessCompiler compiler = new BpmnProcessCompiler();

        BpmnProcess process = compiler.from(null, new ClassPathResource(processResource)).get(0);
        assertThat(process).isNotNull();

        SvgBpmnProcessImageGenerator generator = new SvgBpmnProcessImageGenerator((WorkflowProcess) process.process());

        String svg = generator.generate();

        assertThat(svg).isNotEmpty();

        Files.write(Paths.get("target", "test.svg"), svg.getBytes(StandardCharsets.UTF_8));
    }

}
