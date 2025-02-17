
package io.automatiko.engine.workflow.serverless;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.AccessPolicy;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.ArchiveBuilder;
import io.automatiko.engine.api.workflow.ArchivedProcessInstance;
import io.automatiko.engine.api.workflow.EndOfInstanceStrategy;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.ProcessInstances;
import io.automatiko.engine.api.workflow.Signal;
import io.automatiko.engine.api.workflow.Tags;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.api.workflow.workitem.Transition;
import io.automatiko.engine.workflow.AbstractProcess;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatiko.engine.workflow.process.instance.node.LambdaSubProcessNodeInstance;

public class ServerlessProcessInstance extends AbstractProcessInstance<ServerlessModel> {

    public ServerlessProcessInstance(AbstractProcess<ServerlessModel> process, ServerlessModel variables, ProcessRuntime rt) {
        super(process, variables, rt);
    }

    public ServerlessProcessInstance(AbstractProcess<ServerlessModel> process, ServerlessModel variables, String businessKey,
            ProcessRuntime rt) {
        super(process, variables, businessKey, rt);
    }

    public ServerlessProcessInstance(AbstractProcess<ServerlessModel> process, ServerlessModel variables, ProcessRuntime rt,
            WorkflowProcessInstance wpi, long versionTrack) {
        super(process, variables, rt, wpi, versionTrack);
    }

    public ServerlessProcessInstance(AbstractProcess<ServerlessModel> process, ServerlessModel variables,
            WorkflowProcessInstance wpi) {
        super(process, variables, wpi);
    }

    @Override
    protected Map<String, Object> bind(ServerlessModel variables) {

        if (variables == null) {
            return null;
        }
        return variables.toMap();
    }

    @Override
    protected void unbind(ServerlessModel variables, Map<String, Object> vmap) {

        if (variables == null || vmap == null) {
            return;
        }
        variables.fromMap(vmap);
    }

    @Override
    protected void configureLock(String businessKey) {
        // do nothing on raw bpmn process instance - meaning it disables locking on process instance level
    }

    @Override
    public Collection<ProcessInstance<? extends Model>> subprocesses() {
        Collection<ProcessInstance<? extends Model>> subprocesses = ((WorkflowProcessInstanceImpl) processInstance())
                .getNodeInstances(true)
                .stream()
                .filter(ni -> ni instanceof LambdaSubProcessNodeInstance)
                .map(ni -> new SubProcessInstanceMock<ServerlessModel>(
                        ((LambdaSubProcessNodeInstance) ni).getProcessInstanceId(),
                        ((SubProcessNode) ni.getNode()).getProcessId(),
                        ((SubProcessNode) ni.getNode()).getProcessName(),
                        ((LambdaSubProcessNodeInstance) ni).getTriggerTime()))
                .collect(Collectors.toList());

        return subprocesses;
    }

    private static class SubProcessInstanceMock<ServerlessModel> implements ProcessInstance<ServerlessModel> {
        private String instanceId;
        private String processId;
        private String description;
        private Date startDate;

        public SubProcessInstanceMock(String instanceId, String processId, String description, Date startDate) {
            this.instanceId = instanceId;
            this.processId = processId;
            this.description = description == null ? "" : description;
            this.startDate = startDate;
        }

