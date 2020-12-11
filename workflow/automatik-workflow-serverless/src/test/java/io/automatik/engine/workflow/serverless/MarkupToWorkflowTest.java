package io.automatik.engine.workflow.serverless;

import io.automatik.engine.workflow.serverless.utils.WorkflowTestUtils;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.states.EventState;
import io.serverlessworkflow.api.states.OperationState;
import io.serverlessworkflow.api.states.SwitchState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class MarkupToWorkflowTest {

    @ParameterizedTest
    @ValueSource(strings = {"/examples/applicantrequest.json", "/examples/applicantrequest.yml",
            "/examples/carauctionbids.json", "/examples/carauctionbids.yml",
            "/examples/creditcheck.json", "/examples/creditcheck.yml",
            "/examples/eventbasedgreeting.json", "/examples/eventbasedgreeting.yml",
            "/examples/finalizecollegeapplication.json", "/examples/finalizecollegeapplication.yml",
            "/examples/greeting.json", "/examples/greeting.yml",
            "/examples/helloworld.json", "/examples/helloworld.yml",
            "/examples/jobmonitoring.json", "/examples/jobmonitoring.yml",
            "/examples/monitorpatient.json", "/examples/monitorpatient.yml",
            "/examples/parallel.json", "/examples/parallel.yml",
            "/examples/provisionorder.json", "/examples/provisionorder.yml",
            "/examples/sendcloudevent.json", "/examples/sendcloudevent.yml",
            "/examples/solvemathproblems.json", "/examples/solvemathproblems.yml",
            "/examples/foreachstatewithactions.json", "/examples/foreachstatewithactions.yml",
            "/examples/periodicinboxcheck.json", "/examples/periodicinboxcheck.yml",
            "/examples/vetappointmentservice.json", "/examples/vetappointmentservice.yml",
            "/examples/eventbasedtransition.json", "/examples/eventbasedtransition.yml"
    })
    public void testSpecExamplesParsing(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() > 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/applicantrequest.json", "/features/applicantrequest.yml"})
    public void testSpecFeatureFunctionRef(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() > 0);

        assertNotNull(workflow.getFunctions());
        assertTrue(workflow.getFunctions().getFunctionDefs().size() == 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/vetappointment.json", "/features/vetappointment.yml"})
    public void testSpecFeatureEventRef(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() > 0);

        assertNotNull(workflow.getEvents());
        assertTrue(workflow.getEvents().getEventDefs().size() == 2);
    }
    @ParameterizedTest
    @ValueSource(strings = {"/features/retryforservicecall.json", "/features/retryforservicecall.yml"})
    public void testRetryForServiceCall(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());
        assertNotNull(workflow.getRetries());

        assertEquals(1, workflow.getRetries().getRetryDefs().size());
        assertEquals(1, workflow.getStates().size());

        assertTrue(workflow.getStates().get(0) instanceof OperationState);

        OperationState operationState = (OperationState) workflow.getStates().get(0);
        assertNotNull(operationState);
        assertNotNull(operationState.getOnErrors());
        assertEquals(1, operationState.getOnErrors().size());

    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/compensation.json", "/features/compensation.yml"})
    public void testCompensationWorkflow(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());

        assertEquals(2, workflow.getStates().size());

        assertTrue(workflow.getStates().get(0) instanceof OperationState);
        assertTrue(workflow.getStates().get(1) instanceof OperationState);

        OperationState operationState = (OperationState) workflow.getStates().get(0);
        assertNotNull(operationState.getCompensatedBy());
        assertEquals("SecondExecService", operationState.getCompensatedBy());

        OperationState operationState2 = (OperationState) workflow.getStates().get(1);
        assertTrue(operationState2.isUsedForCompensation());

    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/patientonboarding.json", "/features/patientonboarding.yml"})
    public void testPatientOnboardingWorkflow(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());

        assertEquals(3, workflow.getStates().size());

        assertTrue(workflow.getStates().get(0) instanceof EventState);
        assertTrue(workflow.getStates().get(1) instanceof SwitchState);
        assertTrue(workflow.getStates().get(2) instanceof OperationState);

        EventState eventState = (EventState) workflow.getStates().get(0);
        assertNotNull(eventState.getCompensatedBy());
        assertEquals("DoAbort", eventState.getCompensatedBy());

        OperationState operationState = (OperationState) workflow.getStates().get(2);
        assertTrue(operationState.isUsedForCompensation());

    }
}
