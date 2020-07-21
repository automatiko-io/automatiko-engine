
package io.automatik.engine.api.decision;

import java.util.Map;

public interface DecisionModel<M, C, R, F> {

	C newContext(Map<String, Object> inputSet);

	C newContext(F inputSet);

	R evaluateAll(C context);

	R evaluateDecisionService(C context, String decisionServiceName);

	M getDMNModel();

	R evaluateDecisionByName(C context, String... decisionName);

	R evaluateDecisionById(C context, String... decisionId);

	boolean hasErrors(R result);

	Map<String, Object> getResultData(R result);

	String buildErrorMessage(R result);

}
