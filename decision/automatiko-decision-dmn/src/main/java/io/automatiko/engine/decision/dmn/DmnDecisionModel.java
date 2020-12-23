package io.automatiko.engine.decision.dmn;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNMessage.Severity;

import io.automatiko.engine.api.ExecutionIdSupplier;
import io.automatiko.engine.api.decision.DecisionModel;

import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.api.core.FEELPropertyAccessible;

public class DmnDecisionModel implements DecisionModel<DMNModel, DMNContext, DMNResult, FEELPropertyAccessible> {

    private final DMNRuntime dmnRuntime;    
    private final DMNModel dmnModel;

    public DmnDecisionModel(DMNRuntime dmnRuntime, String namespace, String name) {
        this(dmnRuntime, namespace, name, null);
    }

    public DmnDecisionModel(DMNRuntime dmnRuntime, String namespace, String name, ExecutionIdSupplier execIdSupplier) {
        this.dmnRuntime = dmnRuntime;        
        this.dmnModel = dmnRuntime.getModel(namespace, name);
        if (dmnModel == null) {
            throw new IllegalStateException("DMN model '" + name + "' not found with namespace '" + namespace + "' in the inherent DMNRuntime.");
        }
    }

    @Override
    public DMNContext newContext(Map<String, Object> variables) {
        return new org.kie.dmn.core.impl.DMNContextImpl(variables != null ? variables : Collections.emptyMap());
    }

    @Override
    public DMNContext newContext(FEELPropertyAccessible inputSet) {
        return new org.kie.dmn.core.impl.DMNContextFPAImpl(inputSet);
    }

    @Override
    public DMNResult evaluateAll(DMNContext context) {
        return dmnRuntime.evaluateAll(dmnModel, context);
    }

    @Override
    public DMNResult evaluateDecisionService(DMNContext context, String decisionServiceName) {
        return dmnRuntime.evaluateDecisionService(dmnModel, context, decisionServiceName);
    }

    @Override
    public DMNResult evaluateDecisionByName(DMNContext context, String... decisionName) {
        return dmnRuntime.evaluateByName(dmnModel, context, decisionName);
    }

    @Override
    public DMNResult evaluateDecisionById(DMNContext context, String... decisionId) {
        return dmnRuntime.evaluateById(dmnModel, context, decisionId);
    }

    @Override
    public DMNModel getDMNModel() {
        return dmnModel;
    }

	@Override
	public boolean hasErrors(DMNResult result) {
		return result.hasErrors();
	}

	@Override
	public Map<String, Object> getResultData(DMNResult result) {

		return result.getContext().getAll();
	}

	@Override
	public String buildErrorMessage(DMNResult result) {
		return result.getMessages(Severity.ERROR).stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
	}
}
