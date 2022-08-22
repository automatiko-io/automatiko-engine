
package io.automatiko.engine.workflow.base.instance.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.impl.XmlProcessDumper;
import io.automatiko.engine.workflow.base.core.impl.XmlProcessDumperFactory;
import io.automatiko.engine.workflow.base.instance.ContextInstance;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;
import io.automatiko.engine.workflow.util.InstanceTuple;

/**
 * Default implementation of a process instance.
 * 
 */
public abstract class ProcessInstanceImpl implements ProcessInstance, Serializable {

    private static final long serialVersionUID = 510l;

    private String id;
    private String processId;
    private transient Process process;
    private String processXml;
    private int state = STATE_PENDING;
    private Map<String, ContextInstance> contextInstances = new HashMap<String, ContextInstance>();
    private Map<String, List<ContextInstance>> subContextInstances = new HashMap<String, List<ContextInstance>>();
    private transient InternalProcessRuntime runtime;
    private Map<String, Object> metaData = new ConcurrentHashMap<String, Object>();
    private String outcome;
    private String parentProcessInstanceId;
    private String rootProcessInstanceId;
    private String description;
    private String rootProcessId;

    private Map<String, List<String>> children = new HashMap<String, List<String>>();

    private Map<String, Set<InstanceTuple>> finishedSubProcesses = new HashMap<String, Set<InstanceTuple>>();

    public void setId(final String id) {
        this.id = id;

    }

    public String getId() {
        return this.id;
    }

    public void setProcess(final Process process) {
        this.processId = process.getId();
        this.process = (Process) process;
    }

    public void updateProcess(final Process process) {
        setProcess(process);
        XmlProcessDumper dumper = XmlProcessDumperFactory.newXmlProcessDumperFactory();
        this.processXml = dumper.dumpProcess(process);
    }

    public String getProcessXml() {
        return processXml;
    }

    public void setProcessXml(String processXml) {
        if (processXml != null && processXml.trim().length() > 0) {
            this.processXml = processXml;
        }
    }

