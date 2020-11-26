package io.automatik.engine.workflow.serverless;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.error.Error;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.retry.RetryDefinition;
import io.serverlessworkflow.api.start.Start;
import io.serverlessworkflow.api.states.DelayState;
import io.serverlessworkflow.api.workflow.Events;
import io.serverlessworkflow.api.workflow.Functions;
import io.serverlessworkflow.api.workflow.Retries;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.serverlessworkflow.api.states.DefaultState.Type.DELAY;
import static org.junit.jupiter.api.Assertions.*;

public class WorkflowToMarkupTest {
    @Test
    public void testSingleState() {

        Workflow workflow = new Workflow().withId("test-workflow").withName("test-workflow-name").withVersion("1.0")
                .withStates(Arrays.asList(
                        new DelayState().withName("delayState").withType(DELAY)
                                .withStart(
                                        new Start().withKind(Start.Kind.DEFAULT)
                                )
                                .withEnd(
                                        new End().withKind(End.Kind.DEFAULT)
                                )
                                .withTimeDelay("PT1M")
                        )
                );

        assertNotNull(workflow);
        assertEquals(1, workflow.getStates().size());
        State state = workflow.getStates().get(0);
        assertTrue(state instanceof DelayState);

        assertNotNull(Workflow.toJson(workflow));
        assertNotNull(Workflow.toYaml(workflow));
    }

    @Test
    public void testSingleFunction() {

        Workflow workflow = new Workflow().withId("test-workflow").withName("test-workflow-name").withVersion("1.0")
                .withFunctions(new Functions(Arrays.asList(
                        new FunctionDefinition().withName("testFunction")
                                .withOperation("testSwaggerDef#testOperationId")))
                )
                .withStates(Arrays.asList(
                        new DelayState().withName("delayState").withType(DELAY)
                                .withStart(
                                        new Start().withKind(Start.Kind.DEFAULT)
                                )
                                .withEnd(
                                        new End().withKind(End.Kind.DEFAULT)
                                )
                                .withTimeDelay("PT1M")
                        )
                );

        assertNotNull(workflow);
        assertEquals(1, workflow.getStates().size());
        State state = workflow.getStates().get(0);
        assertTrue(state instanceof DelayState);
        assertNotNull(workflow.getFunctions());
        assertEquals(1, workflow.getFunctions().getFunctionDefs().size());
        assertEquals("testFunction", workflow.getFunctions().getFunctionDefs().get(0).getName());

        assertNotNull(Workflow.toJson(workflow));
        assertNotNull(Workflow.toYaml(workflow));
    }

    @Test
    public void testSingleEvent() {

        Workflow workflow = new Workflow().withId("test-workflow").withName("test-workflow-name").withVersion("1.0")
                .withEvents(new Events(Arrays.asList(
                        new EventDefinition().withName("testEvent").withSource("testSource").withType("testType")
                                .withKind(EventDefinition.Kind.PRODUCED)))
                )
                .withFunctions(new Functions(Arrays.asList(
                        new FunctionDefinition().withName("testFunction")
                                .withOperation("testSwaggerDef#testOperationId")))
                )
                .withStates(Arrays.asList(
                        new DelayState().withName("delayState").withType(DELAY)
                                .withStart(
                                        new Start().withKind(Start.Kind.DEFAULT)
                                )
                                .withEnd(
                                        new End().withKind(End.Kind.DEFAULT)
                                )
                                .withTimeDelay("PT1M")
                        )
                );

        assertNotNull(workflow);
        assertEquals(1, workflow.getStates().size());
        State state = workflow.getStates().get(0);
        assertTrue(state instanceof DelayState);
        assertNotNull(workflow.getFunctions());
        assertEquals(1, workflow.getFunctions().getFunctionDefs().size());
        assertEquals("testFunction", workflow.getFunctions().getFunctionDefs().get(0).getName());
        assertNotNull(workflow.getEvents());
        assertEquals(1, workflow.getEvents().getEventDefs().size());
        assertEquals("testEvent", workflow.getEvents().getEventDefs().get(0).getName());
        assertEquals(EventDefinition.Kind.PRODUCED, workflow.getEvents().getEventDefs().get(0).getKind());

        assertNotNull(Workflow.toJson(workflow));
        assertNotNull(Workflow.toYaml(workflow));
    }

    @Test
    public void testSingleFunctionWithOnErrors() {

        Workflow workflow = new Workflow().withId("test-workflow").withName("test-workflow-name").withVersion("1.0")
                .withFunctions(new Functions(Arrays.asList(
                        new FunctionDefinition().withName("testFunction")
                                .withOperation("testSwaggerDef#testOperationId")))
                )
                .withRetries(new Retries(Arrays.asList(
                        new RetryDefinition().withName("TestRetryStrat")
                        .withMaxAttempts("10")
                        .withDelay("PT1S")
                )))
                .withStates(Arrays.asList(
                        new DelayState().withName("delayState").withType(DELAY)
                                .withStart(
                                        new Start().withKind(Start.Kind.DEFAULT)
                                )
                                .withEnd(
                                        new End().withKind(End.Kind.DEFAULT)
                                )
                                .withTimeDelay("PT1M")
                                .withOnErrors(Arrays.asList(
                                    new Error().withError("TestError").withCode("500")
                                            .withRetryRef("TestRetryStrat")
                                        .withEnd(new End().withKind(End.Kind.DEFAULT))
                                ))
                        )
                );

        assertNotNull(workflow);
        assertEquals(1, workflow.getStates().size());
        assertEquals(1, workflow.getRetries().getRetryDefs().size());
        State state = workflow.getStates().get(0);
        assertTrue(state instanceof DelayState);
        assertNotNull(workflow.getFunctions());
        assertEquals(1, workflow.getFunctions().getFunctionDefs().size());
        assertEquals("testFunction", workflow.getFunctions().getFunctionDefs().get(0).getName());

        DelayState delayState = (DelayState) workflow.getStates().get(0);
        assertEquals(1, delayState.getOnErrors().size());
        Error error = delayState.getOnErrors().get(0);
        assertEquals("TestError", error.getError());
        assertEquals("500", error.getCode());
        assertEquals("TestRetryStrat", error.getRetryRef());

        assertNotNull(Workflow.toJson(workflow));
        assertNotNull(Workflow.toYaml(workflow));
    }
}
