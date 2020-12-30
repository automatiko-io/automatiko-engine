
package io.automatiko.engine.workflow.marshalling.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.protobuf.ExtensionRegistry;

import io.automatiko.engine.api.marshalling.ObjectMarshallingStrategy;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.base.instance.impl.workitem.DefaultWorkItemManager;
import io.automatiko.engine.workflow.base.instance.impl.workitem.WorkItemImpl;
import io.automatiko.engine.workflow.marshalling.impl.AutomatikoMessages.Header;
import io.automatiko.engine.workflow.marshalling.impl.AutomatikoMessages.Variable;
import io.automatiko.engine.workflow.marshalling.impl.AutomatikoMessages.VariableContainer;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

public class ProtobufProcessMarshaller implements ProcessMarshaller {

    private static boolean persistWorkItemVars = Boolean
            .parseBoolean(System.getProperty("org.jbpm.wi.variable.persist", "true"));

    // mainly for testability as the setting is global
    public static void setWorkItemVarsPersistence(boolean turnOn) {
        persistWorkItemVars = turnOn;
    }

    public void writeProcessInstances(MarshallerWriteContext context) throws IOException {
        AutomatikoMessages.ProcessData.Builder _pdata = (AutomatikoMessages.ProcessData.Builder) context.parameterObject;

        List<io.automatiko.engine.api.runtime.process.ProcessInstance> processInstances = new ArrayList<io.automatiko.engine.api.runtime.process.ProcessInstance>(
                context.getProcessRuntime().getProcessInstances());
        Collections.sort(processInstances, new Comparator<io.automatiko.engine.api.runtime.process.ProcessInstance>() {
            public int compare(io.automatiko.engine.api.runtime.process.ProcessInstance o1,
                    io.automatiko.engine.api.runtime.process.ProcessInstance o2) {
                return (int) (o1.getId().compareTo(o2.getId()));
            }
        });

        for (io.automatiko.engine.api.runtime.process.ProcessInstance processInstance : processInstances) {
            String processType = processInstance.getProcess().getType();
            AutomatikoMessages.ProcessInstance _instance = (AutomatikoMessages.ProcessInstance) ProcessMarshallerRegistry.INSTANCE
                    .getMarshaller(processType).writeProcessInstance(context, processInstance);
            _pdata.addExtension(AutomatikoMessages.processInstance, _instance);
        }
    }

    public void writeWorkItems(MarshallerWriteContext context) throws IOException {
        AutomatikoMessages.ProcessData.Builder _pdata = (AutomatikoMessages.ProcessData.Builder) context.parameterObject;

        List<WorkItem> workItems = new ArrayList<WorkItem>(
                ((DefaultWorkItemManager) context.getProcessRuntime().getWorkItemManager()).getWorkItems());
        Collections.sort(workItems, new Comparator<WorkItem>() {
            public int compare(WorkItem o1, WorkItem o2) {
                return (int) (o2.getId().compareTo(o1.getId()));
            }
        });
        for (WorkItem workItem : workItems) {
            _pdata.addExtension(AutomatikoMessages.workItem, writeWorkItemInstance(context, workItem));
        }
    }

    public static AutomatikoMessages.WorkItem writeWorkItemInstance(MarshallerWriteContext context, WorkItem workItem)
            throws IOException {
        return writeWorkItem(context, workItem, true);
    }

    public List<ProcessInstance> readProcessInstances(MarshallerReaderContext context) throws IOException {
        AutomatikoMessages.ProcessData _pdata = (AutomatikoMessages.ProcessData) context.parameterObject;
        List<ProcessInstance> processInstanceList = new ArrayList<ProcessInstance>();
        for (AutomatikoMessages.ProcessInstance _instance : _pdata.getExtension(AutomatikoMessages.processInstance)) {
            context.parameterObject = _instance;
            ProcessInstance processInstance = ProcessMarshallerRegistry.INSTANCE
                    .getMarshaller(_instance.getProcessType()).readProcessInstance(context);
            ((WorkflowProcessInstanceImpl) processInstance).reconnect();
            processInstanceList.add(processInstance);
        }
        return processInstanceList;
    }

