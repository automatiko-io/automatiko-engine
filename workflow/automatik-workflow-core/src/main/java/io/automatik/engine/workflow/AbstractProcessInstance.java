
package io.automatik.engine.workflow;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.automatik.engine.api.Model;
import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.runtime.process.EventListener;
import io.automatik.engine.api.runtime.process.ProcessRuntime;
import io.automatik.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatik.engine.api.workflow.EventDescription;
import io.automatik.engine.api.workflow.MutableProcessInstances;
import io.automatik.engine.api.workflow.NodeInstanceNotFoundException;
import io.automatik.engine.api.workflow.NodeNotFoundException;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessError;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatik.engine.api.workflow.ProcessInstanceNotFoundException;
import io.automatik.engine.api.workflow.Signal;
import io.automatik.engine.api.workflow.WorkItem;
import io.automatik.engine.api.workflow.flexible.AdHocFragment;
import io.automatik.engine.api.workflow.flexible.Milestone;
import io.automatik.engine.api.workflow.workitem.Policy;
import io.automatik.engine.api.workflow.workitem.Transition;
import io.automatik.engine.services.correlation.CorrelationAwareProcessRuntime;
import io.automatik.engine.services.correlation.CorrelationKey;
import io.automatik.engine.services.correlation.StringCorrelationKey;
import io.automatik.engine.services.uow.ProcessInstanceWorkUnit;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatik.engine.workflow.process.instance.NodeInstance;
import io.automatik.engine.workflow.process.instance.NodeInstanceContainer;
import io.automatik.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceImpl;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatik.engine.workflow.process.instance.node.WorkItemNodeInstance;

public abstract class AbstractProcessInstance<T extends Model> implements ProcessInstance<T> {

	protected final T variables;
	protected final AbstractProcess<T> process;
	protected final ProcessRuntime rt;
	protected io.automatik.engine.api.runtime.process.ProcessInstance processInstance;

	protected Integer status;
	protected String id;
	protected CorrelationKey correlationKey;
	protected String description;

	protected ProcessError processError;

	protected Supplier<io.automatik.engine.api.runtime.process.ProcessInstance> reloadSupplier;

	protected CompletionEventListener completionEventListener = new CompletionEventListener();

	public AbstractProcessInstance(AbstractProcess<T> process, T variables, ProcessRuntime rt) {
		this(process, variables, null, rt);
	}

	public AbstractProcessInstance(AbstractProcess<T> process, T variables, String businessKey, ProcessRuntime rt) {
		this.process = process;
		this.rt = rt;
		this.variables = variables;

		setCorrelationKey(businessKey);

		Map<String, Object> map = bind(variables);
		String processId = process.process().getId();
		this.processInstance = ((CorrelationAwareProcessRuntime) rt).createProcessInstance(processId, correlationKey,
				map);
		this.description = ((WorkflowProcessInstanceImpl) processInstance).getDescription();
		this.status = ProcessInstance.STATE_PENDING;
	}

	// for marshaller/persistence only
	public void internalSetProcessInstance(io.automatik.engine.api.runtime.process.ProcessInstance processInstance) {
		if (this.processInstance != null && this.status != ProcessInstance.STATE_PENDING) {
			throw new IllegalStateException("Impossible to override process instance that already exists");
		}
		this.processInstance = processInstance;
		this.status = processInstance.getState();
		this.id = processInstance.getId();
		setCorrelationKey(((WorkflowProcessInstance) processInstance).getCorrelationKey());
		this.description = ((WorkflowProcessInstanceImpl) processInstance).getDescription();
		((WorkflowProcessInstanceImpl) this.processInstance).setProcessRuntime(((InternalProcessRuntime) rt));
		((WorkflowProcessInstanceImpl) this.processInstance).reconnect();
		((WorkflowProcessInstanceImpl) this.processInstance).setMetaData("KogitoProcessInstance", this);
		((WorkflowProcessInstance) processInstance).addEventListener("processInstanceCompleted:" + this.id,
				completionEventListener, false);

		for (io.automatik.engine.api.runtime.process.NodeInstance nodeInstance : ((WorkflowProcessInstance) processInstance)
				.getNodeInstances()) {
			if (nodeInstance instanceof WorkItemNodeInstance) {
				((WorkItemNodeInstance) nodeInstance).internalRegisterWorkItem();
			}
		}

		unbind(variables, processInstance.getVariables());
	}

