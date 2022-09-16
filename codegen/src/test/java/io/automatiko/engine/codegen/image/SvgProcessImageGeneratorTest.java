package io.automatiko.engine.codegen.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.process.image.SvgBpmnProcessImageGenerator;
import io.automatiko.engine.services.io.ClassPathResource;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.bpmn2.BpmnProcess;
import io.automatiko.engine.workflow.bpmn2.BpmnProcessCompiler;
import io.automatiko.engine.workflow.builder.ParallelSplitNodeBuilder;
import io.automatiko.engine.workflow.builder.RestServiceNodeBuilder;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

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

    @Test
    public void testWorkflowAsCodeImageGeneration() throws IOException {
        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("parallelGateway", "test workflow with parallel gateway", "1")
                .dataObject("name", String.class)
                .dataObject("greeting", String.class, Variable.OUTPUT_TAG)
                .dataObject("age", Integer.class);

        ParallelSplitNodeBuilder split = builder.start("start here").then()
                .log("execute script", "Hello world").thenParallelSplit("gateway");

        split.then().log("first branch", "first branch").then().end("end");

        split.then().log("second branch", "second branch").then().end("error");

        SvgBpmnProcessImageGenerator generator = new SvgBpmnProcessImageGenerator(builder.get());

        String svg = generator.generate();

        assertThat(svg).isNotEmpty();

        Files.write(Paths.get("target", "test.svg"), svg.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testWorkflowAsCodeWithErrorImageGeneration() throws IOException {
        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("service", "Sample workflow calling service", "1")
                .dataObject("name", Long.class, Variable.INPUT_TAG)
                .dataObject("pet", Object.class, Variable.OUTPUT_TAG);

        RestServiceNodeBuilder service = builder.start("start here").then()
                .log("execute script", "Hello world").then()
                .restService("get pet from the store");

        service.toDataObject("pet",
                service.openApi("api/swagger.json").operation("getPetById").fromDataObject("name")).then()
                .end("that's it");

        service.onError("404").then().log("Not found", "Pet with id {} not found", "name").then().end("done");

        SvgBpmnProcessImageGenerator generator = new SvgBpmnProcessImageGenerator(builder.get());

        String svg = generator.generate();

        assertThat(svg).isNotEmpty();

        Files.write(Paths.get("target", "test.svg"), svg.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testWorkflowAsCodeImageGenerationForEach() throws IOException {
        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class)
                .dataObject("inputs", List.class);

        builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .repeat("inputs")
                .users("john").outputToDataObject("value", "item")
                .then()
                .end("done");

        SvgBpmnProcessImageGenerator generator = new SvgBpmnProcessImageGenerator(builder.get());

        String svg = generator.generate();

        assertThat(svg).isNotEmpty();

        Files.write(Paths.get("target", "test.svg"), svg.getBytes(StandardCharsets.UTF_8));
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