        @Override
        public Process process() {

            return new Process<ServerlessModel>() {

                @Override
                public ProcessInstance<ServerlessModel> createInstance(ServerlessModel workingMemory) {
                    return null;
                }

                @Override
                public ProcessInstance<ServerlessModel> createInstance(String businessKey, ServerlessModel workingMemory) {

                    return null;
                }

                @Override
                public ProcessInstances<ServerlessModel> instances() {

                    return null;
                }

                @Override
                public <S> void send(Signal<S> sig) {

                }

                @Override
                public ProcessInstance<? extends Model> createInstance(Model m) {
                    return null;
                }

                @Override
                public ProcessInstance<? extends Model> createInstance(String businessKey, Model m) {
                    return null;
                }

                @Override
                public String id() {
                    return processId;
                }

                @Override
                public String name() {
                    return null;
                }

                @Override
                public String description() {
                    return null;
                }

                @Override
                public String version() {
                    return null;
                }

                @Override
                public void activate() {

                }

                @Override
                public void deactivate() {

                }

                @Override
                public ServerlessModel createModel() {
                    return null;
                }

                @Override
                public AccessPolicy<? extends ProcessInstance<ServerlessModel>> accessPolicy() {
                    return null;
                }

                @Override
                public ExportedProcessInstance exportInstance(String id, boolean abort) {
                    return null;
                }

                @Override
                public ProcessInstance<ServerlessModel> importInstance(ExportedProcessInstance instance) {
                    return null;
                }

                @Override
                public ArchivedProcessInstance archiveInstance(String id, ArchiveBuilder builder) {
                    return null;
                }

                @Override
                public EndOfInstanceStrategy endOfInstanceStrategy() {
                    return null;
                }

            };
        }

        @Override
        public void start() {
        }

        @Override
        public void start(String trigger, String referenceId, Object data) {
        }

        @Override
        public void startFrom(String nodeId) {
        }

        @Override
        public void startFrom(String nodeId, String referenceId) {

        }

        @Override
        public void send(Signal signal) {

        }

        @Override
        public void abort() {

        }

        @Override
        public ServerlessModel variables() {
            return null;
        }

        @Override
        public void updateVariables(Object updates) {

        }

        @Override
        public int status() {
            return 1;
        }

        @Override
        public Collection subprocesses() {
            return null;
        }

        @Override
        public void completeWorkItem(String id, Map variables, Policy... policies) {

        }

        @Override
        public void abortWorkItem(String id, Policy... policies) {

        }

        @Override
        public void failWorkItem(String id, Throwable error) {

        }

        @Override
        public void transitionWorkItem(String id, Transition transition) {

        }

        @Override
        public WorkItem workItem(String workItemId, Policy... policies) {
            return null;
        }

        @Override
        public List workItems(Policy... policies) {
            return null;
        }

        @Override
        public String id() {
            return instanceId;
        }

        @Override
        public String businessKey() {
            return null;
        }

        @Override
        public String description() {

            return description;
        }

        @Override
        public String parentProcessInstanceId() {
            return null;
        }

        @Override
        public String rootProcessInstanceId() {
            return null;
        }

        @Override
        public String rootProcessId() {
            return null;
        }

        @Override
        public Date startDate() {
            return startDate;
        }

        @Override
        public Date endDate() {
            return null;
        }

        @Override
        public Date expiresAtDate() {
            return null;
        }

        @Override
        public Optional errors() {
            return null;
        }

        @Override
        public void triggerNode(String nodeId) {

        }

        @Override
        public void cancelNodeInstance(String nodeInstanceId) {

        }

        @Override
        public void retriggerNodeInstance(String nodeInstanceId) {

        }

        @Override
        public Set events() {
            return null;
        }

        @Override
        public Collection milestones() {
            return null;
        }

        @Override
        public Collection adHocFragments() {
            return null;
        }

        @Override
        public void disconnect() {
        }

        @Override
        public Tags tags() {
            return null;
        }

        @Override
        public Optional<String> initiator() {
            return null;
        }

        @Override
        public String image(String path) {
            return null;
        }

        @Override
        public ArchivedProcessInstance archive(ArchiveBuilder builder) {

            return null;
        }

        @Override
        public Collection<ProcessInstance<? extends Model>> subprocesses(ProcessInstanceReadMode mode) {

            return null;
        }

        @Override
        public String abortCode() {
            return null;
        }

        @Override
        public Object abortData() {
            return null;
        }

        @Override
        public boolean isConnected() {
            return false;
        }
    }
}