	private void setCorrelationKey(String businessKey) {
		if (businessKey != null && !businessKey.trim().isEmpty()) {
			correlationKey = new StringCorrelationKey(businessKey);
		}
	}

	public io.automatik.engine.api.runtime.process.ProcessInstance internalGetProcessInstance() {
		return processInstance;
	}

	public void internalRemoveProcessInstance(
			Supplier<io.automatik.engine.api.runtime.process.ProcessInstance> reloadSupplier) {
		this.reloadSupplier = reloadSupplier;
		this.status = processInstance.getState();
		if (this.status == STATE_ERROR) {
			this.processError = buildProcessError();
		}
		this.processInstance = null;
	}

	public void start() {
		start(null, null, null);
	}

	public void start(String trigger, String referenceId, Object data) {
		if (this.status != ProcessInstance.STATE_PENDING) {
			throw new IllegalStateException("Impossible to start process instance that already was started");
		}
		this.status = ProcessInstance.STATE_ACTIVE;

		if (referenceId != null) {
			((WorkflowProcessInstance) processInstance).setReferenceId(referenceId);
		}

		((InternalProcessRuntime) rt).getProcessInstanceManager().addProcessInstance(this.processInstance,
				this.correlationKey);
		this.id = processInstance.getId();
		// this applies to business keys only as non business keys process instances id
		// are always unique
		if (correlationKey != null && process.instances.exists(id)) {
			throw new ProcessInstanceDuplicatedException(correlationKey.getName());
		}
		((WorkflowProcessInstance) processInstance).addEventListener("processInstanceCompleted:" + this.id,
				completionEventListener, false);
		io.automatik.engine.api.runtime.process.ProcessInstance processInstance = this.rt.startProcessInstance(this.id,
				trigger, data);
		addToUnitOfWork(pi -> ((MutableProcessInstances<T>) process.instances()).create(pi.id(), pi));
		unbind(variables, processInstance.getVariables());
		if (this.processInstance != null) {
			this.status = this.processInstance.getState();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void addToUnitOfWork(Consumer<ProcessInstance<T>> action) {
		((InternalProcessRuntime) rt).getUnitOfWorkManager().currentUnitOfWork()
				.intercept(new ProcessInstanceWorkUnit(this, action));
	}

	public void abort() {
		String pid = processInstance().getId();
		unbind(variables, processInstance().getVariables());
		this.rt.abortProcessInstance(pid);
		this.status = processInstance.getState();
		addToUnitOfWork(pi -> ((MutableProcessInstances<T>) process.instances()).remove(pi.id()));
	}

	@Override
	public <S> void send(Signal<S> signal) {
		if (signal.referenceId() != null) {
			((WorkflowProcessInstance) processInstance()).setReferenceId(signal.referenceId());
		}
		processInstance().signalEvent(signal.channel(), signal.payload());
		removeOnFinish();
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
	public Date startDate() {
		return this.processInstance != null && processInstance instanceof WorkflowProcessInstanceImpl
				? ((WorkflowProcessInstanceImpl) this.processInstance).getStartDate()
				: null;
	}

	@Override
	public void updateVariables(T updates) {
		Map<String, Object> map = bind(updates);

		for (Entry<String, Object> entry : map.entrySet()) {
			((WorkflowProcessInstance) processInstance()).setVariable(entry.getKey(), entry.getValue());
		}
		addToUnitOfWork(pi -> ((MutableProcessInstances<T>) process.instances()).update(pi.id(), pi));
	}

	@Override
	public Optional<ProcessError> error() {
		if (this.status == STATE_ERROR) {
			return Optional.of(this.processError != null ? this.processError : buildProcessError());
		}

		return Optional.empty();
	}

	@Override
	public void startFrom(String nodeId) {
		startFrom(nodeId, null);
	}

	@Override
	public void startFrom(String nodeId, String referenceId) {
		((WorkflowProcessInstance) processInstance).setStartDate(new Date());
		((WorkflowProcessInstance) processInstance).setState(STATE_ACTIVE);
		((InternalProcessRuntime) rt).getProcessInstanceManager().addProcessInstance(this.processInstance,
				this.correlationKey);
		this.id = processInstance.getId();
		((WorkflowProcessInstance) processInstance).addEventListener("processInstanceCompleted:" + this.id,
				completionEventListener, false);
		if (referenceId != null) {
			((WorkflowProcessInstance) processInstance).setReferenceId(referenceId);
		}
		triggerNode(nodeId);
		addToUnitOfWork(pi -> ((MutableProcessInstances<T>) process.instances()).update(pi.id(), pi));
		unbind(variables, processInstance.getVariables());
		if (processInstance != null) {
			this.status = processInstance.getState();
		}
	}

	@Override
	public void triggerNode(String nodeId) {
		WorkflowProcessInstanceImpl wfpi = ((WorkflowProcessInstanceImpl) processInstance());
		ExecutableProcess rfp = ((ExecutableProcess) wfpi.getProcess());

		Node node = rfp.getNodesRecursively().stream().filter(ni -> nodeId.equals(ni.getMetaData().get("UniqueId")))
				.findFirst().orElseThrow(() -> new NodeNotFoundException(this.id, nodeId));

		Node parentNode = rfp.getParentNode(node.getId());

		NodeInstanceContainer nodeInstanceContainerNode = parentNode == null ? wfpi
				: ((NodeInstanceContainer) wfpi.getNodeInstance(parentNode));

		nodeInstanceContainerNode.getNodeInstance(node).trigger(null,
				io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
	}

	@Override
	public void cancelNodeInstance(String nodeInstanceId) {
		NodeInstance nodeInstance = ((WorkflowProcessInstanceImpl) processInstance()).getNodeInstances(true).stream()
				.filter(ni -> ni.getId().equals(nodeInstanceId)).findFirst()
				.orElseThrow(() -> new NodeInstanceNotFoundException(this.id, nodeInstanceId));

		nodeInstance.cancel();
		removeOnFinish();
	}

	@Override
	public void retriggerNodeInstance(String nodeInstanceId) {
		NodeInstance nodeInstance = ((WorkflowProcessInstanceImpl) processInstance()).getNodeInstances(true).stream()
				.filter(ni -> ni.getId().equals(nodeInstanceId)).findFirst()
				.orElseThrow(() -> new NodeInstanceNotFoundException(this.id, nodeInstanceId));

		((NodeInstanceImpl) nodeInstance).retrigger(true);
		removeOnFinish();
	}

	public io.automatik.engine.api.runtime.process.ProcessInstance processInstance() {
		if (this.processInstance == null) {
			this.processInstance = reloadSupplier.get();
			if (this.processInstance == null) {
				throw new ProcessInstanceNotFoundException(id);
			}
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
		return new BaseWorkItem(workItemInstance.getId(), workItemInstance.getWorkItem().getId(),
				(String) workItemInstance.getWorkItem().getParameters().getOrDefault("TaskName",
						workItemInstance.getNodeName()),
				workItemInstance.getWorkItem().getState(), workItemInstance.getWorkItem().getPhaseId(),
				workItemInstance.getWorkItem().getPhaseStatus(), workItemInstance.getWorkItem().getParameters(),
				workItemInstance.getWorkItem().getResults());
	}

	@Override
	public List<WorkItem> workItems(Policy<?>... policies) {
		return ((WorkflowProcessInstanceImpl) processInstance()).getNodeInstances(true).stream().filter(
				ni -> ni instanceof WorkItemNodeInstance && ((WorkItemNodeInstance) ni).getWorkItem().enforce(policies))
				.map(ni -> new BaseWorkItem(ni.getId(), ((WorkItemNodeInstance) ni).getWorkItemId(),
						(String) ((WorkItemNodeInstance) ni).getWorkItem().getParameters().getOrDefault("TaskName",
								ni.getNodeName()),
						((WorkItemNodeInstance) ni).getWorkItem().getState(),
						((WorkItemNodeInstance) ni).getWorkItem().getPhaseId(),
						((WorkItemNodeInstance) ni).getWorkItem().getPhaseStatus(),
						((WorkItemNodeInstance) ni).getWorkItem().getParameters(),
						((WorkItemNodeInstance) ni).getWorkItem().getResults()))
				.collect(Collectors.toList());

	}

	@Override
	public void completeWorkItem(String id, Map<String, Object> variables, Policy<?>... policies) {
		this.rt.getWorkItemManager().completeWorkItem(id, variables, policies);
		removeOnFinish();
	}

	@Override
	public void abortWorkItem(String id, Policy<?>... policies) {
		this.rt.getWorkItemManager().abortWorkItem(id, policies);
		removeOnFinish();
	}

	@Override
	public void transitionWorkItem(String id, Transition<?> transition) {
		this.rt.getWorkItemManager().transitionWorkItem(id, transition);
		removeOnFinish();
	}

	@Override
	public Set<EventDescription<?>> events() {
		return processInstance().getEventDescriptions();
	}

	@Override
	public Collection<Milestone> milestones() {
		return ((WorkflowProcessInstance) processInstance).milestones();
	}

	@Override
	public Collection<AdHocFragment> adHocFragments() {
		return ((WorkflowProcessInstance) processInstance).adHocFragments();
	}

	protected void removeOnFinish() {
		if (processInstance.getState() != ProcessInstance.STATE_ACTIVE
				&& processInstance.getState() != ProcessInstance.STATE_ERROR) {
			((WorkflowProcessInstance) processInstance).removeEventListener(
					"processInstanceCompleted:" + processInstance.getId(), completionEventListener, false);

			this.status = processInstance.getState();
			this.id = processInstance.getId();
			setCorrelationKey(((WorkflowProcessInstance) processInstance).getCorrelationKey());
			this.description = ((WorkflowProcessInstanceImpl) processInstance).getDescription();

			addToUnitOfWork(pi -> ((MutableProcessInstances<T>) process.instances()).remove(pi.id()));

		} else {
			addToUnitOfWork(pi -> ((MutableProcessInstances<T>) process.instances()).update(pi.id(), pi));
		}
		unbind(this.variables, processInstance().getVariables());
		this.status = processInstance.getState();
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
				Object v = null;
				v = f.get(variables);
				vmap.put(f.getName(), v);
			}
		} catch (IllegalAccessException e) {
			throw new Error(e);
		}
		vmap.put("$v", variables);
		return vmap;
	}

	protected void unbind(T variables, Map<String, Object> vmap) {
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

	protected ProcessError buildProcessError() {
		WorkflowProcessInstanceImpl pi = (WorkflowProcessInstanceImpl) processInstance();

		final String errorMessage = pi.getErrorMessage();
		final String nodeInError = pi.getNodeIdInError();
		return new ProcessError() {

			@Override
			public String failedNodeId() {
				return nodeInError;
			}

			@Override
			public String errorMessage() {
				return errorMessage;
			}

			@Override
			public void retrigger() {
				WorkflowProcessInstanceImpl pInstance = (WorkflowProcessInstanceImpl) processInstance();
				NodeInstance ni = pInstance.getNodeInstanceByNodeDefinitionId(nodeInError,
						pInstance.getNodeContainer());
				pInstance.setState(STATE_ACTIVE);
				pInstance.internalSetErrorNodeId(null);
				pInstance.internalSetErrorMessage(null);
				ni.trigger(null, io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
				removeOnFinish();
			}

			@Override
			public void skip() {
				WorkflowProcessInstanceImpl pInstance = (WorkflowProcessInstanceImpl) processInstance();
				NodeInstance ni = pInstance.getNodeInstanceByNodeDefinitionId(nodeInError,
						pInstance.getNodeContainer());
				pInstance.setState(STATE_ACTIVE);
				pInstance.internalSetErrorNodeId(null);
				pInstance.internalSetErrorMessage(null);
				((NodeInstanceImpl) ni)
						.triggerCompleted(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, true);
				removeOnFinish();
			}
		};
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
