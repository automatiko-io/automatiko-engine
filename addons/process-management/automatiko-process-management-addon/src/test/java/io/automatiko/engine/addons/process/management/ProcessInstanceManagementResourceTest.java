
package io.automatiko.engine.addons.process.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.RuntimeDelegate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import io.automatiko.engine.addons.process.management.ProcessInstanceManagementResource;
import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessError;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstances;
import io.automatiko.engine.services.identity.StaticIdentityProvider;
import io.automatiko.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatiko.engine.services.uow.DefaultUnitOfWorkManager;

@ExtendWith(MockitoExtension.class)
public class ProcessInstanceManagementResourceTest {

    public static final String MESSAGE = "message";
    public static final String PROCESS_ID = "test";
    public static final String PROCESS_INSTANCE_ID = "xxxxx";
    public static final String NODE_ID = "abc-def";
    private static RuntimeDelegate runtimeDelegate;
    private ResponseBuilder responseBuilder;

    private Map<String, Process<?>> processes;
    @SuppressWarnings("rawtypes")
    private ProcessInstance processInstance;
    private ProcessError error;
    private Application application;
    private ProcessInstanceManagementResource resource;

    @BeforeAll
    public static void configureEnvironment() {
        runtimeDelegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(runtimeDelegate);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeEach
    public void setup() {

        responseBuilder = mock(ResponseBuilder.class);
        Response response = mock(Response.class);

        when((runtimeDelegate).createResponseBuilder()).thenReturn(responseBuilder);
        lenient().when((responseBuilder).status(any(StatusType.class))).thenReturn(responseBuilder);
        lenient().when((responseBuilder).entity(any())).thenReturn(responseBuilder);
        lenient().when((responseBuilder).build()).thenReturn(response);

        application = mock(Application.class);
        Map<String, Process<?>> processes = Mockito.mock(Map.class);

        Process process = mock(Process.class);
        ProcessInstances instances = mock(ProcessInstances.class);
        processInstance = mock(ProcessInstance.class);
        error = mock(ProcessError.class);

        lenient().when(processes.get(anyString())).thenReturn(process);
        lenient().when(process.instances()).thenReturn(instances);
        lenient().when(instances.findById(anyString())).thenReturn(Optional.of(processInstance));
        lenient().when(processInstance.error()).thenReturn(Optional.of(error));
        lenient().when(processInstance.id()).thenReturn("abc-def");
        lenient().when(processInstance.status()).thenReturn(ProcessInstance.STATE_ACTIVE);
        lenient().when(error.failedNodeId()).thenReturn("xxxxx");
        lenient().when(error.errorMessage()).thenReturn("Test error message");

        lenient().when(application.unitOfWorkManager())
                .thenReturn(new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()));

        IdentitySupplier identitySupplier = new IdentitySupplier() {

            @Override
            public IdentityProvider buildIdentityProvider(String user, List<String> roles) {
                return new StaticIdentityProvider("test");
            }
        };
        resource = spy(new ProcessInstanceManagementResource(processes, application, identitySupplier));
    }

    @Test
    public void testGetErrorInfo() {

        Response response = resource.getInstanceInError("test", "xxxxx", null, Collections.emptyList());
        assertThat(response).isNotNull();

        verify(responseBuilder, times(1)).status((StatusType) Status.OK);
        verify(responseBuilder, times(1)).entity(any());

        verify(processInstance, times(2)).error();
        verify(error, times(0)).retrigger();
        verify(error, times(0)).skip();

        verify(resource).doGetInstanceInError(PROCESS_ID, PROCESS_INSTANCE_ID);
    }

    @Test
    public void testRetriggerErrorInfo() {

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                when(processInstance.status()).thenReturn(ProcessInstance.STATE_ACTIVE);
                return null;
            }
        }).when(error).retrigger();

        Response response = resource.retriggerInstanceInError(PROCESS_ID, PROCESS_INSTANCE_ID, null, Collections.emptyList());
        assertThat(response).isNotNull();

        verify(responseBuilder, times(1)).status((StatusType) Status.OK);
        verify(responseBuilder, times(1)).entity(any());

        verify(processInstance, times(2)).error();
        verify(error, times(1)).retrigger();
        verify(error, times(0)).skip();

        verify(resource).doRetriggerInstanceInError(PROCESS_ID, PROCESS_INSTANCE_ID);
    }

    @Test
    public void testGetWorkItemsInProcessInstance() {
        resource.getWorkItemsInProcessInstance(PROCESS_ID, PROCESS_INSTANCE_ID, null, Collections.emptyList());
        verify(resource).doGetWorkItemsInProcessInstance(PROCESS_ID, PROCESS_INSTANCE_ID);
    }

    @Test
    public void testSkipInstanceInError() {
        resource.skipInstanceInError(PROCESS_ID, PROCESS_INSTANCE_ID, null, Collections.emptyList());
        verify(resource).doSkipInstanceInError(PROCESS_ID, PROCESS_INSTANCE_ID);
    }

    @Test
    public void testTriggerNodeInstanceId() {
        resource.triggerNodeInstanceId(PROCESS_ID, PROCESS_INSTANCE_ID, NODE_ID, null, Collections.emptyList());
        verify(resource).doTriggerNodeInstanceId(PROCESS_ID, PROCESS_INSTANCE_ID, NODE_ID);
    }

    @Test
    public void testRetriggerNodeInstanceId() {
        resource.retriggerNodeInstanceId(PROCESS_ID, PROCESS_INSTANCE_ID, NODE_ID, null, Collections.emptyList());
        verify(resource).doRetriggerNodeInstanceId(PROCESS_ID, PROCESS_INSTANCE_ID, NODE_ID);
    }

    @Test
    public void testCancelNodeInstanceId() {
        resource.cancelNodeInstanceId(PROCESS_ID, PROCESS_INSTANCE_ID, NODE_ID, null, Collections.emptyList());
        verify(resource).doCancelNodeInstanceId(PROCESS_ID, PROCESS_INSTANCE_ID, NODE_ID);
    }

    @Test
    public void testCancelProcessInstanceId() {
        resource.cancelProcessInstanceId(PROCESS_ID, PROCESS_INSTANCE_ID, null, Collections.emptyList());
        verify(resource).doCancelProcessInstanceId(PROCESS_ID, PROCESS_INSTANCE_ID);
    }

    @Test
    public void testBubildOkResponse(@Mock Object body) {
        Response response = resource.buildOkResponse(body);
        assertResponse(body, Status.OK);
    }

    public void assertResponse(Object body, Status status) {
        verify(responseBuilder).status((Response.StatusType) status);
        verify(responseBuilder).entity(body);
    }

    @Test
    public void testBadRequestResponse() {
        Response response = resource.badRequestResponse(MESSAGE);
        assertResponse(MESSAGE, Status.BAD_REQUEST);
    }

    @Test
    public void testNotFoundResponse() {
        Response response = resource.notFoundResponse(MESSAGE);
        assertResponse(MESSAGE, Status.NOT_FOUND);
    }
}
