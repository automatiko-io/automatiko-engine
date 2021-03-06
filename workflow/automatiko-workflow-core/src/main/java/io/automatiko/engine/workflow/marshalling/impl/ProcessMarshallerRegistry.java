
package io.automatiko.engine.workflow.marshalling.impl;

import java.util.HashMap;
import java.util.Map;

import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

/**
 * Registry for Process/ProcessMarshaller
 */
public class ProcessMarshallerRegistry {

    public static ProcessMarshallerRegistry INSTANCE = new ProcessMarshallerRegistry();

    private Map<String, ProcessInstanceMarshaller> registry;

    private ProcessMarshallerRegistry() {
        this.registry = new HashMap<String, ProcessInstanceMarshaller>();
        register("RuleFlow", ProtobufRuleFlowProcessInstanceMarshaller.INSTANCE);
        register(ExecutableProcess.WORKFLOW_TYPE, ProtobufRuleFlowProcessInstanceMarshaller.INSTANCE);
        register(ExecutableProcess.FUNCTION_TYPE, ProtobufRuleFlowProcessInstanceMarshaller.INSTANCE);
        register(ExecutableProcess.FUNCTION_FLOW_TYPE, ProtobufRuleFlowProcessInstanceMarshaller.INSTANCE);
    }

    public void register(String type, ProcessInstanceMarshaller marchaller) {
        this.registry.put(type, marchaller);
    }

    public ProcessInstanceMarshaller getMarshaller(String type) {
        return this.registry.get(type);
    }

}