    public void readWorkItems(MarshallerReaderContext context) throws IOException {
        AutomatikoMessages.ProcessData _pdata = (AutomatikoMessages.ProcessData) context.parameterObject;
        InternalProcessRuntime wm = context.getProcessRuntime();
        for (AutomatikoMessages.WorkItem _workItem : _pdata.getExtension(AutomatikoMessages.workItem)) {
            WorkItem workItem = readWorkItem(context, _workItem);
            ((DefaultWorkItemManager) wm.getWorkItemManager()).internalAddWorkItem((WorkItem) workItem);
        }
    }

    public static AutomatikoMessages.WorkItem writeWorkItem(MarshallerWriteContext context, WorkItem workItem,
            boolean includeVariables) throws IOException {
        AutomatikoMessages.WorkItem.Builder _workItem = AutomatikoMessages.WorkItem.newBuilder().setId(workItem.getId())
                .setProcessInstancesId(workItem.getProcessInstanceId()).setName(workItem.getName())
                .setState(workItem.getState());

        if (workItem instanceof WorkItemImpl) {

            _workItem.setNodeId(((WorkItemImpl) workItem).getNodeId())
                    .setNodeInstanceId(((WorkItemImpl) workItem).getNodeInstanceId());
        }

        if (includeVariables) {
            Map<String, Object> parameters = workItem.getParameters();
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                _workItem.addVariable(marshallVariable(context, entry.getKey(), entry.getValue()));
            }
        }
        return _workItem.build();
    }

    public static WorkItem readWorkItem(MarshallerReaderContext context, AutomatikoMessages.WorkItem _workItem)
            throws IOException {
        return readWorkItem(context, _workItem, true);
    }

    public static WorkItem readWorkItem(MarshallerReaderContext context, AutomatikoMessages.WorkItem _workItem,
            boolean includeVariables) throws IOException {
        WorkItemImpl workItem = new WorkItemImpl();
        workItem.setId(_workItem.getId());
        workItem.setProcessInstanceId(_workItem.getProcessInstancesId());
        workItem.setName(_workItem.getName());
        workItem.setState(_workItem.getState());
        workItem.setNodeId(_workItem.getNodeId());
        workItem.setNodeInstanceId(_workItem.getNodeInstanceId());

        if (includeVariables) {
            for (AutomatikoMessages.Variable _variable : _workItem.getVariableList()) {
                try {
                    Object value = unmarshallVariableValue(context, _variable);
                    workItem.setParameter(_variable.getName(), value);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(
                            "Could not reload parameter " + _variable.getName() + " for work item " + _workItem);
                }
            }
        }

        return workItem;
    }

    public static Variable marshallVariable(MarshallerWriteContext context, String name, Object value)
            throws IOException {
        AutomatikoMessages.Variable.Builder builder = AutomatikoMessages.Variable.newBuilder().setName(name);
        if (value != null) {
            ObjectMarshallingStrategy strategy = context.objectMarshallingStrategyStore.getStrategyObject(value);
            Integer index = context.getStrategyIndex(strategy);
            builder.setStrategyIndex(index).setDataType(strategy.getType(value.getClass())).setValue(
                    ByteString.copyFrom(strategy.marshal(context.strategyContext.get(strategy), context, value)));
        }
        return builder.build();
    }

    public static Variable marshallVariablesMap(MarshallerWriteContext context, Map<String, Object> variables)
            throws IOException {
        Map<String, Variable> marshalledVariables = new HashMap<String, Variable>();
        for (String key : variables.keySet()) {
            AutomatikoMessages.Variable.Builder builder = AutomatikoMessages.Variable.newBuilder().setName(key);
            Object variable = variables.get(key);
            if (variable != null) {
                ObjectMarshallingStrategy strategy = context.objectMarshallingStrategyStore.getStrategyObject(variable);
                Integer index = context.getStrategyIndex(strategy);
                builder.setStrategyIndex(index).setDataType(strategy.getType(variable.getClass())).setValue(ByteString
                        .copyFrom(strategy.marshal(context.strategyContext.get(strategy), context, variable)));

            }

            marshalledVariables.put(key, builder.build());
        }

        return marshallVariable(context, "variablesMap", marshalledVariables);
    }

    public static VariableContainer marshallVariablesContainer(MarshallerWriteContext context,
            Map<String, Object> variables) throws IOException {
        AutomatikoMessages.VariableContainer.Builder vcbuilder = AutomatikoMessages.VariableContainer.newBuilder();
        for (String key : variables.keySet()) {
            AutomatikoMessages.Variable.Builder builder = AutomatikoMessages.Variable.newBuilder().setName(key);
            if (variables.get(key) != null) {
                ObjectMarshallingStrategy strategy = context.objectMarshallingStrategyStore
                        .getStrategyObject(variables.get(key));
                Integer index = context.getStrategyIndex(strategy);
                builder.setStrategyIndex(index).setValue(ByteString.copyFrom(
                        strategy.marshal(context.strategyContext.get(strategy), context, variables.get(key))));

            }

            vcbuilder.addVariable(builder.build());
        }

        return vcbuilder.build();
    }

    public static Object unmarshallVariableValue(MarshallerReaderContext context, AutomatikoMessages.Variable _variable)
            throws IOException, ClassNotFoundException {
        if (_variable.getValue() == null || _variable.getValue().isEmpty()) {
            return null;
        }
        ObjectMarshallingStrategy strategy = context.usedStrategies.get(_variable.getStrategyIndex());
        Object value = strategy.unmarshal(_variable.getDataType(), context.strategyContexts.get(strategy), context,
                _variable.getValue().toByteArray(), null);
        return value;
    }

    public static Map<String, Object> unmarshallVariableContainerValue(MarshallerReaderContext context,
            AutomatikoMessages.VariableContainer _variableContiner) throws IOException, ClassNotFoundException {
        Map<String, Object> variables = new HashMap<String, Object>();
        if (_variableContiner.getVariableCount() == 0) {
            return variables;
        }

        for (Variable _variable : _variableContiner.getVariableList()) {

            Object value = ProtobufProcessMarshaller.unmarshallVariableValue(context, _variable);
            if (value != null) {
                variables.put(_variable.getName(), value);
            }
        }
        return variables;
    }

    public void init(MarshallerReaderContext context) {
        ExtensionRegistry registry = (ExtensionRegistry) context.parameterObject;
        registry.add(AutomatikoMessages.processInstance);
        registry.add(AutomatikoMessages.processTimer);
        registry.add(AutomatikoMessages.workItem);
        registry.add(AutomatikoMessages.timerId);
    }

    @Override
    public void writeWorkItem(MarshallerWriteContext context, WorkItem workItem) {
        try {
            AutomatikoMessages.WorkItem _workItem = writeWorkItem(context, workItem, persistWorkItemVars);
            PersisterHelper.writeToStreamWithHeader(context, _workItem);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "IOException while storing work item instance " + workItem.getId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public WorkItemImpl readWorkItem(MarshallerReaderContext context) {
        try {
            ExtensionRegistry registry = PersisterHelper.buildRegistry(context, null);
            Header _header = PersisterHelper.readFromStreamWithHeaderPreloaded(context, registry);
            AutomatikoMessages.WorkItem _workItem = AutomatikoMessages.WorkItem.parseFrom(_header.getPayload(), registry);
            return (WorkItemImpl) readWorkItem(context, _workItem, persistWorkItemVars);
        } catch (IOException e) {
            throw new IllegalArgumentException("IOException while fetching work item instance : " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    "ClassNotFoundException while fetching work item instance : " + e.getMessage(), e);
        }
    }

}