    public Process getProcess() {
        if (this.process == null) {
            if (processXml == null) {
                if (runtime == null) {
                    throw new IllegalStateException("Process instance " + id + "[" + processId + "] is disconnected.");
                }
                this.process = runtime.getProcess(processId);
            } else {
                XmlProcessDumper dumper = XmlProcessDumperFactory.newXmlProcessDumperFactory();
                this.process = dumper.readProcess(processXml);
            }
        }
        return this.process;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getProcessId() {
        return processId;
    }

    public String getProcessName() {
        return getProcess().getName();
    }

    public void setState(final int state) {
        internalSetState(state);
    }

    public void setState(final int state, String outcome) {
        this.outcome = outcome;
        internalSetState(state);
    }

    public void internalSetState(final int state) {
        this.state = state;
    }

    public int getState() {
        return this.state;
    }

    public void setProcessRuntime(final InternalProcessRuntime runtime) {
        if (this.runtime != null) {
            throw new IllegalArgumentException("Runtime can only be set once.");
        }
        this.runtime = runtime;
    }

    public InternalProcessRuntime getProcessRuntime() {
        return this.runtime;
    }

    public ContextContainer getContextContainer() {
        return (ContextContainer) getProcess();
    }

    public void setContextInstance(String contextId, ContextInstance contextInstance) {
        this.contextInstances.put(contextId, contextInstance);
    }

    public ContextInstance getContextInstance(String contextId) {
        ContextInstance contextInstance = this.contextInstances.get(contextId);
        if (contextInstance != null) {
            return contextInstance;
        }
        Context context = ((ContextContainer) getProcess()).getDefaultContext(contextId);
        if (context != null) {
            contextInstance = getContextInstance(context);
            return contextInstance;
        }
        return null;
    }

    public List<ContextInstance> getContextInstances(String contextId) {
        return this.subContextInstances.get(contextId);
    }

    public void addContextInstance(String contextId, ContextInstance contextInstance) {
        List<ContextInstance> list = this.subContextInstances.get(contextId);
        if (list == null) {
            list = new ArrayList<>();
            this.subContextInstances.put(contextId, list);
        }
        list.add(contextInstance);
    }

    public void removeContextInstance(String contextId, ContextInstance contextInstance) {
        List<ContextInstance> list = this.subContextInstances.get(contextId);
        if (list != null) {
            list.remove(contextInstance);
        }
    }

    public ContextInstance getContextInstance(String contextId, long id) {
        List<ContextInstance> contextInstances = subContextInstances.get(contextId);
        if (contextInstances != null) {
            for (ContextInstance contextInstance : contextInstances) {
                if (contextInstance.getContextId() == id) {
                    return contextInstance;
                }
            }
        }
        return null;
    }

    public ContextInstance getContextInstance(final Context context) {
        ContextInstanceFactory conf = ContextInstanceFactoryRegistry.INSTANCE.getContextInstanceFactory(context);
        if (conf == null) {
            throw new IllegalArgumentException("Illegal context type (registry not found): " + context.getClass());
        }
        ContextInstance contextInstance = (ContextInstance) conf.getContextInstance(context, this, this);
        if (contextInstance == null) {
            throw new IllegalArgumentException("Illegal context type (instance not found): " + context.getClass());
        }
        return contextInstance;
    }

    public void signalEvent(String type, Object event) {
    }

    public void start() {
        start(null, null);
    }

    public void start(String trigger, Object triggerData) {
        synchronized (this) {
            if (getState() != ProcessInstanceImpl.STATE_PENDING) {
                throw new IllegalArgumentException("A process instance can only be started once");
            }
            setState(ProcessInstanceImpl.STATE_ACTIVE);
            internalStart(trigger, triggerData);
        }
    }

    protected abstract void internalStart(String trigger, Object triggerData);

    public void disconnect() {
        if (runtime != null) {
            runtime.getProcessInstanceManager().internalRemoveProcessInstance(this);
            runtime = null;
        }
    }

    public void reconnect() {
        runtime.getProcessInstanceManager().internalAddProcessInstance(this);
    }

    public String[] getEventTypes() {
        return null;
    }

    public String toString() {
        final StringBuilder b = new StringBuilder("ProcessInstance ");
        b.append(getId());
        b.append(" [processId=");
        b.append(this.process.getId());
        b.append(",state=");
        b.append(this.state);
        b.append("]");
        return b.toString();
    }

    public Map<String, Object> getMetaData() {
        return this.metaData;
    }

    public void setMetaData(String name, Object data) {
        this.metaData.put(name, data);
    }

    public Object getMetaData(String name) {
        return this.metaData.get(name);
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getParentProcessInstanceId() {
        return parentProcessInstanceId;
    }

    public void setParentProcessInstanceId(String parentProcessInstanceId) {
        this.parentProcessInstanceId = parentProcessInstanceId;
    }

    public String getRootProcessInstanceId() {
        return rootProcessInstanceId;
    }

    public void setRootProcessInstanceId(String rootProcessInstanceId) {
        this.rootProcessInstanceId = rootProcessInstanceId;
    }

    public String getRootProcessId() {
        return rootProcessId;
    }

    public void setRootProcessId(String rootProcessId) {
        this.rootProcessId = rootProcessId;
    }

    public String getDescription() {

        description = process.getName();
        if (process != null) {
            Object metaData = process.getMetaData().getOrDefault("description", process.getMetaData().get("customDescription"));
            if (metaData instanceof String) {
                description = ((WorkflowProcess) process).evaluateExpression((String) metaData, this);
            }
        }

        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, List<String>> getChildren() {
        return this.children;
    }

    public void addChild(String processId, String processInstanceId) {
        this.children.computeIfAbsent(processId, p -> new ArrayList<String>()).add(processInstanceId);
    }

    public void addChildren(String processId, List<String> processInstanceIds) {
        this.children.computeIfAbsent(processId, p -> new ArrayList<String>()).addAll(processInstanceIds);
    }

    public void removeChild(String processId, String processInstanceId) {
        Optional.ofNullable(this.children.get(processId)).ifPresent(l -> l.remove(processInstanceId));
    }

    public Map<String, Set<InstanceTuple>> getFinishedSubProcess() {
        return this.finishedSubProcesses;
    }

    public void addFinishedSubProcess(String processId, String processInstanceId, int status) {
        this.finishedSubProcesses.computeIfAbsent(processId, p -> new LinkedHashSet<InstanceTuple>())
                .add(new InstanceTuple(processInstanceId, status));
    }

    public void addFinishedSubProcesses(String processId, List<InstanceTuple> processInstanceIds) {
        this.finishedSubProcesses.computeIfAbsent(processId, p -> new LinkedHashSet<>()).addAll(processInstanceIds);
    }
}
