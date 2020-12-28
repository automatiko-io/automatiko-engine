
package io.automatiko.engine.workflow.base.instance;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.workflow.VariableInitializer;
import io.automatiko.engine.services.correlation.CorrelationKey;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.process.executable.instance.ExecutableProcessInstance;

public class LightProcessRuntimeContext implements ProcessRuntimeContext {

    private final List<Process> processes;

    public LightProcessRuntimeContext(List<Process> processes) {
        this.processes = processes;
    }

    @Override
    public Collection<Process> getProcesses() {
        return processes;
    }

    @Override
    public Optional<Process> findProcess(String id) {
        return processes.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public ProcessInstance createProcessInstance(Process process, CorrelationKey correlationKey) {

        ExecutableProcessInstance processInstance = new ExecutableProcessInstance();
        processInstance.setProcess(process);

        if (correlationKey != null) {
            processInstance.getMetaData().put("CorrelationKey", correlationKey);
        }

        return processInstance;
    }

    @Override
    public void setupParameters(ProcessInstance processInstance, Map<String, Object> parameters,
            VariableInitializer variableInitializer) {
        Process process = processInstance.getProcess();
        // set variable default values
        // TODO: should be part of processInstanceImpl?
        VariableScope variableScope = (VariableScope) ((ContextContainer) process)
                .getDefaultContext(VariableScope.VARIABLE_SCOPE);
        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) processInstance
                .getContextInstance(VariableScope.VARIABLE_SCOPE);
        // set input parameters
        if (parameters != null) {
            if (variableScope != null) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {

                    variableScope.validateVariable(process.getName(), entry.getKey(), entry.getValue());
                    variableScopeInstance.setVariable(entry.getKey(), entry.getValue());
                }
            } else {
                throw new IllegalArgumentException("This process does not support parameters!");
            }
        }

        for (Variable var : variableScope.getVariables()) {
            if ((var.hasTag(Variable.AUTO_INITIALIZED) || var.getMetaData(Variable.DEFAULT_VALUE) != null)
                    && variableScopeInstance.getVariable(var.getName()) == null) {
                Object value = variableInitializer.initialize(var, variableScopeInstance.getVariables());

                variableScope.validateVariable(process.getName(), var.getName(), value);
                variableScopeInstance.setVariable(var.getName(), value);
            }
            if (var.hasTag(Variable.INITIATOR) && variableScopeInstance.getVariable(var.getName()) != null) {
                processInstance.setInitiator(variableScopeInstance.getVariable(var.getName()).toString());
            }
        }

        variableScopeInstance.enforceRequiredVariables();
    }
}
