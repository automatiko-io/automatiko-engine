
package io.automatiko.engine.addons.process.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessError;
import io.automatiko.engine.api.workflow.ProcessErrors;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstances;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatiko.engine.services.uow.DefaultUnitOfWorkManager;

@ExtendWith(MockitoExtension.class)
class BaseProcessInstanceManagementResourceTest {

    public static final String PROCESS_ID = "processId";
    public static final String PROCESS_INSTANCE_ID = "processInstanceId";
    public static final String NODE_ID_ERROR = "processInstanceIdError";
    public static final String NODE_ID = "nodeId";
    public static final String NODE_INSTANCE_ID = "nodeInstanceId";
    private BaseProcessInstanceManagementResource tested;

    @Mock
    private Application application;

    @Mock
    private ProcessInstance processInstance;

    @Mock
    private ProcessErrors errors;

    @Mock
    private ProcessError error;

    @Mock
    private ProcessInstances instances;

    @Mock
    private Process process;

    @Mock
    private Object variables;

    @BeforeEach
    void setUp() {
        Map<String, Process<?>> processes = Mockito.mock(Map.class);
        lenient().when(processes.get(anyString())).thenReturn(process);

        when(process.instances()).thenReturn(instances);
        lenient().when(instances.findById(anyString())).thenReturn(Optional.of(processInstance));
        lenient().when(processInstance.errors()).thenReturn(Optional.of(errors));
        lenient().when(processInstance.variables()).thenReturn(variables);
        lenient().when(processInstance.id()).thenReturn(PROCESS_INSTANCE_ID);
        lenient().when(processInstance.status()).thenReturn(ProcessInstance.STATE_ERROR);
        lenient().when(errors.failedNodeIds()).thenReturn(NODE_ID_ERROR);
        lenient().when(errors.errorMessages()).thenReturn("Test error message");
        lenient().when(application.unitOfWorkManager())
                .thenReturn(new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()));

