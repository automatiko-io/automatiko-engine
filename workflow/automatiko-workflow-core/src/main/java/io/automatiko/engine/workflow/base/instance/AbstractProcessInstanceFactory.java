
package io.automatiko.engine.workflow.base.instance;

import java.util.Map;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.workflow.VariableInitializer;
import io.automatiko.engine.services.correlation.CorrelationKey;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;

public abstract class AbstractProcessInstanceFactory implements ProcessInstanceFactory {

    public ProcessInstance createProcessInstance(Process process, CorrelationKey correlationKey,
            InternalProcessRuntime runtime, Map<String, Object> parameters, VariableInitializer variableInitializer) {
        ProcessInstance processInstance = createProcessInstance();
        processInstance.setProcessRuntime(runtime);
        processInstance.setProcess(process);

        if (correlationKey != null) {
            processInstance.getMetaData().put("CorrelationKey", correlationKey);
        }

        runtime.getProcessInstanceManager().addProcessInstance(processInstance, correlationKey);

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
            if ((var.hasTag(Variable.AUTO_INITIALIZED_TAG) || var.getMetaData(Variable.DEFAULT_VALUE) != null)
                    && variableScopeInstance.getVariable(var.getName()) == null) {
                Object value = variableInitializer.initialize(var, variableScopeInstance.getVariables());

                variableScope.validateVariable(process.getName(), var.getName(), value);
                variableScopeInstance.setVariable(var.getName(), value);
            }
        }

        variableScopeInstance.enforceRequiredVariables();

        return processInstance;
    }

    public abstract ProcessInstance createProcessInstance();

}
