
package io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers;

public class DecisionConstants {

	public static final String DECISIONS_NAME_SUFFIX = "_dmn_result";
	public static final String DECISIONS_HELP = "Decision output.";
	/**
	 * Array of label names for a prometheus object that needs an handler and an
	 * identifier.
	 */
	public static final String[] DECISION_ENDPOINT_IDENTIFIER_LABELS = new String[] { "decision", "endpoint",
			"identifier" };
	/**
	 * Array of label names for a prometheus object that needs only the handler.
	 */
	public static final String[] DECISION_ENDPOINT_LABELS = new String[] { "decision", "endpoint" };

	private DecisionConstants() {
	}
}
