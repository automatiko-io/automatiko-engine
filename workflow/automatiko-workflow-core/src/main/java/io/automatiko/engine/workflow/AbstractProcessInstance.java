
package io.automatiko.engine.workflow;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.AccessDeniedException;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.api.runtime.process.HumanTaskWorkItem;
import io.automatiko.engine.api.runtime.process.NodeInstanceState;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;
import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.ArchiveBuilder;
import io.automatiko.engine.api.workflow.ArchivedProcessInstance;
import io.automatiko.engine.api.workflow.EventDescription;
import io.automatiko.engine.api.workflow.ExecutionsErrorInfo;
import io.automatiko.engine.api.workflow.MutableProcessInstances;
import io.automatiko.engine.api.workflow.NodeInstanceNotFoundException;
import io.automatiko.engine.api.workflow.NodeNotFoundException;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessError;
import io.automatiko.engine.api.workflow.ProcessErrors;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatiko.engine.api.workflow.ProcessInstanceNotFoundException;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.Signal;
import io.automatiko.engine.api.workflow.Tag;
import io.automatiko.engine.api.workflow.Tags;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.api.workflow.flexible.AdHocFragment;
import io.automatiko.engine.api.workflow.flexible.Milestone;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.api.workflow.workitem.Transition;
import io.automatiko.engine.services.correlation.CorrelationAwareProcessRuntime;
import io.automatiko.engine.services.correlation.CorrelationKey;
import io.automatiko.engine.services.correlation.StringCorrelationKey;
import io.automatiko.engine.services.uow.ProcessInstanceWorkUnit;
import io.automatiko.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.lock.UnlockWorkUnit;
import io.automatiko.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatiko.engine.workflow.process.instance.NodeInstance;
import io.automatiko.engine.workflow.process.instance.NodeInstanceContainer;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceImpl;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatiko.engine.workflow.process.instance.node.CompositeContextNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.EventSubProcessNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.HumanTaskNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.LambdaSubProcessNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.WorkItemNodeInstance;
import io.automatiko.engine.workflow.util.InstanceTuple;