        tested = spy(new BaseProcessInstanceManagementResource(processes, application) {

            @Override
            protected Object buildOkResponse(Object body) {
                return body;
            }

            @Override
            protected Object badRequestResponse(String message) {
                return message;
            }

            @Override
            protected Object notFoundResponse(String message) {
                return message;
            }

            @Override
            public Object getInstanceInError(String processId, String processInstanceId, String user, List groups) {
                return null;
            }

            @Override
            public Object getWorkItemsInProcessInstance(String processId, String processInstanceId, String user, List groups) {
                return null;
            }

            @Override
            public Object retriggerInstanceInError(String processId, String processInstanceId, String user, List groups) {
                return null;
            }

            @Override
            public Object skipInstanceInError(String processId, String processInstanceId, String user, List groups) {
                return null;
            }

            @Override
            public Object triggerNodeInstanceId(String processId, String processInstanceId, String nodeId, String user,
                    List groups) {
                return null;
            }

            @Override
            public Object retriggerNodeInstanceId(String processId, String processInstanceId, String nodeInstanceId,
                    String user, List groups) {
                return null;
            }

            @Override
            public Object cancelNodeInstanceId(String processId, String processInstanceId, String nodeInstanceId, String user,
                    List groups) {
                return null;
            }

            @Override
            public Object cancelProcessInstanceId(String processId, String processInstanceId, String user, List groups) {
                return null;
            }

            @Override
            public Object retriggerInstanceInErrorByErrorId(String processId, String processInstanceId, String errord,
                    String user,
                    List groups) {
                return null;
            }

            @Override
            public Object skipInstanceInErrorByErrorId(String processId, String processInstanceId, String errorId, String user,
                    List groups) {
                return null;
            }
        });
    }

    @Test
    void testDoGetInstanceInError() {
        lenient().when(instances.findById(anyString(), eq(5), any())).thenReturn(Optional.of(processInstance));
        lenient().when(error.failedNodeId()).thenReturn(NODE_ID_ERROR);
        lenient().when(error.errorMessage()).thenReturn("Test error message");
        lenient().when(processInstance.errors()).thenReturn(Optional.of(new ProcessErrors(Arrays.asList(error))));
        Object response = tested.doGetInstanceInError(PROCESS_ID, PROCESS_INSTANCE_ID);
        verify(processInstance, times(2)).errors();
        verify(errors, times(0)).retrigger();
        verify(errors, times(0)).skip();
        verify(tested).buildOkResponse(any());
        assertThat(response).isInstanceOf(List.class);
        Map responseMap = (Map) ((List) response).get(0);
        assertThat(responseMap.get("id")).isEqualTo(PROCESS_INSTANCE_ID);
        assertThat(responseMap.get("failedNodeId")).isEqualTo(NODE_ID_ERROR);
    }

    @Test
    void testDoGetWorkItemsInProcessInstance(@Mock WorkItem workItem) {
        when(processInstance.workItems(any(SecurityPolicy.class))).thenReturn(Collections.singletonList(workItem));
        Object response = tested.doGetWorkItemsInProcessInstance(PROCESS_ID, PROCESS_INSTANCE_ID);
        assertThat(response).isInstanceOf(List.class);
        assertThat(((List) response).get(0)).isEqualTo(workItem);
    }

    @Test
    void testDoRetriggerInstanceInError() {
        lenient().when(instances.findById(anyString(), eq(5), any())).thenReturn(Optional.of(processInstance));
        mockProcessInstanceStatusActiveOnError().retrigger();
        Object response = tested.doRetriggerInstanceInError(PROCESS_ID, PROCESS_INSTANCE_ID);
        verify(processInstance, times(2)).errors();
        verify(errors, times(1)).retrigger();
        verify(errors, times(0)).skip();
        assertResultOk(response);
    }

    @Test
    void testDoSkipInstanceInError() {
        lenient().when(instances.findById(anyString(), eq(5), any())).thenReturn(Optional.of(processInstance));
        mockProcessInstanceStatusActiveOnError().skip();
        Object response = tested.doSkipInstanceInError(PROCESS_ID, PROCESS_INSTANCE_ID);
        verify(processInstance, times(2)).errors();
        verify(errors, times(0)).retrigger();
        verify(errors, times(1)).skip();
        assertResultOk(response);
    }

    @Test
    void testDoTriggerNodeInstanceId() {
        mockProcessInstanceStatusActive().triggerNode(NODE_ID);
        Object response = tested.doTriggerNodeInstanceId(PROCESS_ID, PROCESS_INSTANCE_ID, NODE_ID);
        verify(processInstance, times(0)).errors();
        verify(processInstance, times(1)).triggerNode(NODE_ID);
        assertResultOk(response);
    }

    @Test
    void testDoRetriggerNodeInstanceId() {
        mockProcessInstanceStatusActive().retriggerNodeInstance(NODE_INSTANCE_ID);
        Object response = tested.doRetriggerNodeInstanceId(PROCESS_ID, PROCESS_INSTANCE_ID, NODE_INSTANCE_ID);
        verify(processInstance, times(0)).errors();
        verify(processInstance, times(1)).retriggerNodeInstance(NODE_INSTANCE_ID);
        assertResultOk(response);
    }

    @Test
    void testDoCancelNodeInstanceId() {
        mockProcessInstanceStatusActive().cancelNodeInstance(anyString());
        Object response = tested.doCancelNodeInstanceId(PROCESS_ID, PROCESS_INSTANCE_ID, NODE_INSTANCE_ID);
        verify(processInstance, times(0)).errors();
        verify(processInstance, times(1)).cancelNodeInstance(NODE_INSTANCE_ID);
        assertResultOk(response);
    }

    private void assertResultOk(Object response) {
        verify(tested).buildOkResponse(any());
        assertThat(response).isEqualTo(variables);
    }

    private ProcessInstance mockProcessInstanceStatusActive() {
        return doAnswer((v) -> {
            when(processInstance.status()).thenReturn(ProcessInstance.STATE_ACTIVE);
            return null;
        }).when(processInstance);
    }

    private ProcessErrors mockProcessInstanceStatusActiveOnError() {
        return doAnswer((v) -> {
            when(processInstance.status()).thenReturn(ProcessInstance.STATE_ACTIVE);
            return null;
        }).when(errors);
    }

    @Test
    void testDoCancelProcessInstanceId() {
        mockProcessInstanceStatusActive().abort();
        Object response = tested.doCancelProcessInstanceId(PROCESS_ID, PROCESS_INSTANCE_ID);
        verify(processInstance, times(0)).errors();
        verify(processInstance, times(1)).abort();
        assertResultOk(response);
    }
}