public abstract class AbstractProcessInstance<T extends Model> implements ProcessInstance<T> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcessInstance.class);

    protected final T variables;
    protected final AbstractProcess<T> process;
    protected final ProcessRuntime rt;
    protected io.automatiko.engine.api.runtime.process.ProcessInstance processInstance;

    protected Integer status = 0;
    protected String id;
    protected CorrelationKey correlationKey;
    protected String description;
    protected String parentProcessInstanceId;
    protected String rootProcessInstanceId;
    protected String rootProcessId;

    protected String initiator;

    protected String abortCode;
    protected Object abortData;

    protected ProcessErrors processErrors;

    protected Tags tags;

    protected Supplier<io.automatiko.engine.api.runtime.process.ProcessInstance> reloadSupplier;

    protected CompletionEventListener completionEventListener;

    protected ReentrantLock lock;

    protected long versionTracker;

    protected Set<String> visibleTo;

    public AbstractProcessInstance(AbstractProcess<T> process, T variables, ProcessRuntime rt) {
        this(process, variables, null, rt);
    }

    public AbstractProcessInstance(AbstractProcess<T> process, T variables, String businessKey, ProcessRuntime rt) {
        this.process = process;
        this.rt = rt;
        this.variables = variables;
        configureLock(businessKey);
        lock();

        if (!this.process.accessPolicy().canCreateInstance(IdentityProvider.get())) {
            unlock(true);
            throw new AccessDeniedException("Access is denied to create new instance of process " + process.name());
        }
        setCorrelationKey(businessKey);
        Map<String, Object> map = bind(variables);
        String processId = process.process().getId();
        syncProcessInstance((WorkflowProcessInstance) ((CorrelationAwareProcessRuntime) rt)
                .createProcessInstance(processId, correlationKey, map));
        ((io.automatiko.engine.workflow.base.instance.ProcessInstance) this.processInstance)
                .setInitiator(IdentityProvider.get().getName());
        // this applies to business keys only as non business keys process instances id
        // are always unique
        if (correlationKey != null && ((MutableProcessInstances<T>) process.instances()).exists(id)) {
            unlock(true);
            throw new ProcessInstanceDuplicatedException(correlationKey.getName());
        }
        // add to the instances upon creation so it can be immediately found even if not
        // started
        this.versionTracker = 1;
        ((MutableProcessInstances<T>) process.instances()).create(id, this);

        unbind(variables, processInstance.getVariables());
    }

    /**
     * Without providing a ProcessRuntime the ProcessInstance can only be used as
     * read-only
     * 
     * @param process
     * @param variables
     * @param wpi
     */
    public AbstractProcessInstance(AbstractProcess<T> process, T variables, WorkflowProcessInstance wpi) {
        this.process = process;
        this.variables = variables;
        this.rt = null;

        syncProcessInstance((WorkflowProcessInstance) wpi);
        unbind(variables, processInstance.getVariables());

        if (!this.process.accessPolicy().canReadInstance(IdentityProvider.get(), this)) {
            throw new AccessDeniedException("Access is denied to access instance " + this.id);
        }
    }

    /**
     * With provided process runtime and version tracker process instance is mutable meaning it will be persisted
     * to the configured data store at the end of unit of work
     * 
     * @param process
     * @param variables
     * @param rt
     * @param wpi
     * @param version
     */
    public AbstractProcessInstance(AbstractProcess<T> process, T variables, ProcessRuntime rt,
            WorkflowProcessInstance wpi, long version) {
        this.process = process;
        this.rt = rt;
        this.variables = variables;
        this.versionTracker = version;
        configureLock(wpi.getCorrelationKey());
        lock();

        syncProcessInstance((WorkflowProcessInstance) wpi);
        unbind(variables, processInstance.getVariables());

        if (!this.process.accessPolicy().canReadInstance(IdentityProvider.get(), this)) {
            unlock(true);
            throw new AccessDeniedException("Access is denied to access instance " + this.id);
        }
        reconnect();
    }

    protected void reconnect() {
        if (((WorkflowProcessInstanceImpl) processInstance).getProcessRuntime() == null) {
            ((WorkflowProcessInstanceImpl) processInstance)
                    .setProcessRuntime(((InternalProcessRuntime) getProcessRuntime()));
        }
        ((WorkflowProcessInstanceImpl) processInstance).reconnect();
        ((WorkflowProcessInstanceImpl) processInstance).setMetaData("AutomatikProcessInstance", this);
        addCompletionEventListener();

        for (io.automatiko.engine.api.runtime.process.NodeInstance nodeInstance : ((WorkflowProcessInstanceImpl) processInstance)
                .getNodeInstances(true)) {
            if (nodeInstance instanceof WorkItemNodeInstance) {
                ((WorkItemNodeInstance) nodeInstance).internalRegisterWorkItem();
            }
        }

        unbind(variables, processInstance.getVariables());
    }

    private void addCompletionEventListener() {
        if (completionEventListener == null) {
            completionEventListener = new CompletionEventListener();
            ((WorkflowProcessInstanceImpl) processInstance).addEventListener("processInstanceCompleted:" + id,
                    completionEventListener, false);
        }
    }

    private void removeCompletionListener() {
        if (completionEventListener != null) {
            ((WorkflowProcessInstanceImpl) processInstance).removeEventListener("processInstanceCompleted:" + id,
                    completionEventListener, false);
            completionEventListener = null;
        }
    }

    public void disconnect() {
        if (processInstance == null) {
            LOGGER.debug("Already disconnected instance {} ({}) on thread {} lock {}", id(), businessKey(),
                    Thread.currentThread().getName(), lock);
            return;
        }
        lock();
        LOGGER.debug("Disconnecting instance {} ({}) on thread {} lock {}", id(), businessKey(),
                Thread.currentThread().getName(), lock);
        ((WorkflowProcessInstanceImpl) processInstance).disconnect();
        LOGGER.debug("Disconnected instance {} ({}) on thread {} lock {}", id(), businessKey(),
                Thread.currentThread().getName(), lock);
    }

    private void syncProcessInstance(WorkflowProcessInstance wpi) {
        processInstance = wpi;
        status = wpi.getState();
        id = wpi.getId();
        description = ((WorkflowProcessInstanceImpl) wpi).getDescription();
        parentProcessInstanceId = "".equals(wpi.getParentProcessInstanceId()) ? null : wpi.getParentProcessInstanceId();
        rootProcessInstanceId = "".equals(wpi.getRootProcessInstanceId()) ? null : wpi.getRootProcessInstanceId();
        rootProcessId = "".equals(wpi.getRootProcessId()) ? null : wpi.getRootProcessId();
        initiator = "".equals(((WorkflowProcessInstanceImpl) wpi).getInitiator()) ? null
                : ((WorkflowProcessInstanceImpl) wpi).getInitiator();
        tags = buildTags();
        visibleTo = setVisibleTo();

        setCorrelationKey(wpi.getCorrelationKey());

        if (status == ProcessInstance.STATE_ABORTED) {
            this.abortCode = ((WorkflowProcessInstanceImpl) wpi).getOutcome();
            this.abortData = ((WorkflowProcessInstanceImpl) wpi).getFaultData();
        }

    }

    public void sync(WorkflowProcessInstance wpi) {
        description = ((WorkflowProcessInstanceImpl) wpi).getDescription();
        parentProcessInstanceId = "".equals(wpi.getParentProcessInstanceId()) ? null : wpi.getParentProcessInstanceId();
        rootProcessInstanceId = "".equals(wpi.getRootProcessInstanceId()) ? null : wpi.getRootProcessInstanceId();
        rootProcessId = "".equals(wpi.getRootProcessId()) ? null : wpi.getRootProcessId();
        initiator = "".equals(((WorkflowProcessInstanceImpl) wpi).getInitiator()) ? null
                : ((WorkflowProcessInstanceImpl) wpi).getInitiator();
    }

    private void setCorrelationKey(String businessKey) {
        if (businessKey != null && !businessKey.trim().isEmpty()) {
            correlationKey = new StringCorrelationKey(businessKey);
        }
    }

    public io.automatiko.engine.api.runtime.process.ProcessInstance internalGetProcessInstance() {
        return processInstance;
    }

    public void internalRemoveProcessInstance(
            Supplier<io.automatiko.engine.api.runtime.process.ProcessInstance> reloadSupplier) {
        if (processInstance == null) {
            return;
        }

        this.reloadSupplier = reloadSupplier;
        this.status = processInstance.getState();
        if (this.status == STATE_ERROR) {
            this.processErrors = buildProcessErrors();
        }
        removeCompletionListener();

        disconnect();

        processInstance = null;
    }

    public void start() {
        start(null, null, null);
    }

    public void start(String trigger, String referenceId, Object data) {
        lock();
        if (this.status != ProcessInstance.STATE_PENDING) {
            throw new IllegalStateException("Impossible to start process instance that already was started");
        }
        this.status = ProcessInstance.STATE_ACTIVE;

        if (referenceId != null) {
            ((WorkflowProcessInstanceImpl) processInstance).setReferenceId(referenceId);
        }

        ((InternalProcessRuntime) getProcessRuntime()).getProcessInstanceManager()
                .addProcessInstance(this.processInstance, this.correlationKey);
        this.id = processInstance.getId();
        ((WorkflowProcessInstanceImpl) processInstance).setMetaData("AutomatikProcessInstance", this);
        addCompletionEventListener();
        io.automatiko.engine.api.runtime.process.ProcessInstance processInstance = this.getProcessRuntime()
                .startProcessInstance(this.id, trigger, data);
        syncProcessInstance((WorkflowProcessInstance) processInstance);
        addToUnitOfWork(pi -> ((MutableProcessInstances<T>) process.instances()).create(pi.id(), pi),
                pi -> ((MutableProcessInstances<T>) process.instances()).release(pi.id(), pi));

        unbind(variables, processInstance.getVariables());
        if (this.processInstance != null) {
            this.status = this.processInstance.getState();
        }
        unlock(false);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void addToUnitOfWork(Consumer<ProcessInstance<T>> action) {
        ((InternalProcessRuntime) getProcessRuntime()).getUnitOfWorkManager().currentUnitOfWork()
                .intercept(new ProcessInstanceWorkUnit(this, action));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void addToUnitOfWork(Consumer<ProcessInstance<T>> action, Consumer<ProcessInstance<T>> aborAction) {
        ((InternalProcessRuntime) getProcessRuntime()).getUnitOfWorkManager().currentUnitOfWork()
                .intercept(new ProcessInstanceWorkUnit(this, action, aborAction));
    }

    public void abort() {
        lock();
        if (!this.process.accessPolicy().canDeleteInstance(IdentityProvider.get(), this)) {
            unlock(true);
            throw new AccessDeniedException("Access is denied to delete instance " + this.id);
        }
        String pid = processInstance().getId();
        unbind(variables, processInstance().getVariables());
        this.getProcessRuntime().abortProcessInstance(pid);
        this.status = processInstance.getState();
        this.visibleTo = setVisibleTo();
        // apply end of instance strategy on completion
        process.endOfInstanceStrategy().perform(this);
        if (process.endOfInstanceStrategy().shouldInstanceBeUpdated()) {
            addToUnitOfWork(pi -> ((MutableProcessInstances<T>) process.instances()).update(pi.id(), pi),
                    pi -> ((MutableProcessInstances<T>) process.instances()).release(pi.id(), pi));
        }
        if (process.endOfInstanceStrategy().shouldInstanceBeRemoved()) {
            addToUnitOfWork(pi -> ((MutableProcessInstances<T>) process.instances()).remove(pi.id(), pi),
                    pi -> ((MutableProcessInstances<T>) process.instances()).release(pi.id(), pi));
        }
        unlock(true);

    }

    @Override
    public <S> void send(Signal<S> signal) {
        lock();
        if (!this.process.accessPolicy().canSignalInstance(IdentityProvider.get(), this)) {
            unlock(true);
            throw new AccessDeniedException("Access is denied to signal instance " + this.id);
        }

        if (signal.referenceId() != null) {
            ((WorkflowProcessInstanceImpl) processInstance()).setReferenceId(signal.referenceId());
        }
        processInstance().signalEvent(signal.channel(), signal.payload());
        removeOnFinish();

    }

    @Override
    public Collection<ProcessInstance<? extends Model>> subprocesses() {
        return subprocesses(ProcessInstanceReadMode.READ_ONLY);
    }

    @Override
    public Collection<ProcessInstance<? extends Model>> subprocesses(ProcessInstanceReadMode mode) {
        return Collections.emptyList();
    }

    @Override
    public Process<T> process() {
        return process;
    }

    @Override
    public T variables() {
        return variables;
    }

    @Override
    public int status() {
        return this.status;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public String businessKey() {
        return this.correlationKey == null ? null : this.correlationKey.getName();
    }

    @Override
    public String description() {
        return this.description;
    }

    @Override
    public String parentProcessInstanceId() {
        return this.parentProcessInstanceId;
    }

    @Override
    public String rootProcessInstanceId() {
        return this.rootProcessInstanceId;
    }

    @Override
    public String rootProcessId() {
        return this.rootProcessId;
    }

    @Override
    public Date startDate() {
        return this.processInstance != null && processInstance instanceof WorkflowProcessInstanceImpl
                ? ((WorkflowProcessInstanceImpl) this.processInstance).getStartDate()
                : null;
    }

    @Override
    public Date endDate() {
        return this.processInstance != null && processInstance instanceof WorkflowProcessInstanceImpl
                ? ((WorkflowProcessInstanceImpl) this.processInstance).getEndDate()
                : null;
    }

    @Override
    public Date expiresAtDate() {
        Date endDate = endDate();
        String expiredAt = (String) process.process().getMetaData().get("expiresAfter");
        if (endDate != null && expiredAt != null) {
            long expiresInMillis = DateTimeUtils.parseDuration(expiredAt);

            return new Date(endDate.getTime() + expiresInMillis);
        }

        return null;
    }

    @Override
    public Optional<String> initiator() {

        return Optional.ofNullable(initiator);
    }

    @Override
    public String abortCode() {
        return abortCode;
    }

    @Override
    public Object abortData() {
        return abortData;
    }

    @Override
    public void updateVariables(T updates) {
        lock();
        if (!this.process.accessPolicy().canUpdateInstance(IdentityProvider.get(), this)) {
            unlock(true);
            throw new AccessDeniedException("Access is denied to update instance " + this.id);
        }
        try {
            processInstance();
            Map<String, Object> map = bind(updates);

            for (Entry<String, Object> entry : map.entrySet()) {
                ((WorkflowProcessInstance) processInstance()).setVariable(entry.getKey(), entry.getValue());
            }
            syncProcessInstance(((WorkflowProcessInstance) processInstance()));
            unbind(this.variables, processInstance().getVariables());
            addToUnitOfWork(pi -> {
                synchronized (this) {
                    ((MutableProcessInstances<T>) process.instances()).update(pi.id(), pi);
                }
            }, pi -> ((MutableProcessInstances<T>) process.instances()).release(pi.id(), pi));
            unlock(false);
            removeOnFinish();
        } catch (Throwable e) {
            ((MutableProcessInstances<T>) process.instances()).release(id(), this);
            throw e;
        }
    }

    @Override
    public Optional<ProcessErrors> errors() {
        if (this.status == STATE_ERROR) {
            return Optional.of(this.processErrors != null ? this.processErrors : buildProcessErrors());
        }

        return Optional.empty();
    }

    @Override
    public Tags tags() {

        return this.tags != null ? this.tags : buildTags();
    }

    @Override
    public void startFrom(String nodeId) {
        startFrom(nodeId, null);
    }

    @Override
    public void startFrom(String nodeId, String referenceId) {
        lock();
        ((WorkflowProcessInstanceImpl) processInstance).setStartDate(new Date());
        ((WorkflowProcessInstanceImpl) processInstance).setState(STATE_ACTIVE);
        ((InternalProcessRuntime) getProcessRuntime()).getProcessInstanceManager()
                .addProcessInstance(this.processInstance, this.correlationKey);
        this.id = processInstance.getId();
        addCompletionEventListener();
        if (referenceId != null) {
            ((WorkflowProcessInstanceImpl) processInstance).setReferenceId(referenceId);
        }
        ((WorkflowProcessInstanceImpl) processInstance).setMetaData("AutomatikProcessInstance", this);
        triggerNode(nodeId);
        syncProcessInstance((WorkflowProcessInstance) processInstance);
        addToUnitOfWork(pi -> ((MutableProcessInstances<T>) process.instances()).update(pi.id(), pi),
                pi -> ((MutableProcessInstances<T>) process.instances()).release(pi.id(), pi));
        unlock(false);
        unbind(variables, processInstance.getVariables());
        if (processInstance != null) {
            this.status = processInstance.getState();
        }

    }

    @Override
    public void triggerNode(String nodeId) {
        lock();
        WorkflowProcessInstanceImpl wfpi = ((WorkflowProcessInstanceImpl) processInstance());
        ExecutableProcess rfp = ((ExecutableProcess) wfpi.getProcess());

        Node node = rfp.getNodesRecursively().stream().filter(ni -> nodeId.equals(ni.getMetaData().get("UniqueId")))
                .findFirst().orElseThrow(() -> new NodeNotFoundException(this.id, nodeId));

        Node parentNode = rfp.getParentNode(node.getId());

        NodeInstanceContainer nodeInstanceContainerNode = parentNode == null ? wfpi
                : ((NodeInstanceContainer) wfpi.getNodeInstance(parentNode));

        if (nodeInstanceContainerNode.getNodeInstances().isEmpty()
                && nodeInstanceContainerNode instanceof CompositeContextNodeInstance) {

            ((CompositeContextNodeInstance) nodeInstanceContainerNode).internalTriggerOnlyParent(null, nodeId);
        }

        nodeInstanceContainerNode.getNodeInstance(node).trigger(null,
                io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);

    }

    @Override
    public void cancelNodeInstance(String nodeInstanceId) {
        lock();
        NodeInstance nodeInstance = ((WorkflowProcessInstanceImpl) processInstance()).getNodeInstances(true)
                .stream().filter(ni -> ni.getId().equals(nodeInstanceId)).findFirst()
                .orElseThrow(() -> new NodeInstanceNotFoundException(this.id, nodeInstanceId));

        nodeInstance.cancel();
        removeOnFinish();

    }

    @Override
    public void retriggerNodeInstance(String nodeInstanceId) {
        lock();
        NodeInstance nodeInstance = ((WorkflowProcessInstanceImpl) processInstance()).getNodeInstances(true)
                .stream().filter(ni -> ni.getId().equals(nodeInstanceId)).findFirst()
                .orElseThrow(() -> new NodeInstanceNotFoundException(this.id, nodeInstanceId));

        ((NodeInstanceImpl) nodeInstance).retrigger(true);
        removeOnFinish();

    }

    public io.automatiko.engine.api.runtime.process.ProcessInstance processInstance() {
        if (this.processInstance == null) {
            this.processInstance = reloadSupplier.get();
            if (this.processInstance == null) {
                throw new ProcessInstanceNotFoundException(id);
            } else if (getProcessRuntime() != null) {
                reconnect();
            }
        }
        if (this.rt != null && ((WorkflowProcessInstanceImpl) processInstance).getProcessRuntime() == null) {
            ((WorkflowProcessInstanceImpl) processInstance).setProcessRuntime((InternalProcessRuntime) getProcessRuntime());
            reconnect();
        }
        return this.processInstance;
    }

    @Override
    public WorkItem workItem(String workItemId, Policy<?>... policies) {
        WorkItemNodeInstance workItemInstance = (WorkItemNodeInstance) ((WorkflowProcessInstanceImpl) processInstance())
                .getNodeInstances(true).stream()
                .filter(ni -> ni instanceof WorkItemNodeInstance
                        && ((WorkItemNodeInstance) ni).getWorkItemId().equals(workItemId)
                        && ((WorkItemNodeInstance) ni).getWorkItem().enforce(policies))
                .findFirst().orElseThrow(() -> new WorkItemNotFoundException(
                        "Work item with id " + workItemId + " was not found in process instance " + id(), workItemId));
        return new BaseWorkItem(workItemInstance.getProcessInstance().getId(), workItemInstance.getId(),
                workItemInstance.getWorkItem().getId(), workItemInstance.buildReferenceId(),
                (String) workItemInstance.getWorkItem().getParameters().getOrDefault("TaskName",
                        workItemInstance.getNodeName()),
                (String) workItemInstance.getWorkItem().getParameters().getOrDefault("Description", ""),
                workItemInstance.getWorkItem().getState(), workItemInstance.getWorkItem().getPhaseId(),
                workItemInstance.getWorkItem().getPhaseStatus(), workItemInstance.getWorkItem().getParameters(),
                workItemInstance.getWorkItem().getResults(),
                workItemInstance.buildFormLink());
    }

    @Override
    public List<WorkItem> workItems(Policy<?>... policies) {
        List<WorkItem> mainProcessInstance = ((WorkflowProcessInstanceImpl) processInstance()).getNodeInstances(true)
                .stream()
                .filter(ni -> ni instanceof WorkItemNodeInstance
                        && ((WorkItemNodeInstance) ni).getWorkItem().enforce(policies))
                .map(ni -> new BaseWorkItem(ni.getProcessInstance().getId(), ni.getId(),
                        ((WorkItemNodeInstance) ni).getWorkItemId(), ((WorkItemNodeInstance) ni).buildReferenceId(),
                        (String) ((WorkItemNodeInstance) ni).getWorkItem().getParameters().getOrDefault("TaskName",
                                ni.getNodeName()),
                        (String) ((WorkItemNodeInstance) ni).getWorkItem().getParameters().getOrDefault("Description", ""),
                        ((WorkItemNodeInstance) ni).getWorkItem().getState(),
                        ((WorkItemNodeInstance) ni).getWorkItem().getPhaseId(),
                        ((WorkItemNodeInstance) ni).getWorkItem().getPhaseStatus(),
                        ((WorkItemNodeInstance) ni).getWorkItem().getParameters(),
                        ((WorkItemNodeInstance) ni).getWorkItem().getResults(),
                        ((WorkItemNodeInstance) ni).buildFormLink()))
                .collect(Collectors.toList());

        subprocesses().forEach(pi -> mainProcessInstance.addAll(pi.workItems(policies)));

        return mainProcessInstance;

    }

    @Override
    public void completeWorkItem(String id, Map<String, Object> variables, Policy<?>... policies) {
        lock();
        try {
            processInstance();
            String[] fragments = id.split("/");

            if (fragments.length > 1) {
                // comes from subprocess
                subprocesses().stream()
                        .filter(pi -> pi.process().id().equals(fragments[0]) && pi.id().equals(fragments[1]))
                        .findFirst().ifPresent(pi -> pi.completeWorkItem(fragments[3], variables, policies));
            } else {

                this.getProcessRuntime().getWorkItemManager().completeWorkItem(id, variables, policies);
                removeOnFinish();
            }
        } catch (Throwable e) {
            ((MutableProcessInstances<T>) process.instances()).release(id(), this);
            throw e;
        }
    }

    @Override
    public void abortWorkItem(String id, Policy<?>... policies) {
        lock();
        try {
            processInstance();
            String[] fragments = id.split("/");

            if (fragments.length > 1) {
                // comes from subprocess
                subprocesses().stream()
                        .filter(pi -> pi.process().id().equals(fragments[0]) && pi.id().equals(fragments[1]))
                        .findFirst().ifPresent(pi -> pi.abortWorkItem(fragments[3], policies));
            } else {

                this.getProcessRuntime().getWorkItemManager().abortWorkItem(id, policies);
                removeOnFinish();
            }
        } catch (Throwable e) {
            ((MutableProcessInstances<T>) process.instances()).release(id(), this);
            throw e;
        }
    }

    @Override
    public void failWorkItem(String id, Throwable error) {
        lock();
        try {
            processInstance();
            String[] fragments = id.split("/");

            if (fragments.length > 1) {
                // comes from subprocess
                subprocesses().stream()
                        .filter(pi -> pi.process().id().equals(fragments[0]) && pi.id().equals(fragments[1]))
                        .findFirst().ifPresent(pi -> pi.failWorkItem(fragments[3], error));
            } else {

                this.getProcessRuntime().getWorkItemManager().failWorkItem(id, error);
                removeOnFinish();
            }
        } catch (Throwable e) {
            ((MutableProcessInstances<T>) process.instances()).release(id(), this);
            throw e;
        }
    }

    @Override
    public void transitionWorkItem(String id, Transition<?> transition) {
        lock();
        try {
            processInstance();
            String[] fragments = id.split("/");

            if (fragments.length > 1) {
                // comes from subprocess
                subprocesses().stream()
                        .filter(pi -> pi.process().id().equals(fragments[0]) && pi.id().equals(fragments[1]))
                        .findFirst().ifPresent(pi -> pi.transitionWorkItem(fragments[3], transition));
            } else {

                this.getProcessRuntime().getWorkItemManager().transitionWorkItem(id, transition);
                removeOnFinish();
            }
        } catch (Throwable e) {
            ((MutableProcessInstances<T>) process.instances()).release(id(), this);
            throw e;
        }
    }

    @Override
    public Set<EventDescription<?>> events() {
        return processInstance().getEventDescriptions();
    }

    @Override
    public Collection<Milestone> milestones() {
        return ((WorkflowProcessInstance) processInstance()).milestones();
    }

    @Override
    public Collection<AdHocFragment> adHocFragments() {
        return ((WorkflowProcessInstance) processInstance()).adHocFragments();
    }

    public void imported() {
        unlock(true);
    }

    @Override
    public String image(String path) {
        if (process().image() == null) {
            return null;
        }
        StringBuilder script = new StringBuilder();
        String image = process().image();

        List<String> completedNodes = ((WorkflowProcessInstanceImpl) processInstance()).getCompletedNodeIds();

        Collection<NodeInstance> activeInstances = ((WorkflowProcessInstanceImpl) processInstance()).getNodeInstances(true);
        if (!activeInstances.isEmpty() || !completedNodes.isEmpty() || status == STATE_ERROR) {
            script.append("<script>");
            script.append(
                    "function remove(item) {if (item != null) {var parent = item.parentNode; parent.removeChild(item);}}");
            script.append(
                    "function linkSet(item, link) {if (item != null) {item.setAttribute('xlink:href', link);}}");
            script.append(
                    "function urlSet(item, url) {if (item != null) {item.onclick=function (e) {openInNewTab(url);};}}");
            script.append(
                    "function fill(item, rgb) {if (item != null) {item.style['fill']=rgb;}}");
            script.append(
                    "function highlight(item) {if (item != null) {item.style['stroke']='rgb(255, 0, 0)';item.style['stroke-width']='2';}}");
            script.append(
                    "var warnIcon = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAACXBIWXMAAA7EAAAOxAGVKw4bAAABz0lEQVQ4y2NgoBAwYhNsK7HVdbOWCpCX5FJjYGRgePzi+63dR59tKO86dBmvAQlB2kJT6ixncLN+CWH4/xvVcEbW/9//8q4taDuZPmvFpXcYBsQFagktaDM7zPj3sxZM7Or93wwMDAwM2oqscHP+M/Ney2g8aztrxeV3KAZ8Phe/moftSwiypfVzPjEwMDAwNKbwoTjm+x/e1VwGC8IYGBgYWBgYGBga88x1edi+BhMbcJwsX0I6S610y7uPXWZhYGBgcLUUDmBg+M9IfNj/Z3SxEA5gYGCAGCArzqpGavTJSUD0sDAwMDC8ef+VUUaMnyQDXr/9wgg34Oi5N3cM1DENkBfH7avDZ1/fgceCpb6Q8dElDqcZGRlRdPz//x8S16jCDP////9vF3/Q9Mi5t2dZGBgYGI5ffHd2475nWwOcpX2QFTbO+8zAwMDA0JCMGo2b9j/feuTc27Mo6UBUkE3q0EKHQxpKvMowsZNXvzMwMDAwmGtzwjXfuP/5rn38QbtX734+w0jKwgJsMjPrjWYHuUi5Y/HO/3V7nu3MaDyX+ubDryf4MhOzvjq/lb+TVIC6Ao86AwMDw80HX25u3Pds48WbH48xMDD8YaAmAAAYL5tXiXxXawAAAABJRU5ErkJggg==';");
            script.append(
                    "var errorIcon = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAC80lEQVR42m2TD0yMYRzHP897d65y0akzUTYjVEKnMNQw2RqbzVrzd2wopkwrFrMw//8sDBtbNmZj5k9tDYXMbEYRM8oiSwqp6E53qbvu8uuG5c+zvXv37v09n+f3+36/j+I/azkERsJsM4zVg74D3tfC3ePw4e9a1fdjLvgnQJ5Z0zKGx8ebQqKi0BkM2OrqqH/wwGPr6LjaDDmHoeEfQDxY5kNJZHi4NTYzE1NY2B90l8NBVUEBNRUVLe9g3lF4/BswWNpcAWXxZnNibMZadCYjOk1D9Sh6usHrgW6PB6UUtZcuUVVd3XwPYovhow8wB1YthoKY5NkYJ0czKi2HugsnMdqbUN0aNqdi9IYt1F+5iquykprCQso9nnPHYKXqB1oWVEzVaZNGpE6he1oS1vU7cDu+Ub43DVfLJ2JzTzNoVCS114tp2LUbd81r3thsXUdgmDJBiMzzOdwfLTTBiNMcgHtaOjMy99L5zUZ78wcGR4zjVUkRb/LWYGn5juO9kyYvnIEFahhMPADPhgaJipPBbyi+n/bxeczL3ukT8EXZLar2zCfaz01XIzhExeZ2OA8ZSgSMFn9fWgIhOA76CfGVaxBx224THmP1AZxtX7mRlcSYzqfSq8CroUX8PAXpSoOBZ8XXED2BQePhi0U2599mSJSV+ofXaH1bjnXpfhmnjfubkghueIr9kUBssBUSe10wbITLM2UezQKhm3OJy9nHR9nceHERpgFu9DHZRKQe4vPjUp5nJtP1RBrx0rhWkuqzcTjMOgGl4pjBG6oYmbYaW8VZgkLcGPpLiCQL/Scso6msmLYyO3o7nJRE3oT8X2EzLoTt6ZBrV5IfuQT+oSKovHsBvXN2tkK7zB7gBGmgdAek9IiefdM6IEUA6yDbIVo6ZZNHQqJ0EhSXwNwgvJ47UHQQ1rl9cv51mWQFhMH0JVIghiRIRoLlFCUTOOTwyitifaUApK79v7fx59J6QfKY/QQgDeikaxmANnmkObx9i38AhmwGt8LiZCUAAAAASUVORK5CYII=';");
            script.append("function openInNewTab(url) {var win = window.open(url, '_blank');win.focus();}");
            for (NodeInstance nodeInstance : activeInstances) {
                if (((NodeInstanceImpl) nodeInstance).getNode().getMetaData().containsKey("hidden_node") ||
                        nodeInstance.getNodeDefinitionId().startsWith("_jbpm-unique")) {
                    continue;
                }

                script.append("highlight(document.getElementById('").append(nodeInstance.getNodeDefinitionId())
                        .append("'));\n");

                if (nodeInstance instanceof LambdaSubProcessNodeInstance) {
                    // add links for call activity
                    LambdaSubProcessNodeInstance subprocess = (LambdaSubProcessNodeInstance) nodeInstance;
                    String url = path + "/" + ((SubProcessNode) subprocess.getNode()).getProcessId() + "/"
                            + subprocess.getProcessInstanceId() + "/image";

                    script.append("urlSet(document.getElementById('").append(nodeInstance.getNodeDefinitionId())
                            .append("_image'), '" + url + "');\n");
                }

                if (nodeInstance instanceof EventSubProcessNodeInstance) {
                    if (((EventSubProcessNodeInstance) nodeInstance).getTimerInstances() != null
                            && !((EventSubProcessNodeInstance) nodeInstance).getTimerInstances().isEmpty()) {
                        String nodeDefId = (String) ((EventSubProcessNode) ((EventSubProcessNodeInstance) nodeInstance)
                                .getEventBasedNode())
                                        .findStartNode().getMetaData("UniqueId");

                        if (nodeDefId != null) {
                            script.append("highlight(document.getElementById('").append(nodeDefId)
                                    .append("'));\n");
                        }
                    }
                }

                if (nodeInstance.getNodeInstanceState().equals(NodeInstanceState.Retrying)) {
                    script.append("linkSet(document.getElementById('").append(nodeInstance.getNodeDefinitionId())
                            .append("_warn_image'), warnIcon);\n");
                } else if (nodeInstance.getNodeInstanceState().equals(NodeInstanceState.Failed)) {
                    script.append("linkSet(document.getElementById('").append(nodeInstance.getNodeDefinitionId())
                            .append("_warn_image'), errorIcon);\n");
                } else {
                    script.append("remove(document.getElementById('").append(nodeInstance.getNodeDefinitionId())
                            .append("_warn_image'));\n");
                }
            }
            for (String nodeInstanceId : completedNodes) {
                script.append("remove(document.getElementById('").append(nodeInstanceId)
                        .append("_warn_image'));\n");
                script.append("fill(document.getElementById('").append(nodeInstanceId)
                        .append("'), 'rgb(160, 160, 160)');\n");
            }

            if (status == STATE_ERROR) {

                for (ProcessError error : errors().get().errors()) {
                    String failedNodeId = error.failedNodeId();

                    script.append("highlight(document.getElementById('").append(failedNodeId)
                            .append("'));\n");
                    script.append("linkSet(document.getElementById('").append(failedNodeId)
                            .append("_warn_image'), errorIcon);\n");
                }
            }
            script.append("</script></svg>");

            image = image.replaceAll("</svg>", script.toString());
        }
        return image;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public ArchivedProcessInstance archive(ArchiveBuilder builder) {
        ArchivedProcessInstance archived = builder.instance(id, process.id(),
                ((MutableProcessInstances) process().instances()).exportInstance(this, false));

        Map<String, Object> variables = processInstance().getVariables();

        for (Entry<String, Object> var : variables.entrySet()) {
            archived.addVariable(builder.variable(var.getKey(), var.getValue()));
        }

        Collection<ProcessInstance<? extends Model>> subInstances = subprocesses(ProcessInstanceReadMode.MUTABLE);
        List<ArchivedProcessInstance> subinstances = new ArrayList<ArchivedProcessInstance>();
        if (!subInstances.isEmpty()) {

            for (ProcessInstance<? extends Model> si : subInstances) {

                ArchivedProcessInstance subArchived = si.archive(builder);
                subinstances.add(subArchived);
            }
        }
        archived.setSubInstances(subinstances);

        return archived;
    }

    public long getVersionTracker() {
        return versionTracker;
    }

    public Set<String> visibleTo() {
        return visibleTo;
    }

    public boolean isConnected() {
        return this.rt != null;
    }

    protected Set<String> setVisibleTo() {

        Set<String> visibleTo = new HashSet<>();
        io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance pi = (io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance) processInstance();

        if (pi.getInitiator() != null && !pi.getInitiator().isEmpty()) {
            visibleTo.add(pi.getInitiator());
        }
        ((WorkflowProcessInstanceImpl) pi).getNodeInstances(true).stream()
                .filter(ni -> ni instanceof HumanTaskNodeInstance).forEach(ni -> {
                    if (((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getPotentialUsers() != null) {
                        visibleTo.addAll(
                                ((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getPotentialUsers());
                    }
                    if (((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getPotentialGroups() != null) {
                        visibleTo.addAll(
                                ((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getPotentialGroups());
                    }
                    if (((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getAdminUsers() != null) {
                        visibleTo.addAll(((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getAdminUsers());
                    }
                    if (((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getAdminGroups() != null) {
                        visibleTo.addAll(((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getAdminUsers());
                    }
                    // remove any defined excluded owners                    
                    if (((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getExcludedUsers() != null) {
                        visibleTo
                                .removeAll(((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem())
                                        .getExcludedUsers());
                    }
                });

        return visibleTo;
    }

    protected void removeOnFinish() {
        io.automatiko.engine.api.runtime.process.ProcessInstance processInstance = processInstance();
        this.visibleTo = setVisibleTo();

        if (processInstance.getState() != ProcessInstance.STATE_ACTIVE
                && processInstance.getState() != ProcessInstance.STATE_ERROR) {
            ((WorkflowProcessInstanceImpl) processInstance).removeEventListener(
                    "processInstanceCompleted:" + processInstance.getId(), completionEventListener, false);

            removeCompletionListener();
            syncProcessInstance((WorkflowProcessInstance) processInstance);
            unbind(this.variables, processInstance().getVariables());
            // apply end of instance strategy on completion
            process.endOfInstanceStrategy().perform(this);
            if (process.endOfInstanceStrategy().shouldInstanceBeUpdated()) {
                addToUnitOfWork(pi -> ((MutableProcessInstances<T>) process.instances()).update(pi.id(), pi),
                        pi -> ((MutableProcessInstances<T>) process.instances()).release(pi.id(), pi));
            }
            if (process.endOfInstanceStrategy().shouldInstanceBeRemoved()) {
                addToUnitOfWork(pi -> ((MutableProcessInstances<T>) process.instances()).remove(pi.id(), pi),
                        pi -> ((MutableProcessInstances<T>) process.instances()).release(pi.id(), pi));
            }
            unlock(true);

        } else {
            syncProcessInstance((WorkflowProcessInstance) processInstance);
            unbind(this.variables, processInstance().getVariables());
            addToUnitOfWork(pi -> ((MutableProcessInstances<T>) process.instances()).update(pi.id(), pi),
                    pi -> ((MutableProcessInstances<T>) process.instances()).release(pi.id(), pi));
            unlock(false);
        }

    }

    // this must be overridden at compile time
    protected Map<String, Object> bind(T variables) {
        HashMap<String, Object> vmap = new HashMap<>();
        if (variables == null) {
            return vmap;
        }
        try {
            for (Field f : variables.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object v = f.get(variables);
                vmap.put(f.getName(), v);
            }
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
        vmap.put("$v", variables);
        return vmap;
    }

    protected void unbind(T variables, Map<String, Object> vmap) {
        if (vmap == null) {
            return;
        }
        try {
            for (Field f : variables.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                f.set(variables, vmap.get(f.getName()));
            }
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
        vmap.put("$v", variables);
    }

    protected void populateChildProcesses(Process<?> process, Collection<ProcessInstance<? extends Model>> collection) {
        populateChildProcesses(process, collection, ProcessInstanceReadMode.READ_ONLY);
    }

    @SuppressWarnings("unchecked")
    protected void populateChildProcesses(Process<?> process, Collection<ProcessInstance<? extends Model>> collection,
            ProcessInstanceReadMode mode) {

        WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) processInstance();

        List<String> children = instance.getChildren().get(process.id());
        if (children != null && !children.isEmpty()) {

            for (String id : children) {
                process.instances().findById(id, mode)
                        .ifPresent(pi -> collection.add((ProcessInstance<? extends Model>) pi));

            }
        }
    }

    protected void collectedFinishedSubprocesses(Process<?> process, Collection<ProcessInstance<? extends Model>> collection) {
        WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) processInstance();

        for (Entry<String, Set<InstanceTuple>> entry : instance.getFinishedSubProcess().entrySet()) {

            String processId = entry.getKey();
            if (process.id().equals(processId)) {

                for (InstanceTuple subInstance : entry.getValue()) {

                    process.instances()
                            .findById(subInstance.getId(), subInstance.getStatus(), ProcessInstanceReadMode.READ_ONLY)
                            .ifPresent(pi -> collection.add((ProcessInstance<? extends Model>) pi));
                }
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractProcessInstance other = (AbstractProcessInstance) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (status == null) {
            if (other.status != null)
                return false;
        } else if (!status.equals(other.status))
            return false;
        return true;
    }

    private ProcessRuntime getProcessRuntime() {
        if (rt == null) {
            throw new UnsupportedOperationException("Process instance is not connected to a Process Runtime");
        } else {
            return rt;
        }
    }

    protected ProcessErrors buildProcessErrors() {
        WorkflowProcessInstanceImpl pi = (WorkflowProcessInstanceImpl) processInstance();

        final List<ExecutionsErrorInfo> errors = pi.errors();
        return new ProcessErrors(errors.stream().map(e -> new ProcessError() {

            @Override
            public String failedNodeId() {
                return e.getFailedNodeId();
            }

            @Override
            public String errorMessage() {
                return e.getErrorMessage();
            }

            @Override
            public String errorId() {
                return e.getErrorId();
            }

            @Override
            public String errorDetails() {
                return e.getErrorDetails();
            }

            @Override
            public void retrigger() {
                WorkflowProcessInstanceImpl pInstance = (WorkflowProcessInstanceImpl) processInstance();
                NodeInstance ni = pInstance.getNodeInstanceByNodeDefinitionId(e.getFailedNodeId(),
                        pInstance.getNodeContainer());
                if (ni == null) {
                    throw new IllegalArgumentException("Node with definition id " + e.getFailedNodeId() + " was not found");
                }
                pInstance.setState(STATE_ACTIVE);
                pInstance.resetErrorForNode(e.getFailedNodeId());

                ni.trigger(null, io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
                if (pInstance.hasErrors()) {
                    pInstance.setState(STATE_ERROR);
                }
                removeOnFinish();
            }

            @Override
            public void skip() {
                WorkflowProcessInstanceImpl pInstance = (WorkflowProcessInstanceImpl) processInstance();
                NodeInstance ni = pInstance.getNodeInstanceByNodeDefinitionId(e.getFailedNodeId(),
                        pInstance.getNodeContainer());
                if (ni == null) {
                    throw new IllegalArgumentException("Node with definition id " + e.getFailedNodeId() + " was not found");
                }
                pInstance.setState(STATE_ACTIVE);
                pInstance.resetErrorForNode(e.getFailedNodeId());

                ((NodeInstanceImpl) ni)
                        .triggerCompleted(io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, true);
                if (pInstance.hasErrors()) {
                    pInstance.setState(STATE_ERROR);
                }
                removeOnFinish();
            }
        }).collect(Collectors.toList()));
    }

    protected Tags buildTags() {
        ((WorkflowProcessInstance) processInstance).evaluateTags();
        return new Tags() {

            Collection<String> values = ((WorkflowProcessInstanceImpl) processInstance()).getTags().stream()
                    .map(t -> t.getValue()).collect(Collectors.toList());

            @Override
            public Collection<String> values() {

                return values;
            }

            @Override
            public boolean remove(String id) {
                WorkflowProcessInstanceImpl pi = (WorkflowProcessInstanceImpl) processInstance();
                boolean removed = pi.removedTag(id);
                removeOnFinish();

                return removed;
            }

            @Override
            public Tag get(String id) {
                WorkflowProcessInstanceImpl pi = (WorkflowProcessInstanceImpl) processInstance();
                return pi.getTags().stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
            }

            @Override
            public void add(String value) {
                WorkflowProcessInstanceImpl pi = (WorkflowProcessInstanceImpl) processInstance();
                pi.addTag(value);
                removeOnFinish();
            }

            @Override
            public Collection<Tag> get() {
                WorkflowProcessInstanceImpl pi = (WorkflowProcessInstanceImpl) processInstance();
                return pi.getTags();
            }
        };
    }

    protected void configureLock(String businessKey) {
        this.lock = businessKey == null || businessKey.isEmpty() ? new ReentrantLock() : process.locks().lock(businessKey);
    }

    protected void lock() {
        if (lock == null) {
            return;
        }
        LOGGER.debug("Locking instance {}  ({}) on thread {} lock {}", id, businessKey(), Thread.currentThread().getName(),
                lock);
        lock.lock();
        LOGGER.debug("Locked instance {} ({}) on thread {} lock {}", id(), businessKey(), Thread.currentThread().getName(),
                lock);
    }

    public void unlock(boolean remove) {
        if (lock == null) {
            return;
        }

        ((InternalProcessRuntime) getProcessRuntime()).getUnitOfWorkManager().currentUnitOfWork()
                .intercept(new UnlockWorkUnit(this, lock, remove));
    }

    private class CompletionEventListener implements EventListener {

        @Override
        public void signalEvent(String type, Object event) {
            removeOnFinish();
        }

        @Override
        public String[] getEventTypes() {
            return new String[] { "processInstanceCompleted:" + processInstance.getId() };
        }
    }

}
